package com.smartmoving.network;

import com.smartmoving.SmartMoving;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 서버 → 다른 클라이언트: "entityId 플레이어의 상태가 이렇다" (자세/애니메이션 표시용) */
public record StateSyncS2CPayload(int entityId, int flags) implements CustomPacketPayload {
    public static final Type<StateSyncS2CPayload> ID = new Type<>(SmartMoving.id("state_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateSyncS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StateSyncS2CPayload::entityId,
            ByteBufCodecs.VAR_INT, StateSyncS2CPayload::flags,
            StateSyncS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
