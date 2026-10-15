package com.smartmoving.logic;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

/** 플레이어 주변 공간 검사 */
public final class WallProbe {
    private WallProbe() {
    }

    /** 서 있는 자세로 바꿀 공간이 있는가 (없으면 기어가기/슬라이딩을 유지) */
    public static boolean canStand(PlayerEntity player) {
        Box standing = player.getDimensions(EntityPose.STANDING).getBoxAt(player.getPos()).contract(1.0E-7);
        return player.getWorld().isSpaceEmpty(player, standing);
    }
}
