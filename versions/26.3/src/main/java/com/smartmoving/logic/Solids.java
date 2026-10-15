package com.smartmoving.logic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 원작 SmartMovingBase 의 getMin/MaxPlayerSolidBetween, isPlayerInSolidBetween.
 * 플레이어의 수평 범위(+ 여유) 안, [yMin, yMax] 높이 구간에 있는 단단한 블록 면을 찾는다.
 */
public final class Solids {
    private Solids() {
    }

    /** 구간 안에서 가장 낮은 단단한 면의 아래쪽 높이. 없으면 yMax */
    public static double minBetween(Player player, double yMin, double yMax, double horizontalTolerance) {
        AABB probe = probe(player, yMin, yMax, horizontalTolerance);
        double result = yMax;
        for (VoxelShape shape : player.level().getBlockCollisions(player, probe)) {
            for (AABB box : shape.toAabbs()) {
                if (touches(box, probe)) result = Math.min(result, box.minY);
            }
        }
        return Math.max(result, yMin);
    }

    /** 구간 안에서 가장 높은 단단한 면의 위쪽 높이. 없으면 yMin */
    public static double maxBetween(Player player, double yMin, double yMax, double horizontalTolerance) {
        AABB probe = probe(player, yMin, yMax, horizontalTolerance);
        double result = yMin;
        for (VoxelShape shape : player.level().getBlockCollisions(player, probe)) {
            for (AABB box : shape.toAabbs()) {
                if (touches(box, probe)) result = Math.max(result, box.maxY);
            }
        }
        return Math.min(result, yMax);
    }

    /** 원작 isPlayerInSolidBetween: 플레이어 수평 범위의 [yMin, yMax] 에 단단한 블록이 있나 */
    public static boolean anyBetween(Player player, double yMin, double yMax) {
        return !player.level().noCollision(player, probe(player, yMin, yMax, 0));
    }

    private static AABB probe(Player player, double yMin, double yMax, double horizontalTolerance) {
        AABB bb = player.getBoundingBox();
        return new AABB(bb.minX - horizontalTolerance, yMin, bb.minZ - horizontalTolerance,
                bb.maxX + horizontalTolerance, yMax, bb.maxZ + horizontalTolerance);
    }

    private static boolean touches(AABB box, AABB probe) {
        return box.maxX >= probe.minX && box.minX <= probe.maxX
                && box.maxY >= probe.minY && box.minY <= probe.maxY
                && box.maxZ >= probe.minZ && box.minZ <= probe.maxZ;
    }
}
