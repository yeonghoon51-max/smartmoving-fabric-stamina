package com.yourpkg.smartmoving.client.net;

import com.yourpkg.smartmoving.client.ui.ClientState;
import com.yourpkg.smartmoving.network.Channels;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public class ClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Channels.SYNC, (client, handler, buf, responseSender) -> {
            float jumpCharge = buf.readFloat();
            float grabEnergy = buf.readFloat();
            boolean grabbing = buf.readBoolean();
            boolean crawling = buf.readBoolean();
            client.execute(() -> {
                ClientState.jumpCharge = jumpCharge;
                ClientState.grabEnergy = grabEnergy;
                ClientState.grabbing = grabbing;
                ClientState.crawling = crawling;
            });
        });
    }

    public static void sendAction(byte actionId) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeByte(actionId);
        ClientPlayNetworking.send(Channels.ACTION, buf);
    }
}
