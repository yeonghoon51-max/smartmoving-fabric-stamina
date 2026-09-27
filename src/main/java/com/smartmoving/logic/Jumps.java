package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 원작 SmartMovingSelf.tryJump() 의 포트.
 *
 * <p>핵심: 점프는 "지금 수평 속도 x 배율"로 계산된다. 달리기 점프는 x2 (상한 있음),
 * 헤드 점프는 같은 속도를 유지한 채 포물선 각도만 낮춘다.
 */
public final class Jumps {
    /** 원작 getMaxHorizontalMotion 의 걷기 기준 속도 */
    private static final double MAX_MOTION_WALK = 0.117852041920949;

    private Jumps() {
    }

    public static boolean tryJump(PlayerEntity player, SmartMovingState state, JumpType type, MoveSpeed speed,
                                  Float angleDegrees) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;

        boolean up = type != JumpType.SLIDE_DOWN;
        boolean head = type == JumpType.HEAD_UP || type == JumpType.CLIMB_BACK_HEAD;
        boolean charged = type == JumpType.CHARGE_UP;

        // ---- 지침 확인 ----
        float gain = 0f;
        float stop = Float.MAX_VALUE;
        boolean exhaustionApplies = true;
        switch (type) {
            case SLIDE_DOWN -> {
                gain = cfg.slideExhaustionGain;
                stop = cfg.slideExhaustionStop;
            }
            case CHARGE_UP -> {
                float ratio = Math.min(state.jumpCharge, cfg.jumpChargeMaximum) / (float) cfg.jumpChargeMaximum;
                gain = cfg.jumpChargeExhaustionGain * ratio;
                stop = cfg.jumpChargeExhaustionStop - gain;
            }
            // 원작 기본값에서 벽 점프는 지침이 없다 (move.jump.climb.exhaustion = false)
            case CLIMB_UP, CLIMB_BACK_UP, CLIMB_BACK_HEAD -> exhaustionApplies = false;
            default -> {
                if (speed == MoveSpeed.SPRINTING) {
                    gain = cfg.sprintJumpExhaustionGain;
                    stop = cfg.sprintJumpExhaustionStop;
                } else if (speed == MoveSpeed.RUNNING) {
                    gain = cfg.runJumpExhaustionGain;
                    stop = cfg.runJumpExhaustionStop;
                } else {
                    exhaustionApplies = false; // 걷기/웅크리기/제자리 점프는 원작 기본값에서 지침 없음
                }
            }
        }
        if (exhaustionApplies && !Exhaustion.allows(state, stop, gain)) {
            return false;
        }

        // ---- 배율 ----
        float jumpBoost = 1f;
        StatusEffectInstance effect = player.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (effect != null) jumpBoost = 1f + (effect.getAmplifier() + 1) * 0.2f;

        double horizontalFactor = horizontalFactor(type, speed, cfg) * jumpBoost;
        double verticalFactor = verticalFactor(type, cfg) * jumpBoost;
        double chargeFactor = 1.0;
        if (charged) {
            float ratio = Math.min(state.jumpCharge, cfg.jumpChargeMaximum) / (float) cfg.jumpChargeMaximum;
            chargeFactor = 1.0 + ratio * (cfg.jumpChargeFactor - 1.0);
        }

        if (!up) {
            horizontalFactor = Math.sqrt(horizontalFactor * horizontalFactor + verticalFactor * verticalFactor);
            verticalFactor = 0;
        }

        Vec3d velocity = player.getVelocity();
        double motionX = velocity.x;
        double motionY = velocity.y;
        double motionZ = velocity.z;

        Double maxHorizontalMotion = null;
        double horizontalMotion = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double verticalMotion = -0.078 + 0.498 * verticalFactor * chargeFactor;

        if (horizontalFactor > 1.0 && !player.horizontalCollision) {
            maxHorizontalMotion = maxHorizontalMotion(speed, cfg);
        }

        if (head && horizontalMotion > 0) {
            // 헤드 점프: 전체 속력은 그대로, 오래 모을수록 원래 각도에 가깝게 (짧게 모으면 납작하게)
            double normalAngle = Math.atan(verticalMotion / horizontalMotion);
            double totalMotion = Math.sqrt(verticalMotion * verticalMotion + horizontalMotion * horizontalMotion);
            double newAngle = headJumpFactor(state.headJumpCharge, cfg) * normalAngle;
            double newVerticalMotion = totalMotion * Math.sin(newAngle);
            double newHorizontalMotion = totalMotion * Math.cos(newAngle);
            if (maxHorizontalMotion != null) {
                maxHorizontalMotion = maxHorizontalMotion * (newHorizontalMotion / horizontalMotion);
            }
            verticalMotion = newVerticalMotion;
            horizontalMotion = newHorizontalMotion;
        }

        if (angleDegrees != null) {
            double rad = Math.toRadians(angleDegrees);
            double horizontal = Math.max(horizontalMotion, horizontalFactor);
            motionX += -Math.sin(rad) * horizontal;
            motionZ += Math.cos(rad) * horizontal;
            horizontalMotion = 0;
            verticalMotion = verticalFactor;
        }

        if (horizontalMotion > 0) {
            double absX = Math.abs(motionX) * horizontalFactor;
            double absZ = Math.abs(motionZ) * horizontalFactor;
            if (maxHorizontalMotion != null) {
                absX = Math.min(absX, maxHorizontalMotion * (horizontalFactor * (Math.abs(motionX) / horizontalMotion)));
                absZ = Math.min(absZ, maxHorizontalMotion * (horizontalFactor * (Math.abs(motionZ) / horizontalMotion)));
            }
            motionX = Math.signum(motionX) * absX;
            motionZ = Math.signum(motionZ) * absZ;
        }

        if (up) {
            motionY = verticalMotion;
            state.sprintJump = state.fast;
        }
        player.setVelocity(motionX, motionY, motionZ);

        if (exhaustionApplies) {
            Exhaustion.add(state, gain);
        }
        if (head) {
            state.headJumping = true;
            state.aerodynamic = false;
        }
        return true;
    }

    private static double horizontalFactor(JumpType type, MoveSpeed speed, SmartMovingConfig cfg) {
        switch (type) {
            case CLIMB_UP:
                return 1.0;
            case CLIMB_BACK_UP:
            case CLIMB_BACK_HEAD:
                return cfg.climbBackJumpHorizontal;
            default:
                break;
        }
        return switch (speed) {
            case SPRINTING -> cfg.sprintJumpHorizontalFactor;
            case RUNNING -> cfg.runJumpHorizontalFactor;
            case WALKING, SNEAKING -> 1.0;
            case STANDING -> 0.0;
        };
    }

    private static double verticalFactor(JumpType type, SmartMovingConfig cfg) {
        return switch (type) {
            case CLIMB_BACK_UP, CLIMB_BACK_HEAD -> cfg.climbBackJumpVertical;
            default -> 1.0;
        };
    }

    private static double maxHorizontalMotion(MoveSpeed speed, SmartMovingConfig cfg) {
        return MAX_MOTION_WALK * switch (speed) {
            case SPRINTING -> cfg.sprintFactor;
            case RUNNING -> 1.3;
            case SNEAKING -> 0.3;
            default -> 1.0;
        };
    }

    /** 원작 getHeadJumpFactor: 1틱 모으면 0 (완전 수평), 최대치면 1 (일반 점프 각도) */
    private static double headJumpFactor(int headJumpCharge, SmartMovingConfig cfg) {
        if (cfg.headJumpChargeMaximum <= 1) return 1.0;
        int charge = Math.min(headJumpCharge, cfg.headJumpChargeMaximum);
        return (charge - 1) / (double) (cfg.headJumpChargeMaximum - 1);
    }
}
