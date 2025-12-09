package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public final class ClientTickEvents {
    public interface EndTick { void onEndTick(MinecraftClient client); }
    public static final End end = new End();
    public static final EndTick END = client -> {};
    public static final EndTickInvoker END_CLIENT_TICK = new End();

    public static class End implements EndTickInvoker {
        private final List<EndTick> handlers = new ArrayList<>();
        @Override public void register(EndTick handler) { handlers.add(handler); }
        public void invoke(MinecraftClient client) { handlers.forEach(h -> h.onEndTick(client)); }
    }

    public interface EndTickInvoker { void register(EndTick handler); }
}
