package com.yourpkg.smartmoving.network;

import com.yourpkg.smartmoving.network.payload.ActionPayload;
import com.yourpkg.smartmoving.network.payload.SyncPayload;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerNetworking {

    public static void register() {
        // 패킷 타입 등록
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID,   SyncPayload.CODEC);

        // 클라이언트에서 받은 잡기 키 상태 처리
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            var server = player.getServer();
            if (server == null) return;
            server.execute(() -> {
                if (!(player instanceof PlayerContext.Holder h)) return;
                PlayerContext playerCtx = h.smartmoving$getContext();
                byte action = payload.actionId();
                if (action == 1) {
                    // 키 누름: 스태미나 있으면 잡기 활성화
                    if (playerCtx.grabEnergy > 0f) playerCtx.grabbing = true;
                } else if (action == 2) {
                    // 키 떼기
                    playerCtx.grabbing = false;
                }
                sync(player, playerCtx);
            });
        });
    }

    /** 서버 → 클라이언트 상태 동기화 전송 */
    public static void sync(ServerPlayerEntity player, PlayerContext ctx) {
        ServerPlayNetworking.send(player,
                new SyncPayload(ctx.jumpCharge, ctx.grabEnergy, ctx.grabbing, ctx.crawling));
    }
}
