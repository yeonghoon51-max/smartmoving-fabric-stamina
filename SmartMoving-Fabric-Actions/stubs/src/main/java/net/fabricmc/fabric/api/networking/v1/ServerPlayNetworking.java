package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerPlayNetworking {
    public interface PlayPayloadHandler<T extends CustomPayload> {
        void receive(T payload, Context context);
    }
    public interface Context {
        ServerPlayerEntity player();
    }
    public static <T extends CustomPayload> void registerGlobalReceiver(CustomPayload.Id<T> id, PlayPayloadHandler<T> handler) {}
    public static <T extends CustomPayload> void send(ServerPlayerEntity player, CustomPayload payload) {}
}
