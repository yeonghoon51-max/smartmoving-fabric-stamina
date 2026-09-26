package com.smartmoving.logic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/** 플레이어 앞에 붙잡을 수 있는 벽이 있는지 검사한다. */
public final class WallProbe {
    /** 벽과 이 거리(블록) 이내에 있어야 붙잡을 수 있다 */
    private static final double REACH = 0.15;

    private WallProbe() {
    }

    /**
     * 플레이어 몸의 [fromFraction, toFraction] 높이 구간 바로 앞(dir 방향)에 충돌 블록이 있는지.
     * 0 = 발, 1 = 머리 꼭대기.
     */
    public static boolean hasWall(PlayerEntity player, Direction dir, double fromFraction, double toFraction) {
        Box box = player.getBoundingBox();
        double height = box.getLengthY();
        Box probe = box
                .offset(dir.getOffsetX() * REACH, 0, dir.getOffsetZ() * REACH)
                .withMinY(box.minY + height * fromFraction)
                .withMaxY(box.minY + height * toFraction);
        return !player.getWorld().isSpaceEmpty(player, probe);
    }

    /** 몸 어느 부분이든 벽에 닿아 있다 */
    public static boolean hasAnyWall(PlayerEntity player, Direction dir) {
        return hasWall(player, dir, 0.05, 1.0);
    }

    /**
     * 하체 앞에는 벽이 있는데 상체 앞은 비어 있다 = 벽 꼭대기(턱)에 도달했다.
     */
    public static boolean isAtLedge(PlayerEntity player, Direction dir) {
        return hasWall(player, dir, 0.05, 0.5) && !hasWall(player, dir, 0.55, 1.0);
    }
}
