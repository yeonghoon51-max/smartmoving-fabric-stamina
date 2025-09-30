package com.yourpkg.smartmoving.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void smartmoving$getDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        // 아직 로직이 없으면 아무 것도 하지 않아도 됩니다.
        // 추후 슬라이딩 상태일 때만 cir.setReturnValue(...) 로 낮은 치수를 반환하도록 구현하세요.
    }
}
