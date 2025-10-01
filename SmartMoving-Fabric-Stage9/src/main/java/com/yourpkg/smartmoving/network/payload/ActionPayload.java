package com.yourpkg.smartmoving.network.payload;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ActionPayload(byte actionId) implements CustomPayload {

    public static final Id<ActionPayload> ID = new Id<>(Identifier.of("smartmoving", "action"));

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, ActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BYTE, ActionPayload::actionId,
                    ActionPayload::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
