package com.smartmoving.client.mixin;

import com.smartmoving.client.SmartAnimations;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 원작 Smart Render 애니메이션: 스마트 무빙 동작 중인 플레이어의 팔·다리·머리 각도 (갑옷 모델에도 같이 적용됨). */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void smartmoving$animate(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
                                     float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;
        SmartAnimations.apply((BipedEntityModel<?>) (Object) this, player, animationProgress, headYaw, headPitch);
    }
}
