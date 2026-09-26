package com.smartmoving.network;

import com.smartmoving.SmartMoving;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** 클라이언트 → 서버: "지금 내 상태(기어가기/벽타기/슬라이딩)는 이렇다" */
public record StateC2SPayload(byte flags) implements CustomPayload {
    public static final Id<StateC2SPayload> ID = new Id<>(SmartMoving.id("state"));
    public static final PacketCodec<RegistryByteBuf, StateC2SPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BYTE, StateC2SPayload::flags, StateC2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
