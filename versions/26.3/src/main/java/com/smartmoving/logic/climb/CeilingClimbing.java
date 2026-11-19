// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.handleCeilingClimbing), GPL-3.0-or-later.
package com.smartmoving.logic.climb;

import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Exhaustion;
import com.smartmoving.logic.Solids;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

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

    static void handle(Player player, SmartMovingState state, SmartMovingConfig cfg) {
        if (state.climbing) return;

        boolean exhaustionEnabled = cfg.ceilingClimbExhaustion && Exhaustion.enabled();
        if (exhaustionEnabled) {
            state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, cfg.ceilingClimbExhaustionStop);
            state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, cfg.ceilingClimbExhaustionStart);
            boolean allowed = state.exhaustion <= cfg.ceilingClimbExhaustionStop
                    && (state.wasCeilingClimbingThisTravel || state.exhaustion <= cfg.ceilingClimbExhaustionStart);
            if (!allowed) return;
        }

        Level world = player.level();
        double jd = player.getBoundingBox().maxY;
        int i = Mth.floor(player.getX());
        int j = Mth.floor(jd);
        int k = Mth.floor(player.getZ());

        boolean top = supportsCeilingClimbing(world.getBlockState(new BlockPos(i, j, k)));
        boolean bottom = supportsCeilingClimbing(world.getBlockState(new BlockPos(i, j + 1, k)));
        if (!top && !bottom) return;

        double jgap = 1D - jd + j;
        if (bottom) jgap++;

        double actuallySolidHeight = Solids.minBetween(player, jd, jd + 0.6, 0.2);
        if (jgap < 1.9 && actuallySolidHeight < jd + 0.5) {
            double motionY;
            if (jgap > 1.2) motionY = 0.12;
            else if (jgap > 1.115) motionY = 0.08;
            else motionY = 0.04;

            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, motionY, v.z);
            player.resetFallDistance();
            state.ceilingClimbing = true;
        }
    }

    /** 원작 supportsCeilingClimbing: 철창, 닫힌 다락문 (나무/철) */
    private static boolean supportsCeilingClimbing(BlockState state) {
        if (state.is(Blocks.IRON_BARS)) return true;
        return state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN);
    }
}
