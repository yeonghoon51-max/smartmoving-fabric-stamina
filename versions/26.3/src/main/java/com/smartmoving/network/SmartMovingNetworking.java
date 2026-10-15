package com.smartmoving.network;

import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Pose;
import net.minecraft.server.level.ServerPlayer;

public final class SmartMovingNetworking {
    private SmartMovingNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(StateC2SPayload.ID, StateC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StateSyncS2CPayload.ID, StateSyncS2CPayload.CODEC);

        // 클라이언트가 보낸 상태를 저장하고, 이 플레이어를 보고 있는 다른 플레이어들에게 전달한다.
        ServerPlayNetworking.registerGlobalReceiver(StateC2SPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            SmartMovingState state = SmartMovingPlayer.of(player);
            int old = state.flags();
            boolean wasHeadJumping = state.headJumping;
            boolean wasLow = state.wantsLowPose();
            state.applyFlags(payload.flags());
            if (!wasLow && state.wantsLowPose() && !player.isSpectator() && !player.isPassenger()
                    && !player.getAbilities().flying && !player.isFallFlying() && !player.isSleeping()) {
                // 납작해진 몸으로 바로 다음 이동(틈으로 들어가기)을 하므로, 서버의 충돌 상자도 지금 바꿔 둔다.
                player.setPose(Pose.SWIMMING);
            }
            if (wasHeadJumping && !state.headJumping) {
                state.headJumpEndAge = player.tickCount;
            }
            if (old != state.flags()) {
                broadcast(player, state.flags());
            }
        });

        // 새로 시야에 들어온 플레이어에게 현재 상태를 알려준다.
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayer target) {
                int flags = SmartMovingPlayer.of(target).flags();
                if (flags != 0 && ServerPlayNetworking.canSend(viewer, StateSyncS2CPayload.ID)) {
                    ServerPlayNetworking.send(viewer, new StateSyncS2CPayload(target.getId(), flags));
                }
            }
        });
    }

    private static void broadcast(ServerPlayer player, int flags) {
        StateSyncS2CPayload packet = new StateSyncS2CPayload(player.getId(), flags);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            if (viewer != player && ServerPlayNetworking.canSend(viewer, StateSyncS2CPayload.ID)) {
                ServerPlayNetworking.send(viewer, packet);
            }
        }
    }
}
