package com.yourpkg.smartmoving.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 클라이언트 → 서버: 잡기 키 상태 전송
 * actionId: 1 = 누름, 2 = 떼기
 */
public record ActionPayload(byte actionId) implements CustomPayload {
    public static final Id<ActionPayload> ID =
            new Id<>(Identifier.of("smartmoving", "action"));

    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BYTE, ActionPayload::actionId, ActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
