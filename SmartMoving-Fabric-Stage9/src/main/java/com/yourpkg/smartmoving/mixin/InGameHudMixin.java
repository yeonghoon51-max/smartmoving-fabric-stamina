package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.client.ui.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow @Final private MinecraftClient client;

    private static final Identifier ICONS = Identifier.of("smartmoving", "textures/gui/icons.png");
    private static final int ICON_W = 9, ICON_H = 9, PAD = 1;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderHud(DrawContext ctx, float tickDelta, CallbackInfo ci) {
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int seg = Math.max(1, SmartMovingMod.CONFIG.hudSegments);

        // Blue arrows: jumpCharge (left to right)
        float ratioJ = clamp01(ClientState.jumpCharge / 100f);
        float jScaled = ratioJ * seg;
        int jFull = (int) jScaled;
        float jPart = jScaled - jFull;

        int ax = sw / 2 - 91;
        int ay = sh - 50;
        for (int i = 0; i < seg; i++) {
            int x = ax + i * (ICON_W + PAD);
            ctx.drawTexture(ICONS, x, ay, 0, 0, ICON_W, ICON_H, 256, 256); // arrow empty
            if (i < jFull) ctx.drawTexture(ICONS, x, ay, 0, 9, ICON_W, ICON_H, 256, 256);
            else if (i == jFull && jPart > 0f) {
                int pw = Math.max(1, Math.min(ICON_W, (int)(ICON_W * jPart)));
                ctx.drawTexture(ICONS, x, ay, 0, 9, pw, ICON_H, 256, 256);
            }
        }

        // Lightning: grabEnergy (right-aligned)
        float ratioG = clamp01(ClientState.grabEnergy / 100f);
        float gScaled = ratioG * seg;
        int gFull = (int) gScaled;
        float gPart = gScaled - gFull;

        int startRight = sw - 10;
        int by = sh - 49 - (ICON_H + PAD);
        for (int i = 0; i < seg; i++) {
            int x = startRight - i * (ICON_W + PAD);
            ctx.drawTexture(ICONS, x, by, 9, 0, ICON_W, ICON_H, 256, 256); // lightning empty
            if (i < gFull) ctx.drawTexture(ICONS, x, by, 9, 9, ICON_W, ICON_H, 256, 256);
            else if (i == gFull && gPart > 0f) {
                int pw = Math.max(1, Math.min(ICON_W, (int)(ICON_W * gPart)));
                ctx.drawTexture(ICONS, x, by, 9, 9, pw, ICON_H, 256, 256);
            }
        }
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
