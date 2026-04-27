package net.fabricmc.fabric.api.client.networking.v1;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;
public final class ClientPlayNetworking {
    public interface Context {
        MinecraftClient client();
    }
    @FunctionalInterface
    public interface PlayPayloadHandler<P extends CustomPayload> {
        void receive(P payload, Context context);
    }
    public static <P extends CustomPayload> void registerGlobalReceiver(
            CustomPayload.Id<P> id, PlayPayloadHandler<P> handler) {}
    public static <P extends CustomPayload> void send(P payload) {}
}
