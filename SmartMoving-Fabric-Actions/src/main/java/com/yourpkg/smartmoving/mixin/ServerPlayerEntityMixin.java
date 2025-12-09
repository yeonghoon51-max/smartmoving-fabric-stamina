package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.network.ServerNetworking;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerContext.Holder {
    @Unique private PlayerContext ctx = new PlayerContext();
    @Unique private float lastJC = -1f, lastGE = -1f;
    @Override public PlayerContext smartmoving$getContext() { return ctx; }

    @Inject(method="tick()V", at=@At("TAIL"))
    private void smartmoving$tick(CallbackInfo ci){
        ServerPlayerEntity self=(ServerPlayerEntity)(Object)this;

        if (self.isOnGround() && self.isSneaking()) {
            ctx.jumpCharge = Math.min(100f, ctx.jumpCharge + SmartMovingMod.CONFIG.jumpChargePerTick);
        } else if (!self.isOnGround() && ctx.jumpCharge > 0f) {
            ctx.jumpCharge = Math.max(0f, ctx.jumpCharge - 0.15f);
        }

        if (ctx.grabbing) ctx.grabEnergy = Math.max(0f, ctx.grabEnergy - SmartMovingMod.CONFIG.grabDrainPerTick);
        else              ctx.grabEnergy = Math.min(100f, ctx.grabEnergy + SmartMovingMod.CONFIG.grabRegenPerTick);

        if (ctx.jumpCharge!=lastJC || ctx.grabEnergy!=lastGE){
            ServerNetworking.sync(self, ctx);
            lastJC=ctx.jumpCharge; lastGE=ctx.grabEnergy;
        }
    }
}
