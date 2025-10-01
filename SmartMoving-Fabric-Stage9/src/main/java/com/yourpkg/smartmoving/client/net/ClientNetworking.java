package com.yourpkg.smartmoving.client.net;

import com.yourpkg.smartmoving.client.ui.ClientState;
import com.yourpkg.smartmoving.network.payload.ActionPayload;
import com.yourpkg.smartmoving.network.payload.SyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ClientNetworking {
    public static void register() {
        // Codec 등록
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);

        // S2C 동기화 수신
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientState.jumpCharge = payload.jumpCharge();
                ClientState.grabEnergy = payload.grabEnergy();
                ClientState.grabbing   = payload.grabbing();
                ClientState.crawling   = payload.crawling();
            });
        });
    }

    /** 1 = grab down, 2 = grab up */
    public static void sendAction(byte actionId) {
        ClientPlayNetworking.send(new ActionPayload(actionId));
    }
}
