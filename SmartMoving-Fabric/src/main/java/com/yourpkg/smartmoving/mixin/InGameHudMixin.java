package com.yourpkg.smartmoving.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void smartmoving$render(DrawContext ctx, float tickDelta, CallbackInfo ci) {
        // HUD 커스텀 렌더링은 여기서. (필요 없으면 비워두셔도 됩니다)
    }
}
