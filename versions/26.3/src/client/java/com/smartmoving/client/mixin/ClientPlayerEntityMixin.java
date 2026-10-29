package com.smartmoving.client.mixin;

import com.smartmoving.client.ClientMovementController;
import com.smartmoving.state.SmartMovingPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    /** 바닐라가 이동을 계산하기 직전에 스마트 무빙 상태를 갱신한다. */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void smartmoving$beforeMovement(CallbackInfo ci) {
        ClientMovementController.tick((LocalPlayer) (Object) this);
    }

    /**
     * 틈으로 기어 오르는 중에는 납작한 자세여도 바닐라 기어가기 감속(0.3배)을 하지 않는다.
     * 원작도 이때는 기어가기 속도 배율을 쓰지 않았다.
     */
    @Inject(method = "isMovingSlowly", at = @At("RETURN"), cancellable = true)
    private void smartmoving$noSlowWhileClimbCrawling(CallbackInfoReturnable<Boolean> cir) {
        if (SmartMovingPlayer.of((LocalPlayer) (Object) this).climbCrawling) {
            cir.setReturnValue(false);
        }
    }
}
