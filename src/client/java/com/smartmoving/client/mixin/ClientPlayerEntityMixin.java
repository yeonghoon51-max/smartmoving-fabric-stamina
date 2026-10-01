package com.smartmoving.client.mixin;

import com.smartmoving.client.ClientMovementController;
import com.smartmoving.state.SmartMovingPlayer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    /** 바닐라가 이동을 계산하기 직전에 스마트 무빙 상태를 갱신한다. */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void smartmoving$beforeMovement(CallbackInfo ci) {
        ClientMovementController.tick((ClientPlayerEntity) (Object) this);
    }

    /** 지쳐 있으면 달릴 수 없다. */
    @Inject(method = "canSprint", at = @At("RETURN"), cancellable = true)
    private void smartmoving$noSprintWhenExhausted(CallbackInfoReturnable<Boolean> cir) {
        if (SmartMovingPlayer.of((ClientPlayerEntity) (Object) this).sprintBlocked) {
            cir.setReturnValue(false);
        }
    }
}
