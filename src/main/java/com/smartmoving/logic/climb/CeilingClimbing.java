// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.handleCeilingClimbing), GPL-3.0-or-later.
package com.smartmoving.logic.climb;

import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Exhaustion;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * 원작 천장 매달리기: 잡기 키를 누른 채 철창이나 닫힌 다락문 바로 아래에 있으면 매달려서 천천히 움직인다.
 */
public final class CeilingClimbing {
    private CeilingClimbing() {
    }

    /** 원작 couldClimbCeiling (벽 타기 결과는 handle 에서 확인) */
    static boolean couldClimb(SmartMovingState state, SmartMovingConfig cfg) {
        return state.wantClimbCeiling && !state.crawling && cfg.enableCeilingClimbing;
    }

    static void handle(PlayerEntity player, SmartMovingState state, SmartMovingConfig cfg) {
        if (state.climbing) return;

        boolean exhaustionEnabled = cfg.ceilingClimbExhaustion && Exhaustion.enabled();
        if (exhaustionEnabled) {
            state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, cfg.ceilingClimbExhaustionStop);
            state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, cfg.ceilingClimbExhaustionStart);
            boolean allowed = state.exhaustion <= cfg.ceilingClimbExhaustionStop
                    && (state.wasCeilingClimbingThisTravel || state.exhaustion <= cfg.ceilingClimbExhaustionStart);
            if (!allowed) return;
        }

        World world = player.getWorld();
        double jd = player.getBoundingBox().maxY;
        int i = MathHelper.floor(player.getX());
        int j = MathHelper.floor(jd);
        int k = MathHelper.floor(player.getZ());

        boolean top = supportsCeilingClimbing(world.getBlockState(new BlockPos(i, j, k)));
        boolean bottom = supportsCeilingClimbing(world.getBlockState(new BlockPos(i, j + 1, k)));
        if (!top && !bottom) return;

        double jgap = 1D - jd + j;
        if (bottom) jgap++;

        double actuallySolidHeight = minSolidBetween(player, jd, jd + 0.6, 0.2);
        if (jgap < 1.9 && actuallySolidHeight < jd + 0.5) {
            double motionY;
            if (jgap > 1.2) motionY = 0.12;
            else if (jgap > 1.115) motionY = 0.08;
            else motionY = 0.04;

            Vec3d v = player.getVelocity();
            player.setVelocity(v.x, motionY, v.z);
            player.onLanding();
            state.ceilingClimbing = true;
        }
    }

    /** 원작 supportsCeilingClimbing: 철창, 닫힌 다락문 (나무/철) */
    private static boolean supportsCeilingClimbing(BlockState state) {
        if (state.isOf(Blocks.IRON_BARS)) return true;
        return state.getBlock() instanceof TrapdoorBlock && !state.get(TrapdoorBlock.OPEN);
    }

    /** 원작 getMinPlayerSolidBetween: 플레이어 위 [yMin, yMax] 구간에서 가장 낮은 단단한 면의 높이 */
    private static double minSolidBetween(PlayerEntity player, double yMin, double yMax, double horizontalTolerance) {
        Box bb = player.getBoundingBox();
        Box probe = new Box(bb.minX - horizontalTolerance, yMin, bb.minZ - horizontalTolerance,
                bb.maxX + horizontalTolerance, yMax, bb.maxZ + horizontalTolerance);
        double result = yMax;
        for (VoxelShape shape : player.getWorld().getBlockCollisions(player, probe)) {
            for (Box box : shape.getBoundingBoxes()) {
                if (box.maxX >= probe.minX && box.minX <= probe.maxX
                        && box.maxY >= yMin && box.minY <= yMax
                        && box.maxZ >= probe.minZ && box.minZ <= probe.maxZ) {
                    result = Math.min(result, box.minY);
                }
            }
        }
        return Math.max(result, yMin);
    }
}
