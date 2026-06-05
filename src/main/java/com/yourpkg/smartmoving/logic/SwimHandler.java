package com.yourpkg.smartmoving.logic;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class SwimHandler {

    public static Vec3d apply(PlayerEntity player, PlayerContext ctx, Vec3d movementInput) {
        if (!player.isTouchingWater()) return movementInput;
        // 스태미나가 남아있는 동안만 수영 부스트 적용
        if (ctx.grabEnergy <= 0f) return movementInput;

        float boost = SmartMovingMod.CONFIG.swimBoost;
        return new Vec3d(movementInput.x * boost, movementInput.y, movementInput.z * boost);
    }
}
