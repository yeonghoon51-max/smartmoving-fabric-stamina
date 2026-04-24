package com.yourpkg.smartmoving.client.net;

import com.yourpkg.smartmoving.client.ui.ClientState;
import com.yourpkg.smartmoving.network.payload.ActionPayload;
import com.yourpkg.smartmoving.network.payload.SyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworking {

    public static void register() {
        // 서버에서 보내는 상태 동기화 수신
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientState.jumpCharge = payload.jumpCharge();
                    ClientState.grabEnergy = payload.grabEnergy();
                    ClientState.grabbing   = payload.grabbing();
                    ClientState.crawling   = payload.crawling();
                }));
    }

    /** 잡기 키 상태를 서버로 전송 (1=누름, 2=떼기) */
    public static void sendAction(byte actionId) {
        ClientPlayNetworking.send(new ActionPayload(actionId));
    }
}
