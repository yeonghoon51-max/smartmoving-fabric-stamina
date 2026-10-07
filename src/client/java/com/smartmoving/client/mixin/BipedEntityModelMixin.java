package com.smartmoving.client.mixin;

import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
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

/** 벽을 타거나 천장에 매달린 플레이어는 두 팔을 위로 뻗는다 (갑옷 모델에도 같이 적용됨). */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
    private void smartmoving$climbPose(BipedEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(playerState.id);
        if (!(entity instanceof PlayerEntity player)) return;
        SmartMovingState sm = SmartMovingPlayer.of(player);
        if (!sm.climbing && !sm.ceilingClimbing) return;

        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        float swing = MathHelper.sin(state.age * 0.35f) * 0.35f;
        if (sm.ceilingClimbing) {
            // 천장: 두 팔을 똑바로 위로, 움직일 때만 번갈아 잡는다
            swing = MathHelper.sin(state.limbSwingAnimationProgress * 0.6662f) * 0.3f * state.limbSwingAmplitude;
            armsUp(model, swing);
            return;
        }
        rightArm.pitch = -2.8f + swing;
        leftArm.pitch = -2.8f - swing;
        rightArm.yaw = 0f;
        leftArm.yaw = 0f;
        rightArm.roll = 0.1f;
        leftArm.roll = -0.1f;
        model.rightLeg.pitch = -swing * 0.8f;
        model.leftLeg.pitch = swing * 0.8f;
    }

    private static void armsUp(BipedEntityModel<?> model, float swing) {
        model.rightArm.pitch = (float) Math.PI + swing;
        model.leftArm.pitch = (float) Math.PI - swing;
        model.rightArm.yaw = 0f;
        model.leftArm.yaw = 0f;
        model.rightArm.roll = 0f;
        model.leftArm.roll = 0f;
    }
}
