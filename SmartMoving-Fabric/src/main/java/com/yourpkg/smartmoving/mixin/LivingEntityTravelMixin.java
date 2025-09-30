package com.yourpkg.smartmoving.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {

    // LivingEntity#travel(Vec3d movementInput)
    // 이동 입력 벡터를 받을 때 살짝 개입할 수 있는 훅 (여기선 예시로 그대로 반환)
    @ModifyVariable(
            method = "travel",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Vec3d smartmoving$travel(Vec3d input) {
        // 필요 로직이 아직 없다면 그대로 반환해도 됩니다.
        return input;
    }
}
