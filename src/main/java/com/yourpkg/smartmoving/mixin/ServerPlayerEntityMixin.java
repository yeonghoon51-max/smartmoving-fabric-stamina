package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.logic.ClimbHandler;
import com.yourpkg.smartmoving.network.ServerNetworking;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerContext.Holder {

    @Unique private final PlayerContext ctx = new PlayerContext();
    @Unique private float lastSyncJC = -1f;
    @Unique private float lastSyncGE = -1f;

    @Override
    public PlayerContext smartmoving$getContext() { return ctx; }

    // ── NBT 저장/로드 ──────────────────────────────────────────────────────────

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void smSave(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("SmartMoving", ctx.toNbt());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void smLoad(NbtCompound nbt, CallbackInfo ci) {
        NbtElement el = nbt.get("SmartMoving");
        if (el instanceof NbtCompound sm) ctx.fromNbt(sm);
    }

    // ── 매 틱 처리 ─────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void smTick(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;

        tickChargedJump(self);
        tickGrabEnergy(self);
        tickWallGrab(self);
        syncIfNeeded(self);
    }

    @Unique
    private void tickChargedJump(ServerPlayerEntity player) {
        // 바닥에서 쪼그려 앉으면 충전 (달리기 중 제외)
        if (player.isOnGround() && player.isSneaking() && !player.isSprinting()) {
            ctx.jumpCharge = Math.min(100f,
                    ctx.jumpCharge + SmartMovingMod.CONFIG.jumpChargePerTick);
        } else if (!player.isOnGround() && ctx.jumpCharge > 0f) {
            // 공중에 있으면 서서히 감소
            ctx.jumpCharge = Math.max(0f, ctx.jumpCharge - 0.15f);
        }
    }

    @Unique
    private void tickGrabEnergy(ServerPlayerEntity player) {
        if (ctx.grabbing) {
            ctx.grabEnergy = Math.max(0f,
                    ctx.grabEnergy - SmartMovingMod.CONFIG.grabDrainPerTick);
            // 스태미나 소진 시 잡기 해제
            if (ctx.grabEnergy == 0f) ctx.grabbing = false;
        } else {
            ctx.grabEnergy = Math.min(100f,
                    ctx.grabEnergy + SmartMovingMod.CONFIG.grabRegenPerTick);
        }
    }

    @Unique
    private void tickWallGrab(ServerPlayerEntity player) {
        // 잡기 상태 + 스태미나 있음 + 벽 접촉: 낙하 속도 제한 (벽 타기)
        if (ctx.grabbing && ctx.grabEnergy > 0f
                && ClimbHandler.isSolidWallAhead(player.getWorld(), player)) {
            var vel = player.getVelocity();
            if (vel.y < -0.1) {
                player.setVelocity(vel.x, Math.max(vel.y, -0.1), vel.z);
            }
        }
    }

    @Unique
    private void syncIfNeeded(ServerPlayerEntity player) {
        if (ctx.jumpCharge != lastSyncJC || ctx.grabEnergy != lastSyncGE) {
            ServerNetworking.sync(player, ctx);
            lastSyncJC = ctx.jumpCharge;
            lastSyncGE = ctx.grabEnergy;
        }
    }
}
