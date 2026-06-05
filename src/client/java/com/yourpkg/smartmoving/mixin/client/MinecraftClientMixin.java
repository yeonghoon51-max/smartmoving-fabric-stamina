package com.yourpkg.smartmoving.mixin.client;

import com.yourpkg.smartmoving.client.ui.ClientState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void smClamp(CallbackInfo ci) {
        // 네트워크 지연 등으로 인한 범위 초과 방지
        if (ClientState.jumpCharge < 0f)   ClientState.jumpCharge = 0f;
        if (ClientState.jumpCharge > 100f) ClientState.jumpCharge = 100f;
        if (ClientState.grabEnergy < 0f)   ClientState.grabEnergy = 0f;
        if (ClientState.grabEnergy > 100f) ClientState.grabEnergy = 100f;
    }
}
