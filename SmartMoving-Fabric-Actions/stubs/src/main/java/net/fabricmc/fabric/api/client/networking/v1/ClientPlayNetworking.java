package net.fabricmc.fabric.api.client.networking.v1;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;

public final class ClientPlayNetworking {
    public interface PlayChannelHandler<T extends CustomPayload> {
        void receive(T payload, Context context);
    }
    public interface Context {
        MinecraftClient client();
    }
    public static <T extends CustomPayload> void registerGlobalReceiver(CustomPayload.Id<T> id, PlayChannelHandler<T> handler) {}
    public static <T extends CustomPayload> void send(CustomPayload packet) {}
}
