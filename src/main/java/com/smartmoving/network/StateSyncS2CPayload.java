package com.smartmoving.network;

import com.smartmoving.SmartMoving;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** 서버 → 다른 클라이언트: "entityId 플레이어의 상태가 이렇다" (자세/애니메이션 표시용) */
public record StateSyncS2CPayload(int entityId, byte flags) implements CustomPayload {
    public static final Id<StateSyncS2CPayload> ID = new Id<>(SmartMoving.id("state_sync"));
    public static final PacketCodec<RegistryByteBuf, StateSyncS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StateSyncS2CPayload::entityId,
            PacketCodecs.BYTE, StateSyncS2CPayload::flags,
            StateSyncS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
