package com.yourpkg.smartmoving.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 서버 → 클라이언트: 플레이어 상태 동기화
 */
public record SyncPayload(
        float   jumpCharge,
        float   grabEnergy,
        boolean grabbing,
        boolean crawling
) implements CustomPayload {

    public static final Id<SyncPayload> ID =
            new Id<>(Identifier.of("smartmoving", "sync"));

    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT,   SyncPayload::jumpCharge,
                    PacketCodecs.FLOAT,   SyncPayload::grabEnergy,
                    PacketCodecs.BOOLEAN, SyncPayload::grabbing,
                    PacketCodecs.BOOLEAN, SyncPayload::crawling,
                    SyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
