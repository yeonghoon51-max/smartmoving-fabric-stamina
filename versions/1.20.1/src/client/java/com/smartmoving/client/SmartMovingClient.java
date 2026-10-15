package com.smartmoving.client;

import com.smartmoving.network.SmartMovingNetworking;
import com.smartmoving.state.SmartMovingPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** 클라이언트 전용 진입점: 키 등록, HUD 등록, 다른 플레이어 상태 수신 */
public class SmartMovingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        SmartAnimations.register();

        HudRenderCallback.EVENT.register(ExhaustionHud::render);

        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetworking.STATE_SYNC, (client, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt();
            int flags = buf.readVarInt();
            client.execute(() -> {
                if (client.world == null) return;
                Entity entity = client.world.getEntityById(entityId);
                // 내 캐릭터 상태는 내가 직접 계산하므로 덮어쓰지 않는다.
                if (entity instanceof PlayerEntity player && player != client.player) {
                    SmartMovingPlayer.of(player).applyFlags(flags);
                }
            });
        });
    }
}
