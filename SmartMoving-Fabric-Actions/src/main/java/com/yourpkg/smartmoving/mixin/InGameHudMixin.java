package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.client.ui.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderHud(DrawContext ctx, float tickDelta, CallbackInfo ci) {
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        int seg = Math.max(1, SmartMovingMod.CONFIG.hudSegments);
        int segW = 8, segH = 6, pad = 1;

        float jr = clamp01(ClientState.jumpCharge / 100f);
        int jFilled = Math.round(jr * seg);
        int jx = sw / 2 - 91;
        int jy = sh - 50;

        for (int i = 0; i < seg; i++) {
            int x = jx + i * (segW + pad);
            int y = jy;
            ctx.fill(x, y, x + segW, y + segH, 0x88000000);
            if (i < jFilled) ctx.fill(x, y, x + segW, y + segH, 0xFF2EA3FF);
        }

        float gr = clamp01(ClientState.grabEnergy / 100f);
        int gFilled = Math.round(gr * seg);
        int gx = sw - 10;
        int gy = sh - 50 - (segH + pad);

        for (int i = 0; i < seg; i++) {
            int x = gx - i * (segW + pad) - segW;
            int y = gy;
            ctx.fill(x, y, x + segW, y + segH, 0x88000000);
            if (i < gFilled) ctx.fill(x, y, x + segW, y + segH, 0xFFFFD94D);
        }
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
