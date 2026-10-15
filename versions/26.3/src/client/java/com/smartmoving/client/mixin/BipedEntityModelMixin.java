package com.smartmoving.client.mixin;

import com.smartmoving.client.SmartAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 원작 Smart Render 애니메이션: 스마트 무빙 동작 중인 플레이어의 팔·다리·머리 각도 (갑옷 모델에도 같이 적용됨). */
@Mixin(HumanoidModel.class)
public abstract class BipedEntityModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void smartmoving$animate(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState playerState)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity entity = client.level.getEntity(playerState.id);
        if (!(entity instanceof Player player)) return;
        SmartAnimations.apply((HumanoidModel<?>) (Object) this, state, player);
    }
}
