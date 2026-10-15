package com.smartmoving.network;

import com.smartmoving.SmartMoving;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 1.20.1 은 채널(Identifier) + PacketByteBuf 방식이다.
 * STATE: 클라이언트 → 서버 "지금 내 상태는 이렇다" (varint flags)
 * STATE_SYNC: 서버 → 다른 클라이언트 "entityId 플레이어의 상태가 이렇다" (varint entityId, varint flags)
 */
public final class SmartMovingNetworking {
    public static final Identifier STATE = SmartMoving.id("state");
    public static final Identifier STATE_SYNC = SmartMoving.id("state_sync");

    private SmartMovingNetworking() {
    }

    public static void registerCommon() {
        // 클라이언트가 보낸 상태를 저장하고, 이 플레이어를 보고 있는 다른 플레이어들에게 전달한다.
        ServerPlayNetworking.registerGlobalReceiver(STATE, (server, player, handler, buf, responseSender) -> {
            int flags = buf.readVarInt();
            server.execute(() -> receiveState(player, flags));
        });

        // 새로 시야에 들어온 플레이어에게 현재 상태를 알려준다.
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayerEntity target) {
                int flags = SmartMovingPlayer.of(target).flags();
                if (flags != 0 && ServerPlayNetworking.canSend(viewer, STATE_SYNC)) {
                    ServerPlayNetworking.send(viewer, STATE_SYNC, syncBuf(target.getId(), flags));
                }
            }
        });
    }

    private static void receiveState(ServerPlayerEntity player, int flags) {
        SmartMovingState state = SmartMovingPlayer.of(player);
        int old = state.flags();
        boolean wasHeadJumping = state.headJumping;
        boolean wasLow = state.wantsLowPose();
        state.applyFlags(flags);
        if (!wasLow && state.wantsLowPose() && !player.isSpectator() && !player.hasVehicle()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isSleeping()) {
            // 납작해진 몸으로 바로 다음 이동(틈으로 들어가기)을 하므로, 서버의 충돌 상자도 지금 바꿔 둔다.
            player.setPose(EntityPose.SWIMMING);
        }
        if (wasHeadJumping && !state.headJumping) {
            state.headJumpEndAge = player.age;
        }
        if (old != state.flags()) {
            broadcast(player, state.flags());
        }
    }

    public static PacketByteBuf stateBuf(int flags) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(flags);
        return buf;
    }

    private static PacketByteBuf syncBuf(int entityId, int flags) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeVarInt(flags);
        return buf;
    }

    private static void broadcast(ServerPlayerEntity player, int flags) {
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(player)) {
            if (viewer != player && ServerPlayNetworking.canSend(viewer, STATE_SYNC)) {
                ServerPlayNetworking.send(viewer, STATE_SYNC, syncBuf(player.getId(), flags));
            }
        }
    }
}
