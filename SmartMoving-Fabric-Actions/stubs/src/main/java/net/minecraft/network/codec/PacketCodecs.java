package net.minecraft.network.codec;

public final class PacketCodecs {
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, Byte> BYTE = new PacketCodec<>();
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, Float> FLOAT = new PacketCodec<>();
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, Boolean> BOOLEAN = new PacketCodec<>();
}
