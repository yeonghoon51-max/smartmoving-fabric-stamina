package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public final class PayloadTypeRegistry {
    public static PayloadTypeRegistry playC2S() { return new PayloadTypeRegistry(); }
    public static PayloadTypeRegistry playS2C() { return new PayloadTypeRegistry(); }
    public <B extends net.minecraft.network.RegistryByteBuf, T extends CustomPayload> void register(CustomPayload.Id<T> id, PacketCodec<B, T> codec) {}
}
