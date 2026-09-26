package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * 바닐라 travel() 대신 실행되는 이동 물리.
 * 자기 캐릭터를 조종하는 쪽(클라이언트)에서만 호출된다.
 */
public final class MovementPhysics {
    private static final double GRAVITY = 0.08;

    private MovementPhysics() {
    }

    /**
     * @param input 바닐라 이동 입력 (x = 좌(+)/우(-), z = 앞(+)/뒤(-))
     * @return true 면 바닐라 travel 을 취소한다
     */
    public static boolean travel(PlayerEntity player, SmartMovingState state, Vec3d input) {
        if (state.climbing) {
            climb(player, input);
            return true;
        }
        if (state.sliding) {
            slide(player);
            return true;
        }
        return false;
    }

    /** 벽 타기: 중력 없이 앞키=위, 뒤키=아래, 좌우키=벽을 따라 옆으로 */
    private static void climb(PlayerEntity player, Vec3d input) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        Direction facing = player.getHorizontalFacing();
        Direction left = facing.rotateYCounterclockwise();

        double up = 0;
        if (input.z > 0) up = cfg.climbUpSpeed;
        else if (input.z < 0) up = -cfg.climbDownSpeed;

        double side = Math.signum(input.x) * cfg.climbSideSpeed;
        // 벽 쪽으로 살짝 밀어서 붙어 있게 한다.
        double stick = 0.04;
        Vec3d motion = new Vec3d(
                facing.getOffsetX() * stick + left.getOffsetX() * side,
                up,
                facing.getOffsetZ() * stick + left.getOffsetZ() * side);

        player.move(MovementType.SELF, motion);
        player.setVelocity(Vec3d.ZERO);
        player.onLanding(); // 낙하 거리 초기화
    }

    /** 슬라이딩: 입력 없이 관성으로 미끄러지며 천천히 감속 */
    private static void slide(PlayerEntity player) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        Vec3d v = player.getVelocity();
        double friction = player.isOnGround() ? cfg.slideFriction : 0.98;
        Vec3d motion = new Vec3d(v.x * friction, v.y - GRAVITY, v.z * friction);
        player.setVelocity(motion);
        player.move(MovementType.SELF, player.getVelocity());
        Vec3d after = player.getVelocity();
        player.setVelocity(after.x, after.y * 0.98, after.z);
    }
}
