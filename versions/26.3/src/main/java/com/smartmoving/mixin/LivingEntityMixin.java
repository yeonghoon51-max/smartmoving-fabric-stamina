package com.smartmoving.mixin;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.MovementPhysics;
import com.smartmoving.logic.WallJumping;
import com.smartmoving.logic.climb.FreeClimbing;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** 바닐라 달리기 속도 배율 */
    private static final float VANILLA_SPRINT_FACTOR = 1.3f;

    /** 벽 타기 준비: 이전 틱 벽 타기 상태로 이동 입력을 바꾸고 상태를 초기화한다 (원작 moveEntityWithHeading 앞부분). */
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d smartmoving$beforeTravel(Vec3d movementInput) {
        if (!((Object) this instanceof PlayerEntity player)) return movementInput;
        if (!player.isLogicalSideForUpdatingMovement() || !player.getWorld().isClient) return movementInput;
        return FreeClimbing.beforeTravel(player, SmartMovingPlayer.of(player), movementInput);
    }

    /** 바닐라 이동이 끝난 뒤: 벽 타기(손/발로 잡을 곳을 찾아 세로 속도 결정), 그다음 벽 점프. 원작 순서 그대로. */
    @Inject(method = "travel", at = @At("RETURN"))
    private void smartmoving$afterTravel(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.isLogicalSideForUpdatingMovement() || !player.getWorld().isClient) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        FreeClimbing.afterTravel(player, state);
        WallJumping.afterTravel(player, state);
    }

    /** 슬라이딩 / 헤드 점프 중에는 바닐라 이동 대신 직접 만든 물리를 쓴다. */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void smartmoving$travel(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.isLogicalSideForUpdatingMovement()) return;
        if (MovementPhysics.travel(player, SmartMovingPlayer.of(player), movementInput)) {
            ci.cancel();
        }
    }

    /** 점프는 원작처럼 이 모드가 직접 계산하므로 바닐라 점프를 막는다 (ClientMovementController 참고). */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void smartmoving$replaceVanillaJump(CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.getWorld().isClient) return;
        if (SmartMovingPlayer.of(player).handlesJumps) {
            ci.cancel();
        }
    }

    /** 달리기 키를 누르고 달리면 원작처럼 걷기의 1.5배 (바닐라는 1.3배) */
    @Inject(method = "getMovementSpeed(F)F", at = @At("RETURN"), cancellable = true)
    private void smartmoving$sprintSpeed(float slipperiness, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.getWorld().isClient || !player.isOnGround()) return;
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        if (cfg.enableSprint && SmartMovingPlayer.of(player).fast) {
            cir.setReturnValue(cir.getReturnValueF() * cfg.sprintFactor / VANILLA_SPRINT_FACTOR);
        }
    }

    /** 머리부터 착지하면 더 아프다 (원작: 2칸부터, 2배) */
    @Inject(method = "computeFallDamage", at = @At("RETURN"), cancellable = true)
    private void smartmoving$headFallDamage(double fallDistance, float damagePerDistance,
                                            CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (player.getWorld().isClient) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        if (!state.headJumping && player.age - state.headJumpEndAge > 5) return;

        SmartMovingConfig cfg = SmartMoving.CONFIG;
        if (fallDistance < cfg.headFallDamageStartDistance) return;
        int headDamage = MathHelper.ceil((fallDistance - cfg.headFallDamageStartDistance) * cfg.headFallDamageFactor);
        cir.setReturnValue(Math.max(cir.getReturnValueI(), headDamage));
    }
}
