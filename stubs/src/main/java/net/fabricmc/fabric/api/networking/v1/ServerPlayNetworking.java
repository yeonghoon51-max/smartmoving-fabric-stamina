package net.fabricmc.fabric.api.networking.v1;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
public final class ServerPlayNetworking {
    public interface Context {
        ServerPlayerEntity player();
    }
    @FunctionalInterface
    public interface PlayPayloadHandler<P extends CustomPayload> {
        void receive(P payload, Context context);
    }
    public static <P extends CustomPayload> void registerGlobalReceiver(
            CustomPayload.Id<P> id, PlayPayloadHandler<P> handler) {}
    public static <P extends CustomPayload> void send(ServerPlayerEntity player, P payload) {}
}
