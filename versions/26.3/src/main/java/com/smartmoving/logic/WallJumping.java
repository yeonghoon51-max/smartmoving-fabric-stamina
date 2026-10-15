// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.handleWallJumping), GPL-3.0-or-later.
package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 원작 벽 점프: 공중에서 점프 키를 두 번 누르고 누른 채로 벽에 부딪히면, 들어온 각도의 반사 방향으로 튕겨 나간다.
 * 잡기 키를 누르고 있으면 헤드 점프. 점프 키를 계속 누르고 있으면 다음 벽에서도 이어서 튕긴다.
 */
public final class WallJumping {
    private WallJumping() {
    }

    /** 원작 beforeMoveEntity(): 이번 이동이 어느 쪽 벽에 막히는지 */
    public static void beforeMove(PlayerEntity player, SmartMovingState state, Vec3d movement) {
        World world = player.getWorld();
        Box box = player.getBoundingBox().contract(1.0E-7);
        boolean blockedX = movement.x != 0 && !world.isSpaceEmpty(player, box.offset(movement.x, 0, 0));
        boolean blockedZ = movement.z != 0 && !world.isSpaceEmpty(player, box.offset(0, 0, movement.z));
        // 원작은 (Z+, Z-, X+, X-) 순서로 넘긴다
        state.horizontalCollisionAngle = horizontalCollisionAngle(
                blockedZ && movement.z > 0, blockedZ && movement.z < 0,
                blockedX && movement.x > 0, blockedX && movement.x < 0);
    }

    /** 원작 handleWallJumping(): 이동이 끝난 뒤 */
    public static void afterTravel(PlayerEntity player, SmartMovingState state) {
        if (!state.wantWallJumping || Float.isNaN(state.horizontalCollisionAngle)) return;
        SmartMovingConfig cfg = SmartMoving.CONFIG;

        JumpType type;
        if (state.grabKey && cfg.enableWallHeadJump) {
            if (player.fallDistance > cfg.wallHeadJumpFallMaximumDistance) return;
            type = state.wasCollidedHorizontally ? JumpType.WALL_HEAD_SLIDE : JumpType.WALL_HEAD;
        } else {
            if (player.fallDistance > cfg.wallUpJumpFallMaximumDistance) return;
            type = state.wasCollidedHorizontally ? JumpType.WALL_UP_SLIDE : JumpType.WALL_UP;
        }

        float jumpAngle;
        if (!state.wasCollidedHorizontally) {
            // 들어온 방향을 벽에 대해 반사한다
            float movementAngle = angle(state.jumpMotionZ, -state.jumpMotionX);
            if (Float.isNaN(movementAngle)) return;
            jumpAngle = state.horizontalCollisionAngle * 2 - movementAngle + 180F;
        } else {
            jumpAngle = state.horizontalCollisionAngle;
        }

        while (jumpAngle > 360F) jumpAngle -= 360F;

        if (cfg.wallJumpOrthogonalTolerance != 0F) {
            float aligned = jumpAngle;
            while (aligned > 45F) aligned -= 90F;
            if (Math.abs(aligned) < cfg.wallJumpOrthogonalTolerance) {
                jumpAngle = Math.round(jumpAngle / 90F) * 90F;
            }
        }

        if (Jumps.tryJump(player, state, type, MoveSpeed.STANDING, jumpAngle, state.jumpMotionX, state.jumpMotionZ)) {
            state.continueWallJumping = !state.headJumping;
            player.horizontalCollision = false;
            player.setYaw(jumpAngle);
        }
    }

    /** 원작 SmartRenderUtilities.getHorizontalCollisionangle (인자 이름은 원작 그대로) */
    private static float horizontalCollisionAngle(boolean positiveX, boolean negativeX, boolean positiveZ, boolean negativeZ) {
        if (positiveX) {
            if (negativeX) {
                if (positiveZ) {
                    if (!negativeZ) return 90F;
                } else if (negativeZ) {
                    return 270F;
                }
            } else {
                if (positiveZ) return negativeZ ? 0F : 45F;
                return negativeZ ? 315F : 0F;
            }
        } else if (negativeX) {
            if (positiveZ) return negativeZ ? 180F : 135F;
            return negativeZ ? 225F : 180F;
        } else {
            if (positiveZ) {
                if (!negativeZ) return 90F;
            } else if (negativeZ) {
                return 270F;
            }
        }
        return Float.NaN;
    }

    /** 원작 SmartRenderUtilities.getAngle */
    private static float angle(double x, double y) {
        if (x == 0) {
            if (y == 0) return Float.NaN;
            return y < 0 ? 270F : 90F;
        }
        if (y == 0) return x < 0 ? 180F : 0F;
        float angle = (float) Math.toDegrees(Math.atan(y / x));
        if (x < 0) return 180F + angle;
        if (y < 0) return 360F + angle;
        return angle;
    }
}
