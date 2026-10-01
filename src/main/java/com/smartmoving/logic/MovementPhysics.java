package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.climb.FreeClimbing;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * 바닐라 travel() 대신 실행되는 이동 물리.
 * 자기 캐릭터를 조종하는 쪽(클라이언트)에서만 호출된다.
 *
 * <p>벽 타기는 바닐라 이동을 그대로 쓰고 이동 직후에 logic.climb.FreeClimbing 이 세로 속도를 정한다.
 * 슬라이딩/헤드 점프는 원작 SmartMovingSelf.moveEntityWithHeading() 의 포트다.
 * 순서도 바닐라와 같다: 입력 가속 → 이동 → 중력과 감속.
 */
public final class MovementPhysics {
    /** 원작 SmartMovingContext 의 감속 상수 */
    private static final double HORIZONTAL_AIR_DAMPING = 0.91;
    private static final double HORIZONTAL_AERODYNAMIC_DAMPING = 0.999;
    private static final float AIR_ACCELERATION = 0.02f;

    private MovementPhysics() {
    }

    /**
     * @param input 바닐라 이동 입력 (x = 좌(+)/우(-), z = 앞(+)/뒤(-))
     * @return true 면 바닐라 travel 을 취소한다
     */
    public static boolean travel(PlayerEntity player, SmartMovingState state, Vec3d input) {
        if (state.sliding || state.headJumping) {
            FreeClimbing.reset(state);
        }
        if (state.sliding) {
            slide(player, input);
            return true;
        }
        if (state.headJumping) {
            headJump(player, state, input);
            return true;
        }
        return false;
    }

    /**
     * 슬라이딩: 입력 가속 없이 관성으로 미끄러진다.
     * 바닥 블록이 미끄러울수록(얼음) 덜 감속하고, 좌우 키로 조금씩 방향을 튼다.
     */
    private static void slide(PlayerEntity player, Vec3d input) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        Vec3d v = player.getVelocity();
        double motionX = v.x;
        double motionZ = v.z;
        double damping;

        if (player.isOnGround()) {
            // 바닐라 getVelocityAffectingPos() 와 같은 위치 (발 0.5칸 아래)
            BlockPos below = BlockPos.ofFloored(player.getX(), player.getBoundingBox().minY - 0.5000001, player.getZ());
            float slipperiness = player.getWorld().getBlockState(below).getBlock().getSlipperiness();
            damping = 1.0 / (((1.0 / slipperiness) - 1.0) / 25.0 * cfg.slideGlideFactor + 1.0) * 0.98;

            if (input.x != 0 && cfg.slideControlDegrees > 0) {
                double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
                if (horizontal > 1.0E-4) {
                    // 속도 방향을 yaw 각도로 보고, 누른 쪽으로 회전시킨다.
                    double angle = Math.atan2(-motionX, motionZ);
                    angle -= Math.toRadians(cfg.slideControlDegrees) * Math.signum(input.x);
                    motionX = horizontal * -Math.sin(angle);
                    motionZ = horizontal * Math.cos(angle);
                }
            }
        } else {
            damping = HORIZONTAL_AIR_DAMPING;
        }

        player.setVelocity(motionX, v.y, motionZ);
        player.move(MovementType.SELF, player.getVelocity());
        applyGravityAndDamping(player, damping);
    }

    /**
     * 헤드 점프: 공중 조작이 약하고(원작 0.2배),
     * 슬라이딩하다 떨어진 경우(aerodynamic)에는 공기 저항이 거의 없어 멀리 날아간다.
     */
    private static void headJump(PlayerEntity player, SmartMovingState state, Vec3d input) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        player.updateVelocity(AIR_ACCELERATION * cfg.headJumpControlFactor, input);
        player.move(MovementType.SELF, player.getVelocity());
        applyGravityAndDamping(player, state.aerodynamic ? HORIZONTAL_AERODYNAMIC_DAMPING : HORIZONTAL_AIR_DAMPING);
    }

    private static void applyGravityAndDamping(PlayerEntity player, double horizontalDamping) {
        Vec3d after = player.getVelocity();
        double y = (after.y - player.getFinalGravity()) * 0.98;
        player.setVelocity(after.x * horizontalDamping, y, after.z * horizontalDamping);
    }
}
