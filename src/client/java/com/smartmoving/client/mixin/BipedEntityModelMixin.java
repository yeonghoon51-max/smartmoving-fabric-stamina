package com.smartmoving.client.mixin;

import com.smartmoving.state.SmartMovingPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 벽을 타는 플레이어는 두 팔을 위로 뻗고 번갈아 움직인다 (갑옷 모델에도 같이 적용됨). */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
    private void smartmoving$climbPose(BipedEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(playerState.id);
        if (!(entity instanceof PlayerEntity player) || !SmartMovingPlayer.of(player).climbing) return;

        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        float swing = MathHelper.sin(state.age * 0.35f) * 0.35f;
        rightArm.pitch = -2.8f + swing;
        leftArm.pitch = -2.8f - swing;
        rightArm.yaw = 0f;
        leftArm.yaw = 0f;
        rightArm.roll = 0.1f;
        leftArm.roll = -0.1f;
        model.rightLeg.pitch = -swing * 0.8f;
        model.leftLeg.pitch = swing * 0.8f;
    }
}
