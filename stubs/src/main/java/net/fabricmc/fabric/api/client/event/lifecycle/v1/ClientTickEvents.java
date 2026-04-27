package net.fabricmc.fabric.api.client.event.lifecycle.v1;
import net.minecraft.client.MinecraftClient;
public final class ClientTickEvents {
    @FunctionalInterface
    public interface Client {
        void onEndTick(MinecraftClient client);
    }
    public static class EndClientTickEvent {
        public void register(Client callback) {}
    }
    public static final EndClientTickEvent END_CLIENT_TICK = new EndClientTickEvent();
}
