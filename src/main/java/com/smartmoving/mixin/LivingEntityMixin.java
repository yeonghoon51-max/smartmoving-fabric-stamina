package com.smartmoving.mixin;

import com.smartmoving.SmartMoving;
import com.smartmoving.logic.HeadJump;
import com.smartmoving.logic.MovementPhysics;
import com.smartmoving.logic.Stamina;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** 벽 타기 / 슬라이딩 중에는 바닐라 이동 대신 직접 만든 물리를 쓴다. */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void smartmoving$travel(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.isLogicalSideForUpdatingMovement()) return;
        if (MovementPhysics.travel(player, SmartMovingPlayer.of(player), movementInput)) {
            ci.cancel();
        }
    }

    /** 모아 뛰기: 충전량만큼 점프 속도를 키운다. */
    @Inject(method = "getJumpVelocity()F", at = @At("RETURN"), cancellable = true)
    private void smartmoving$chargedJump(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.getWorld().isClient) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        if (state.jumpCharge <= 0f) return;
        float multiplier = 1f + state.jumpCharge * (SmartMoving.CONFIG.chargedJumpMaxMultiplier - 1f);
        cir.setReturnValue(cir.getReturnValueF() * multiplier);
    }

    @Inject(method = "jump", at = @At("TAIL"))
    private void smartmoving$afterJump(CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.getWorld().isClient) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        if (state.jumpCharge > 0f) {
            Stamina.use(state, state.jumpCharge * SmartMoving.CONFIG.chargedJumpStaminaCost);
            state.jumpCharge = 0f;
        }
        if (state.headJumpArmed) {
            HeadJump.launch(player, state);
        }
    }
}
