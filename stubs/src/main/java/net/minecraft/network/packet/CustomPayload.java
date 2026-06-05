package net.minecraft.network.packet;
import net.minecraft.util.Identifier;
public interface CustomPayload {
    record Id<P extends CustomPayload>(Identifier id) {}
    Id<? extends CustomPayload> getId();
}
