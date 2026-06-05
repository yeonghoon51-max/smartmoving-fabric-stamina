package com.yourpkg.smartmoving.mixin.client;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.client.ui.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
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
    private void smRenderHud(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (client.player == null) return;

        int sw  = client.getWindow().getScaledWidth();
        int sh  = client.getWindow().getScaledHeight();
        int seg = Math.max(1, Math.min(20, SmartMovingMod.CONFIG.hudSegments));

        final int segW = 8, segH = 6, pad = 1;
        final int totalW = seg * (segW + pad) - pad;

        // ── 충전 점프 바 (파란색, 화면 하단 중앙 왼쪽) ──
        float jr = clamp01(ClientState.jumpCharge / 100f);
        int jFilled = Math.round(jr * seg);
        int jx = sw / 2 - 91;
        int jy = sh - 49;
        for (int i = 0; i < seg; i++) {
            int x = jx + i * (segW + pad);
            ctx.fill(x, jy, x + segW, jy + segH, 0x88000000);
            if (i < jFilled) ctx.fill(x, jy, x + segW, jy + segH, 0xFF2EA3FF);
        }

        // ── 잡기 스태미나 바 (노란색, 20% 이하 빨간 경고) ──
        float gr = clamp01(ClientState.grabEnergy / 100f);
        int gFilled = Math.round(gr * seg);
        int gx = sw - totalW - 8;
        int gy = sh - 49;
        for (int i = 0; i < seg; i++) {
            int x = gx + i * (segW + pad);
            ctx.fill(x, gy, x + segW, gy + segH, 0x88000000);
            if (i < gFilled) {
                int color = (gr <= 0.2f) ? 0xFFFF4444 : 0xFFFFD94D;
                ctx.fill(x, gy, x + segW, gy + segH, color);
            }
        }

        // 잡기 활성화 시 깜박이는 표시
        if (ClientState.grabbing) {
            long t = System.currentTimeMillis() / 400;
            if ((t & 1) == 0) {
                ctx.fill(gx - 10, gy + 1, gx - 5, gy + segH - 1, 0xFFFF8800);
            }
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
