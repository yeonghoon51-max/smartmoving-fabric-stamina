// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.handleSwimming), GPL-3.0-or-later.
package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.climb.Orientation;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 원작 수영/잠수.
 *
 * <ul>
 *   <li><b>헤엄치기</b>(isSwimming): 물이 깊으면 머리를 물 위에 내놓고 평영으로 나아간다. 수면 높이에 맞춰 떠 있는다.</li>
 *   <li><b>잠수</b>(isDiving): 몸이 다 잠기면 보는 방향(위아래 포함)으로 나아간다. 점프 = 위로, 웅크리기 = 아래로.
 *       아무 키도 안 누르면 그 자리에 떠 있는다(levitating).</li>
 *   <li><b>물 걷기</b>(isDipping): 얕은 물에서 걷는다.</li>
 * </ul>
 *
 * <p>원작은 이 계산을 "몸 전체(1.8칸) 상자" 기준으로 하고, 헤엄/잠수 중에는 상자를 납작하게 했다.
 * 여기서는 헤엄칠 때는 서 있는 상자(머리가 물 위에), 잠수할 때는 바닐라의 납작한 자세를 쓴다.
 */
public final class SmartSwimming {
    /** 원작 SmartMovingContext */
    private static final float SWIM_CRAWL_WATER_TOP_BORDER = 0.65F;
    private static final float SWIM_CRAWL_WATER_BOTTOM_BORDER = 0.55F;
    private static final double BODY_HEIGHT = 1.8;

    private SmartSwimming() {
    }

    /**
     * 원작 handleSwimming(). Player.travel() 시작에서 부른다.
     *
     * @return true 면 바닐라 이동(물속 이동)을 취소한다
     */
    public static boolean travel(Player player, SmartMovingState state, Vec3 input) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        boolean wasSwimming = state.swimming;
        boolean wasDiving = state.diving;
        boolean wasJumpingOutOfWater = state.jumpingOutOfWater;

        boolean liquidClimbing = cfg.enableClimbing && player.fallDistance <= 3.0 && state.wantClimbUp
                && player.horizontalCollision && !state.diving;
        boolean handle = !state.flying && !liquidClimbing && !player.isPassenger()
                && (player.isInWater() || (wasSwimming && isInLiquid(player)));
        if (!handle || (!cfg.enableSwimming && !cfg.enableDiving)) {
            reset(state);
            return false;
        }

        // 기어가며 오르기 중에는 바닐라 물 이동
        if (state.climbCrawling || state.crawlClimbing) {
            reset(state);
            return false;
        }

        Level world = player.level();
        float moveStrafing = (float) input.x;
        float moveForward = (float) input.z;

        // 작은 자세(기어가기, 슬라이딩)는 "원래 발"이 상자 아래 1칸이다.
        boolean small = state.crawling || state.sliding;
        double minY = player.getBoundingBox().minY - (small ? 1D : 0D);
        double maxY = minY + BODY_HEIGHT;

        int i = Mth.floor(player.getX());
        int j = Mth.floor(minY);
        int k = Mth.floor(player.getZ());

        double totalSwimWaterBorder = maxLiquidBetween(player, maxY - BODY_HEIGHT, maxY + 1.2);
        double minPlayerSwimWaterCeiling = Solids.minBetween(player, maxY - BODY_HEIGHT, maxY + 1.2, 0);
        double realTotalSwimWaterBorder = Math.min(totalSwimWaterBorder, minPlayerSwimWaterCeiling);
        double minPlayerSwimWaterDepth = totalSwimWaterBorder
                - Solids.maxBetween(player, totalSwimWaterBorder - 2, totalSwimWaterBorder, 0);
        double realMinPlayerSwimWaterDepth = totalSwimWaterBorder
                - Solids.maxBetween(player, realTotalSwimWaterBorder - 2, realTotalSwimWaterBorder, 0);
        double playerSwimWaterBorder = totalSwimWaterBorder - minY;

        // 기어가다 물이 깊어지면 헤엄치기로, 얕으면 계속 기어간다.
        if (small) {
            double playerCrawlWaterBorder = playerSwimWaterBorder - 1D;
            if (playerCrawlWaterBorder < SWIM_CRAWL_WATER_TOP_BORDER) {
                reset(state);
                return false;
            }
            state.crawling = false;
            state.sliding = false;
            state.swimming = true;
            state.diving = false;
            return false; // 이번 틱은 바닐라 물 이동, 다음 틱부터 헤엄친다
        }

        double motionYDiff = 0;
        boolean couldStandUp = playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5;

        boolean diveUp = state.prevJumpKey;
        boolean sneak = state.prevSneakKey;
        boolean diveDown = sneak && cfg.diveDownOnSneak;
        boolean swimDown = sneak && cfg.swimDownOnSneak;
        boolean isFast = state.liquidFast;
        float speedFactor = MoveFlying.baseSpeedFactor(player) * (isFast ? cfg.sprintFactor : 1F);

        boolean wantShallowSwim = couldStandUp && (wasSwimming || wasDiving);
        if (wantShallowSwim && Orientation.isTunnelAhead(world, Orientation.normalizeYaw(player.getYRot()), i, j, k)) {
            wantShallowSwim = false;
        }

        if (wasSwimming && wantShallowSwim && swimDown) swimDown = false;
        if (wasDiving && diveUp && diveDown) diveUp = diveDown = false;

        boolean swimming = false;
        boolean diving = false;
        boolean dipping = false;

        if (playerSwimWaterBorder >= 0 && playerSwimWaterBorder <= 2) {
            double offset = playerSwimWaterBorder + 0.1625D; // 원작의 미세 조정 값
            boolean moveSwim = player.getXRot() < 0F && moveForward > 0F || player.getXRot() > 0F && moveForward < 0F;
            if (diveUp || moveSwim || wantShallowSwim) {
                if (offset < 1.4) {
                    dipping = true;
                    motionYDiff = offset < 1 ? -0.02D : -0.01D;
                } else if (offset < 1.9) {
                    swimming = true;
                    if (swimDown) motionYDiff = -0.05D * (isFast ? cfg.sprintFactor : 1F);
                    else if (offset < 1.5) motionYDiff = -0.02D;
                    // 원작 그대로: 아래 비교가 위의 결과를 덮어쓴다
                    if (offset < 1.6) motionYDiff = -0.01D;
                    else if (offset < 1.62) motionYDiff = -0.005D;
                    else if (offset < 1.64) motionYDiff = -0.0025D;
                    else if (offset < 1.66) motionYDiff = -0.00125D;
                    else if (offset < 1.664) motionYDiff = -0.000625D;
                    else if (offset < 1.668) motionYDiff = 0D;
                    else if (offset < 1.672) motionYDiff = 0.000625D;
                    else if (offset < 1.676) motionYDiff = 0.00125D;
                    else if (offset < 1.68) motionYDiff = 0.0025D;
                    else if (offset < 1.7) motionYDiff = 0.005D;
                    else if (offset < 1.8) motionYDiff = 0.01D;
                    else motionYDiff = 0.02D;
                } else {
                    diving = true;
                    if (diveUp) motionYDiff = 0.05D * (isFast ? cfg.sprintFactor : 1F);
                    else if (diveDown) motionYDiff = 0.01 - 0.1 * speedFactor;
                    else motionYDiff = moveSwim ? 0.04D : 0.02D;
                }
            } else {
                if (offset < 1.5) {
                    dipping = true;
                    motionYDiff = -0.02D;
                } else {
                    diving = true;
                    if (diveDown) motionYDiff = 0.01 - 0.1 * speedFactor;
                    else if (offset < 1.8) motionYDiff = -0.02D;
                    else if (offset < 1.82) motionYDiff = -0.01D;
                    else if (offset < 1.84) motionYDiff = -0.005D;
                    else if (offset < 1.86) motionYDiff = -0.0025D;
                    else if (offset < 1.864) motionYDiff = -0.00125D;
                    else if (offset < 1.868) motionYDiff = 0D;
                    else if (offset < 1.872) motionYDiff = 0.00125D;
                    else if (offset < 1.876) motionYDiff = 0.0025D;
                    else if (offset < 1.88) motionYDiff = 0.005D;
                    else motionYDiff = 0.01D;
                }
            }
        } else if (playerSwimWaterBorder > 2) {
            diving = true;
            if (diveUp) {
                if (isFast && playerSwimWaterBorder < 2.5 && world.getBlockState(new BlockPos(i, j + 3, k)).isAir()) {
                    motionYDiff = 0.11D / cfg.sprintFactor;
                } else {
                    motionYDiff = 0.01 + 0.1 * speedFactor;
                }
            } else if (diveDown) {
                motionYDiff = 0.01 - 0.1 * speedFactor;
            } else {
                motionYDiff = 0.01D;
            }
        } else {
            reset(state);
            return false;
        }

        swimming = swimming && cfg.enableSwimming;
        diving = diving && cfg.enableDiving;
        dipping = dipping && cfg.enableSwimming;
        if (!swimming && !diving && !dipping) {
            reset(state);
            return false; // 바닐라 물 이동
        }

        Vec3 v = player.getDeltaMovement();
        double motionX = v.x;
        double motionY = v.y;
        double motionZ = v.z;

        // 바닐라가 travel() 전에 더한 값을 되돌린다 (원작도 1.8 바닐라의 점프 +0.04 를 되돌렸다).
        if (diveUp) motionY -= 0.04D;
        if (sneak) motionY += 0.04D; // 1.13 이후 바닐라의 "웅크리면 가라앉기"

        if (swimming) {
            motionX *= 0.85D;
            motionY *= 0.85D;
            motionZ *= 0.85D;
        } else if (diving) {
            motionX *= 0.83D;
            motionY *= 0.83D;
            motionZ *= 0.83D;
        } else {
            motionX *= 0.80D;
            motionY *= 0.83D;
            motionZ *= 0.80D;
        }

        boolean levitating = diving && !diveUp && !diveDown && moveStrafing == 0F && moveForward == 0F;
        if (diving) speedFactor *= cfg.diveSpeedFactor;
        if (swimming) speedFactor *= cfg.swimSpeedFactor;

        if (swimming || diving) state.waterMovementTicks++;
        else state.waterMovementTicks = 0;

        // 헤엄치다 물가 벽에 부딪히며 점프하면 물 밖으로 뛰어오른다
        boolean wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0) && player.horizontalCollision && diveUp && !sneak;
        state.jumpingOutOfWater = wantJumpOutOfWater
                && (state.waterMovementTicks > 10 || player.onGround() || wasJumpingOutOfWater);

        player.setDeltaMovement(motionX, motionY, motionZ);
        boolean moveFlying = true;
        if (diving) {
            if (diveUp || diveDown || levitating) {
                setVelocityY(player, (motionY + motionYDiff) * 0.6);
            } else {
                MoveFlying.apply(player, (float) motionYDiff, moveStrafing, moveForward, 0.02F * speedFactor,
                        cfg.diveControlVertical);
            }
            moveFlying = false;
        } else if (swimming && swimDown) {
            setVelocityY(player, (motionY + motionYDiff) * 0.6);
        } else if (state.jumpingOutOfWater) {
            setVelocityY(player, 0.30000001192092896D);
        } else {
            setVelocityY(player, motionY + motionYDiff);
        }

        state.diving = diving;
        state.swimming = swimming;
        state.dipping = dipping;
        state.levitating = levitating;

        // 얕은 곳에 닿으면 헤엄치기를 끝내고 걷거나(웅크리고 있으면) 기어간다
        boolean shallowDiveOrSwim = couldStandUp && (state.diving || state.swimming);
        if (shallowDiveOrSwim && realMinPlayerSwimWaterDepth < SWIM_CRAWL_WATER_BOTTOM_BORDER) {
            state.diving = false;
            state.swimming = false;
            state.dipping = true;
            if (sneak) {
                state.crawling = true;
            } else {
                double bb = player.getBoundingBox().minY;
                double lift = Solids.maxBetween(player, bb, bb + BODY_HEIGHT, 0) - bb;
                if (lift > 0) player.move(MoverType.SELF, new Vec3(0, lift, 0));
            }
        }

        if (moveFlying) {
            // 원작 sp.moveFlying(): 수평 입력만, 1.8 바닐라와 같은 방식
            player.moveRelative(0.02F * speedFactor, new Vec3(moveStrafing, 0, moveForward));
        }
        player.move(MoverType.SELF, player.getDeltaMovement());
        player.resetFallDistance();
        return true;
    }

    public static void reset(SmartMovingState state) {
        state.swimming = false;
        state.diving = false;
        state.dipping = false;
        state.levitating = false;
        state.jumpingOutOfWater = false;
        state.waterMovementTicks = 0;
    }

    private static void setVelocityY(Player player, double y) {
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, y, v.z);
    }

    /** 원작 isInLiquid() */
    private static boolean isInLiquid(Player player) {
        double minY = player.getBoundingBox().minY;
        return maxLiquidBetween(player, minY, minY + BODY_HEIGHT) != minY;
    }

    /** 원작 getMaxPlayerLiquidBetween(): 위에서부터 내려가며 처음 만난 물의 수면 높이 */
    private static double maxLiquidBetween(Player player, double yMin, double yMax) {
        Level world = player.level();
        int i = Mth.floor(player.getX());
        int k = Mth.floor(player.getZ());
        for (int j = Mth.floor(yMax); j >= Mth.floor(yMin); j--) {
            BlockPos pos = new BlockPos(i, j, k);
            FluidState fluid = world.getFluidState(pos);
            if (fluid.is(FluidTags.WATER)) {
                float border = fluid.getHeight(world, pos);
                if (border > 0) return j + border;
            }
        }
        return yMin;
    }
}
