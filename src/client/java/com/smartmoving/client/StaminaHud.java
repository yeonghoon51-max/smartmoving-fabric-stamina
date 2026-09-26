package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/**
 * 스태미나 바(배고픔 줄 위, 오른쪽)와 모아 뛰기 게이지(조준점 아래)를 그린다.
 */
public final class StaminaHud {
    private static final int BAR_WIDTH = 81;
    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_STAMINA = 0xFFFFD23F;
    private static final int COLOR_EXHAUSTED = 0xFFD9453B;
    private static final int COLOR_CHARGE = 0xFF3FA9FF;

    private StaminaHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        ClientPlayerEntity player = client.player;
        if (!cfg.showHud || player == null || client.options.hudHidden) return;
        if (client.interactionManager == null || !client.interactionManager.hasStatusBars()) return;

        SmartMovingState state = SmartMovingPlayer.of(player);
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // ---- 스태미나 바: 꽉 차 있을 때는 숨긴다 ----
        if (cfg.enableStamina && state.stamina >= 0f && (state.stamina < cfg.maxStamina || state.climbing)) {
            int right = width / 2 + 91;
            int left = right - BAR_WIDTH;
            // 배고픔 줄(h-39) 위. 물속에서 공기 방울이 보이면 한 줄 더 위로.
            boolean airShown = player.isSubmergedInWater() || player.getAir() < player.getMaxAir();
            int y = height - 39 - 7 - (airShown ? 10 : 0);

            float ratio = Math.max(0f, Math.min(1f, state.stamina / cfg.maxStamina));
            int filled = Math.round(ratio * (BAR_WIDTH - 2));
            context.fill(left, y, right, y + 4, COLOR_BG);
            // 오른쪽에서 왼쪽으로 채워진다 (배고픔 줄과 같은 방향)
            context.fill(right - 1 - filled, y + 1, right - 1, y + 3,
                    state.exhausted ? COLOR_EXHAUSTED : COLOR_STAMINA);
        }

        // ---- 모아 뛰기 게이지 ----
        if (state.jumpCharge > 0f) {
            int w = 30;
            int x = width / 2 - w / 2;
            int y = height / 2 + 10;
            int filled = Math.round(state.jumpCharge * (w - 2));
            context.fill(x, y, x + w, y + 4, COLOR_BG);
            context.fill(x + 1, y + 1, x + 1 + filled, y + 3, COLOR_CHARGE);
        }
    }
}
