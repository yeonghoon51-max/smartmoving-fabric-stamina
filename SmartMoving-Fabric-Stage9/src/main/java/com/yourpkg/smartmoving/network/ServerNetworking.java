package com.yourpkg.smartmoving.network;

import com.yourpkg.smartmoving.network.payload.ActionPayload;
import com.yourpkg.smartmoving.network.payload.SyncPayload;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerNetworking {

    public static void register() {
        // Codec 등록
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID,   SyncPayload.CODEC);

        // C2S 액션 수신(그랩 on/off)
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            player.server.execute(() -> {
                if (!(player instanceof PlayerContext.Holder h)) return;
                PlayerContext ctx = h.smartmoving$getContext();
                byte a = payload.actionId();
                if (a == 1) ctx.grabbing = true;
                else if (a == 2) ctx.grabbing = false;
                sync(player, ctx);
            });
        });
    }

    public static void sync(ServerPlayerEntity player, PlayerContext ctx) {
        ServerPlayNetworking.send(player,
                new SyncPayload(ctx.jumpCharge, ctx.grabEnergy, ctx.grabbing, ctx.crawling));
    }
}
