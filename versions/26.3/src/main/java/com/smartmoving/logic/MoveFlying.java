// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingBase.moveFlying), GPL-3.0-or-later.
package com.smartmoving.logic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 원작 moveFlying(): 보는 방향(위아래 포함)으로 가속한다. 잠수와 원작 비행에서 쓴다. */
public final class MoveFlying {
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;

    private MoveFlying() {
    }

    /**
     * @param moveUpward     위(+)/아래(-) 입력
     * @param moveStrafing   좌(+)/우(-) 입력
     * @param moveForward    앞(+)/뒤(-) 입력
     * @param threeDimensional true 면 위아래 시선(pitch)도 방향에 들어간다
     */
    public static void apply(Player player, float moveUpward, float moveStrafing, float moveForward,
                             float speedFactor, boolean threeDimensional) {
        float diffMotionXStrafing = 0, diffMotionXForward = 0, diffMotionZStrafing = 0, diffMotionZForward = 0;
        float total = Mth.sqrt(moveStrafing * moveStrafing + moveForward * moveForward);
        if (total >= 0.01F) {
            if (total < 1.0F) total = 1.0F;
            float moveStrafingFactor = moveStrafing / total;
            float moveForwardFactor = moveForward / total;
            float sin = Mth.sin(player.getYRot() * DEG_TO_RAD);
            float cos = Mth.cos(player.getYRot() * DEG_TO_RAD);
            diffMotionXStrafing = moveStrafingFactor * cos;
            diffMotionXForward = -moveForwardFactor * sin;
            diffMotionZStrafing = moveStrafingFactor * sin;
            diffMotionZForward = moveForwardFactor * cos;
        }

        float rotation = threeDimensional ? player.getXRot() * DEG_TO_RAD : 0;
        float divingHorizontalFactor = Mth.cos(rotation);
        float divingVerticalFactor = -Mth.sin(rotation) * Math.signum(moveForward);

        float diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing;
        float diffMotionY = Mth.sqrt(diffMotionXForward * diffMotionXForward + diffMotionZForward * diffMotionZForward)
                * divingVerticalFactor + moveUpward;
        float diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing;

        // 원작 그대로의 식 (수평 성분에 sqrt 가 한 번 더 들어간다)
        float length = Mth.sqrt(Mth.sqrt(diffMotionX * diffMotionX + diffMotionZ * diffMotionZ)
                + diffMotionY * diffMotionY);
        if (length > 0.01F) {
            float factor = speedFactor / length;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x + diffMotionX * factor, v.y + diffMotionY * factor, v.z + diffMotionZ * factor);
        }
    }

    /** 원작 getSpeedFactor(): 이동 속도 속성(신속 효과 등) 기준, 바닐라 달리기 보정은 뺀다 */
    public static float baseSpeedFactor(Player player) {
        float speed = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 10F;
        return player.isSprinting() ? speed / 1.3F : speed;
    }
}
