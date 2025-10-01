package com.yourpkg.smartmoving.network;

import com.yourpkg.smartmoving.state.PlayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerNetworking {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(Channels.ACTION, (server, player, handler, buf, responseSender) -> {
            byte action = buf.readByte();
            server.execute(() -> handleAction(player, action));
        });
    }

    public static void sync(ServerPlayerEntity player, PlayerContext ctx) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeFloat(ctx.jumpCharge);
        buf.writeFloat(ctx.grabEnergy);
        buf.writeBoolean(ctx.grabbing);
        buf.writeBoolean(ctx.crawling);
        ServerPlayNetworking.send(player, Channels.SYNC, buf);
    }

    // 1=grab down, 2=grab up
    private static void handleAction(ServerPlayerEntity player, byte action) {
        if (!(player instanceof PlayerContext.Holder holder)) return;
        PlayerContext ctx = holder.smartmoving$getContext();
        if (action == 1) ctx.grabbing = true;
        else if (action == 2) ctx.grabbing = false;
        sync(player, ctx);
    }
}
