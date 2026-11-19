package com.smartmoving.logic;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** 플레이어 주변 공간 검사 */
public final class WallProbe {
    private WallProbe() {
    }

    /** 서 있는 자세로 바꿀 공간이 있는가 (없으면 기어가기/슬라이딩을 유지) */
    public static boolean canStand(Player player) {
        AABB standing = player.getDimensions(Pose.STANDING).makeBoundingBox(player.position()).deflate(1.0E-7);
        return player.level().noCollision(player, standing);
    }
}
