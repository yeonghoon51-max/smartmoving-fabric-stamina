// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.handleAlternativeFlying), GPL-3.0-or-later.
package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 원작 비행(smart flying): 크리에이티브 비행을 보는 방향으로 나는 비행으로 바꾼다.
 * 앞키를 누르면 바라보는 방향(위아래 포함)으로 날고, 점프/웅크리기로 위아래, 몸은 엎드린 자세가 된다.
 */
public final class SmartFlying {
    /** 원작 HorizontalAirDamping (비행 중에는 세 방향 모두) */
    private static final double DAMPING = 0.91;

    private SmartFlying() {
    }

    /** 원작 isFlying: 원작 비행을 쓰는 중인가 (헤엄/잠수 중이면 아니다) */
    public static boolean wants(PlayerEntity player, SmartMovingState state) {
        return SmartMoving.CONFIG.enableSmartFlying && player.getAbilities().flying
                && !player.hasVehicle() && !player.isGliding() && !state.swimming && !state.diving;
    }

    /**
     * 원작 handleAlternativeFlying(). PlayerEntity.travel() 시작에서 부른다.
     *
     * @return true 면 바닐라 비행을 취소한다
     */
    public static boolean travel(PlayerEntity player, SmartMovingState state, Vec3d input) {
        state.flying = wants(player, state);
        if (!state.flying) return false;
        SmartMovingConfig cfg = SmartMoving.CONFIG;

        // 바닐라가 travel() 전에 더한 위아래 비행 속도(비행 속도 x 3)를 되돌리고 원작 방식으로 다시 준다.
        double flyVertical = player.getAbilities().getFlySpeed() * 3.0;
        Vec3d v = player.getVelocity();
        double motionY = v.y;
        float moveUpward = 0F;
        if (state.prevSneakKey) {
            motionY += flyVertical;
            moveUpward -= 0.98F;
        }
        if (state.prevJumpKey) {
            motionY -= flyVertical;
            moveUpward += 0.98F;
        }
        player.setVelocity(v.x, motionY, v.z);

        float speedFactor = MoveFlying.baseSpeedFactor(player) * (state.liquidFast ? cfg.sprintFactor : 1F);
        MoveFlying.apply(player, moveUpward, (float) input.x, (float) input.z,
                speedFactor * 0.05F * cfg.flyingSpeedFactor, cfg.flyControlVertical);

        player.move(MovementType.SELF, player.getVelocity());

        Vec3d after = player.getVelocity();
        player.setVelocity(after.x * DAMPING, after.y * DAMPING, after.z * DAMPING);
        player.onLanding();
        return true;
    }
}
