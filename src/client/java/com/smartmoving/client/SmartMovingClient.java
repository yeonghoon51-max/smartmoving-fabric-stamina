package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import com.smartmoving.network.StateSyncS2CPayload;
import com.smartmoving.state.SmartMovingPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** 클라이언트 전용 진입점: 키 등록, HUD 등록, 다른 플레이어 상태 수신 */
public class SmartMovingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();

        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, SmartMoving.id("stamina"), StaminaHud::render);

        ClientPlayNetworking.registerGlobalReceiver(StateSyncS2CPayload.ID, (payload, context) -> {
            if (context.client().world == null) return;
            Entity entity = context.client().world.getEntityById(payload.entityId());
            // 내 캐릭터 상태는 내가 직접 계산하므로 덮어쓰지 않는다.
            if (entity instanceof PlayerEntity player && player != context.client().player) {
                SmartMovingPlayer.of(player).applyFlags(payload.flags());
            }
        });
    }
}
