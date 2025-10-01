package com.yourpkg.smartmoving.logic;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class SwimHandler {
    public static Vec3d apply(PlayerEntity player, PlayerContext ctx, Vec3d in) {
        if (!player.isTouchingWater()) return in;
        return new Vec3d(in.x * SmartMovingMod.CONFIG.swimBoost, in.y, in.z * SmartMovingMod.CONFIG.swimBoost);
    }
}
