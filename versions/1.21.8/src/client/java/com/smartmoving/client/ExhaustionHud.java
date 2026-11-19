package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Exhaustion;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * 원작 SmartMovingRender.renderGuiIngame() 의 포트.
 * 배고픔 줄 위: 번개 아이콘 10개 = 남은 체력(fitness). 회색 = 지금 행동을 계속하려면 필요한 만큼.
 * 체력 줄 위: 파란 다이아 = 모아 뛰기 / 헤드 점프 충전량.
 */
public final class ExhaustionHud {
    private static final Identifier ICONS = SmartMoving.id("textures/gui/icons.png");

    private ExhaustionHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) return;
        if (client.interactionManager == null || !client.interactionManager.hasStatusBars()) return;

        SmartMovingState state = SmartMovingPlayer.of(player);
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        if (cfg.showExhaustionBar && Exhaustion.enabled()) {
            renderExhaustion(context, player, state, width, height);
        }
        if (cfg.showJumpChargeBar) {
            renderJumpCharge(context, player, state, cfg, width, height);
        }
    }

    private static void renderExhaustion(DrawContext context, ClientPlayerEntity player, SmartMovingState state,
                                         int width, int height) {
        float maxExhaustion = Exhaustion.maximum();
        float exhaustion = Math.min(state.exhaustion, maxExhaustion);
        if (exhaustion <= 0) return;

        float maxExhaustionForAction = Math.min(state.maxExhaustionForAction, maxExhaustion);
        float maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, maxExhaustion);

        float fitness = maxExhaustion - exhaustion;
        float minFitnessForAction = maxExhaustion - maxExhaustionForAction;
        float minFitnessToStartAction = maxExhaustion - maxExhaustionToStartAction;
        float maxFitnessDrawn = Math.max(Math.max(minFitnessToStartAction, fitness), minFitnessForAction);

        int halfs = (int) Math.floor(maxFitnessDrawn / maxExhaustion * 21F);
        int fulls = halfs / 2;
        int half = halfs % 2;

        int fitnessHalfs = (int) Math.floor(fitness / maxExhaustion * 21F);
        int fitnessFulls = fitnessHalfs / 2;
        int fitnessHalf = fitnessHalfs % 2;

        int minFitnessForActionHalfs = (int) Math.floor(minFitnessForAction / maxExhaustion * 21F);
        int minFitnessForActionFulls = minFitnessForActionHalfs / 2;
        int minFitnessForActionHalf = minFitnessForActionHalfs % 2;

        int minFitnessToStartActionHalfs = (int) Math.floor(minFitnessToStartAction / maxExhaustion * 21F);
        int minFitnessToStartActionFulls = minFitnessToStartActionHalfs / 2;

        int y = height - 39 - 10 - (player.isSubmergedInWater() ? 10 : 0);
        for (int i = 0; i < Math.min(fulls + half, 10); i++) {
            int x = (width / 2 + 90) - (i + 1) * 8;
            int u;
            int v;
            if (i < fitnessFulls) {
                if (i < minFitnessForActionFulls) { u = 2; v = 2; }
                else if (i == minFitnessForActionFulls && minFitnessForActionHalf > 0) { u = 3; v = 2; }
                else { u = 0; v = 0; }
            } else if (i == fitnessFulls && fitnessHalf > 0) {
                if (i < minFitnessForActionFulls) { u = 1; v = 2; }
                else if (i == minFitnessForActionFulls && minFitnessForActionHalf > 0) {
                    if (i < minFitnessToStartActionFulls) { u = 3; v = 1; } else { u = 4; v = 2; }
                } else {
                    if (i < minFitnessToStartActionFulls) { u = 1; v = 1; } else { u = 1; v = 0; }
                }
            } else {
                if (i < minFitnessForActionFulls) { u = 0; v = 2; }
                else if (i == minFitnessForActionFulls && minFitnessForActionHalf > 0) {
                    if (i < minFitnessToStartActionFulls) { u = 2; v = 1; } else { u = 5; v = 2; }
                } else {
                    if (i < minFitnessToStartActionFulls) { u = 0; v = 1; } else { u = 4; v = 1; }
                }
            }
            drawIcon(context, x, y, u, v);
        }
    }

    private static void renderJumpCharge(DrawContext context, ClientPlayerEntity player, SmartMovingState state,
                                         SmartMovingConfig cfg, int width, int height) {
        float maxStillJumpCharge = cfg.jumpChargeMaximum;
        float stillJumpCharge = Math.min(state.jumpCharge, maxStillJumpCharge);
        float maxRunJumpCharge = cfg.headJumpChargeMaximum;
        float runJumpCharge = Math.min(state.headJumpCharge, maxRunJumpCharge);
        if (stillJumpCharge <= 0 && runJumpCharge <= 0) return;

        float maxJumpCharge = stillJumpCharge > runJumpCharge ? maxStillJumpCharge : maxRunJumpCharge;
        float jumpCharge = Math.max(stillJumpCharge, runJumpCharge);

        boolean max = jumpCharge == maxJumpCharge;
        int fulls = max ? 10 : (int) Math.ceil(((jumpCharge - 2) * 10D) / maxJumpCharge);
        int half = max ? 0 : (int) Math.ceil((jumpCharge * 10D) / maxJumpCharge) - fulls;

        int y = height - 39 - 10 - (player.getArmor() > 0 ? 10 : 0);
        for (int i = 0; i < fulls + half; i++) {
            int x = (width / 2 - 91) + i * 8;
            drawIcon(context, x, y, i < fulls ? 2 : 3, 0);
        }
    }

    private static void drawIcon(DrawContext context, int x, int y, int u, int v) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, x, y, u * 9f, v * 9f, 9, 9, 256, 256);
    }
}
