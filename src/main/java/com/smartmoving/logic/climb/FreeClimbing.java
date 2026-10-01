// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (net.smart.moving.SmartMovingSelf.handleClimbing), GPL-3.0-or-later.
package com.smartmoving.logic.climb;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Exhaustion;
import com.smartmoving.logic.JumpType;
import com.smartmoving.logic.Jumps;
import com.smartmoving.logic.MoveSpeed;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 원작 handleClimbing() 의 자유 벽 타기 부분.
 *
 * <p>원작 순서는 "입력 가속 → 이동 → 벽 타기로 세로 속도 결정 → 중력" 이다.
 * 바닐라 travel() 은 이동 직후 바로 중력을 적용하므로, travel() 이 끝난 뒤 중력과 감속을 되돌리고
 * 원작 계산을 한 다음 다시 적용한다. 결과는 원작과 같다.
 *
 * <p>벽 타기는 손이나 발로 잡을 곳(블록 모서리, 반블록, 계단, 울타리 등)이 있어야 한다. 판정은 {@link Orientation}.
 */
public final class FreeClimbing {
    /** 원작 SmartMovingContext 의 벽 타기 속도 상수 */
    public static final float CLIMB_PULL_MOTION = 0.3F;
    private static final double FAST_UP_MOTION = 0.2D;
    private static final double MEDIUM_UP_MOTION = 0.14D;
    private static final double SLOW_UP_MOTION = 0.1D;
    private static final double HOLD_MOTION = 0.08D;
    private static final double SINK_DOWN_MOTION = 0.05D;
    private static final double CLIMB_DOWN_MOTION = 0.01D;

    /** 원작 rotation 순서 그대로 (같은 값이면 먼저 찾은 쪽이 남는다) */
    private static final Orientation[] ORTHOGONALS = {Orientation.PZ, Orientation.NZ, Orientation.ZP, Orientation.ZN};
    private static final Orientation[] DIAGONALS = {Orientation.PP, Orientation.NP, Orientation.NN, Orientation.PN};

    private static final HandsClimbing[] INOUT_HANDS = new HandsClimbing[1];
    private static final FeetClimbing[] INOUT_FEET = new FeetClimbing[1];
    private static final ClimbGap HANDS_GAP = new ClimbGap();
    private static final ClimbGap FEET_GAP = new ClimbGap();

    private FreeClimbing() {
    }

    /**
     * travel() 시작. 원작 moveEntityWithHeading() 앞부분과 getSpeedFactor() 의 벽 타기 부분.
     *
     * @return 바뀐 이동 입력 (x = 좌우, z = 앞뒤)
     */
    public static Vec3d beforeTravel(PlayerEntity player, SmartMovingState state, Vec3d input) {
        if (player.horizontalCollision) {
            state.lastHorizontalCollisionX = player.getX();
            state.lastHorizontalCollisionZ = player.getZ();
        }

        // 바닐라 travelMidAir() 가 쓰는 수평 감속 (이동 전에 정해진다)
        float slipperiness = 1F;
        if (player.isOnGround()) {
            BlockPos below = BlockPos.ofFloored(player.getX(), player.getBoundingBox().minY - 0.5000001, player.getZ());
            slipperiness = player.getWorld().getBlockState(below).getBlock().getSlipperiness();
        }
        state.travelHorizontalDamping = slipperiness * 0.91F;

        Vec3d result = input;
        // 내려가는 중 아무 키도 안 누르면 벽 쪽으로 당겨서 붙어 있게 한다.
        if (state.climbing && input.x == 0 && input.z == 0 && state.wantClimbDown && state.neighborClimbing
                && !(Math.abs(player.getX() - state.lastHorizontalCollisionX) < 0.05
                && Math.abs(player.getZ() - state.lastHorizontalCollisionZ) < 0.05)) {
            result = new Vec3d(input.x, input.y, CLIMB_PULL_MOTION);
        }

        state.wasClimbingThisTravel = state.climbing;
        reset(state);
        return result;
    }

    public static void reset(SmartMovingState state) {
        state.climbing = false;
        state.neighborClimbing = false;
    }

    /** travel() 끝. 바닐라 이동이 끝난 직후. */
    public static void afterTravel(PlayerEntity player, SmartMovingState state) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        if (!cfg.enableClimbing) return;
        // 사다리/덩굴은 바닐라가 처리한다.
        if (player.isClimbing() || player.hasNoGravity() || player.hasStatusEffect(StatusEffects.LEVITATION)) return;
        if (player.fallDistance > cfg.freeClimbFallMaximumDistance) return;
        // 기어가며 오르기 / 슬라이딩하다 벽 잡기는 아직 포트하지 않았다.
        if (state.crawling || state.sliding || state.headJumping) return;
        if (!state.wantClimbUp && !state.wantClimbDown) return;

        boolean exhaustionEnabled = cfg.climbExhaustion && Exhaustion.enabled();
        if (exhaustionEnabled) {
            state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, cfg.climbExhaustionStop);
            state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, cfg.climbExhaustionStart);
            boolean allowed = state.exhaustion <= cfg.climbExhaustionStop
                    && (state.wasClimbingThisTravel || state.exhaustion <= cfg.climbExhaustionStart);
            if (!allowed) return;
        }

        // 바닐라가 이미 적용한 중력과 감속을 되돌린다.
        Vec3d v = player.getVelocity();
        double gravity = player.getFinalGravity();
        double motionY = v.y / 0.98F + gravity;
        if (motionY <= 0 && player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = Math.min(gravity, 0.01);
            motionY = v.y / 0.98F + gravity;
        }
        double damping = state.travelHorizontalDamping;
        player.setVelocity(v.x / damping, motionY, v.z / damping);

        handleClimbing(player, state, cfg);

        // 원작 setLandMotions(): 중력과 감속
        Vec3d after = player.getVelocity();
        player.setVelocity(after.x * damping, (after.y - gravity) * 0.98F, after.z * damping);
    }

    private static void handleClimbing(PlayerEntity player, SmartMovingState state, SmartMovingConfig cfg) {
        World world = player.getWorld();
        double id = player.getX();
        double jd = player.getBoundingBox().minY;
        double kd = player.getZ();

        int i = MathHelper.floor(id);
        int j = MathHelper.floor(jd);
        int k = MathHelper.floor(kd);

        float rotation = Orientation.normalizeYaw(player.getYaw());
        double jh = jd * 2D + 1;

        INOUT_HANDS[0] = HandsClimbing.NONE;
        INOUT_FEET[0] = FeetClimbing.NONE;
        HANDS_GAP.reset();
        FEET_GAP.reset();

        for (Orientation orientation : ORTHOGONALS) {
            orientation.seekClimbGap(rotation, world, i, id, jh, k, kd, false, false, false,
                    INOUT_HANDS, INOUT_FEET, HANDS_GAP, FEET_GAP);
        }
        state.neighborClimbing = INOUT_HANDS[0] != HandsClimbing.NONE || INOUT_FEET[0] != FeetClimbing.NONE;

        for (Orientation orientation : DIAGONALS) {
            orientation.seekClimbGap(rotation, world, i, id, jh, k, kd, false, false, false,
                    INOUT_HANDS, INOUT_FEET, HANDS_GAP, FEET_GAP);
        }

        HandsClimbing handsClimbing = INOUT_HANDS[0];
        FeetClimbing feetClimbing = INOUT_FEET[0];

        boolean hasClimbGap = HANDS_GAP.canStand || FEET_GAP.canStand;
        boolean hasClimbCrawlGap = HANDS_GAP.mustCrawl || FEET_GAP.mustCrawl;

        // 사다리 맨 아래를 손으로 잡았는데 사다리 뒤가 비어 있으면 잡을 수 없다.
        if (handsClimbing == HandsClimbing.BOTTOM_HOLD
                && Orientation.isLadder(world.getBlockState(new BlockPos(i, j + 2, k)))) {
            Orientation ladder = Orientation.getKnownLadderOrientation(world, i, j + 2, k);
            if (ladder != null) {
                int ri = i + ladder.i;
                int rk = k + ladder.k;
                if (!isSolid(world, ri, j, rk) && !isSolid(world, ri, j + 1, rk)) {
                    handsClimbing = HandsClimbing.NONE;
                }
            }
        }

        if (!state.grabKey && handsClimbing == HandsClimbing.UP && feetClimbing == FeetClimbing.NONE) {
            if (!player.horizontalCollision && world.isAir(new BlockPos(i, j, k)) && world.isAir(new BlockPos(i, j + 1, k))) {
                handsClimbing = HandsClimbing.NONE;
            }
        }

        // 발로만 오르는 건 틈에서 균형을 잡을 때나 손과 함께일 때뿐
        if (!feetClimbing.isRelevant() && !handsClimbing.isRelevant()) return;

        if (state.wantClimbUp) {
            handsClimbing = handsClimbing.toUp();

            if (feetClimbing == FeetClimbing.FAST_UP
                    && !(handsClimbing == HandsClimbing.NONE && player.isOnGround() && !(FEET_GAP.block instanceof BedBlock))) {
                // 빠르게 오르기
                setClimbSpeed(player, state, cfg, FAST_UP_MOTION);
            } else if ((hasClimbGap || hasClimbCrawlGap) && handsClimbing == HandsClimbing.FAST_UP
                    && (feetClimbing == FeetClimbing.NONE || feetClimbing == FeetClimbing.BASE_WITH_HANDS)) {
                // 틈으로 기어 올라가기
                setClimbSpeed(player, state, cfg, feetClimbing == FeetClimbing.NONE ? SLOW_UP_MOTION : FAST_UP_MOTION);
            } else if (feetClimbing.isRelevant() && handsClimbing.isRelevant()
                    && !(feetClimbing == FeetClimbing.BASE_HOLD && handsClimbing == HandsClimbing.SINK)
                    && !(handsClimbing == HandsClimbing.SINK && feetClimbing == FeetClimbing.TOP_WITH_HANDS)
                    && !(handsClimbing == HandsClimbing.TOP_HOLD && feetClimbing == FeetClimbing.TOP_WITH_HANDS)) {
                // 손발 모두로 오르기
                setClimbSpeed(player, state, cfg, MEDIUM_UP_MOTION);
            } else if (handsClimbing.isUp()) {
                // 손으로만 천천히
                setClimbSpeed(player, state, cfg, SLOW_UP_MOTION);
            } else if (handsClimbing == HandsClimbing.TOP_HOLD || feetClimbing == FeetClimbing.BASE_HOLD
                    || (feetClimbing == FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS && handsClimbing == HandsClimbing.NONE)) {
                // 꼭대기에 매달림: 점프하면 위로 뛰어오른다
                boolean jumped = state.jumpStartKey && Jumps.tryJump(player, state,
                        feetClimbing != FeetClimbing.NONE ? JumpType.CLIMB_UP : JumpType.CLIMB_UP_HANDS_ONLY,
                        MoveSpeed.STANDING, null);
                if (!jumped) {
                    setClimbSpeed(player, state, cfg, HOLD_MOTION);
                }
            } else if (handsClimbing == HandsClimbing.SINK
                    || (feetClimbing == FeetClimbing.SLOW_UP_WITH_SINK_WITHOUT_HANDS && handsClimbing == HandsClimbing.NONE)) {
                // 버티지 못하고 미끄러짐
                setClimbSpeed(player, state, cfg, SINK_DOWN_MOTION);
            }
        } else if (state.wantClimbDown) {
            handsClimbing = handsClimbing.toDown();

            if (handsClimbing == HandsClimbing.BOTTOM_HOLD && !feetClimbing.isIndependentlyRelevant()) {
                // 아래쪽에 매달림
                setClimbSpeed(player, state, cfg, HOLD_MOTION);
            } else if (handsClimbing.isRelevant()) {
                // 일부러 내려가기
                if (feetClimbing == FeetClimbing.FAST_UP
                        || feetClimbing == FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS
                        || feetClimbing == FeetClimbing.TOP_WITH_HANDS) {
                    setClimbSpeed(player, state, cfg, CLIMB_DOWN_MOTION);
                } else if (feetClimbing == FeetClimbing.BASE_WITH_HANDS || feetClimbing == FeetClimbing.BASE_HOLD) {
                    if ((handsClimbing != HandsClimbing.NONE && handsClimbing != HandsClimbing.UP)
                            || (handsClimbing == HandsClimbing.UP && feetClimbing == FeetClimbing.BASE_HOLD)) {
                        setClimbSpeed(player, state, cfg, CLIMB_DOWN_MOTION);
                    } else {
                        setClimbSpeed(player, state, cfg, SINK_DOWN_MOTION);
                    }
                } else {
                    setClimbSpeed(player, state, cfg, SINK_DOWN_MOTION);
                }
            }

            if (state.climbHolding) {
                // 웅크리기로 버티기: 점프하면 벽을 박차고 뒤로
                setClimbSpeed(player, state, cfg, HOLD_MOTION);

                if (state.jumpStartKey) {
                    // 원작 그대로: 발이 걸려 있으면 "손만" 점프 종류를 쓴다
                    boolean handsOnly = feetClimbing != FeetClimbing.NONE;
                    boolean head = cfg.enableHeadJump && (cfg.climbJumpBackHeadOnGrab == state.grabKey);
                    JumpType type = head
                            ? (handsOnly ? JumpType.CLIMB_BACK_HEAD_HANDS_ONLY : JumpType.CLIMB_BACK_HEAD)
                            : (handsOnly ? JumpType.CLIMB_BACK_UP_HANDS_ONLY : JumpType.CLIMB_BACK_UP);

                    float jumpAngle = player.getYaw() + 180F;
                    if (Jumps.tryJump(player, state, type, MoveSpeed.STANDING, jumpAngle)) {
                        state.climbing = false;
                        state.climbHolding = false;
                        player.setYaw(jumpAngle);
                    }
                }
            }
        }

        if (state.climbing) {
            // 원작 handleCrash(): 대미지는 서버가 준다 (PlayerEntityMixin). 여기서는 낙하 거리만 지운다.
            player.onLanding();
        }
    }

    /** 원작 setOnlyShouldClimbSpeed() */
    private static void setClimbSpeed(PlayerEntity player, SmartMovingState state, SmartMovingConfig cfg, double value) {
        state.climbing = true;

        if (value != HOLD_MOTION) {
            float factor = speedFactor(player);
            if (state.climbFast) factor *= cfg.sprintFactor;

            if (value > HOLD_MOTION) {
                value = (value - HOLD_MOTION) * cfg.freeClimbingUpSpeedFactor * factor + HOLD_MOTION;
            } else {
                value = HOLD_MOTION - (HOLD_MOTION - value) * cfg.freeClimbingDownSpeedFactor * factor;
            }
        }

        Vec3d v = player.getVelocity();
        if (value < 0 || value > v.y) {
            player.setVelocity(v.x, value, v.z);
        }
    }

    /** 원작 getSpeedFactor(): 이동 속도 속성(신속 효과 등) 기준, 바닐라 달리기 보정은 뺀다 */
    private static float speedFactor(PlayerEntity player) {
        float speed = (float) player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 10F;
        return player.isSprinting() ? speed / 1.3F : speed;
    }

    private static boolean isSolid(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }
}
