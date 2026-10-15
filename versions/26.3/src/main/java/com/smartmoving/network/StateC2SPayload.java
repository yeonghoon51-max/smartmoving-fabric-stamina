package com.smartmoving.network;

import com.smartmoving.SmartMoving;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 클라이언트 → 서버: "지금 내 상태(기어가기/벽타기/수영 ...)는 이렇다" */
public record StateC2SPayload(int flags) implements CustomPacketPayload {
    public static final Type<StateC2SPayload> ID = new Type<>(SmartMoving.id("state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateC2SPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StateC2SPayload::flags, StateC2SPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
