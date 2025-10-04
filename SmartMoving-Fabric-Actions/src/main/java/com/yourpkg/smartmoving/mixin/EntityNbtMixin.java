package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityNbtMixin {
    @Inject(method="writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at=@At("HEAD"))
    private void smartmoving$save(NbtCompound n, CallbackInfo ci){
        if (!((Object)this instanceof ServerPlayerEntity sp)) return;
        PlayerContext ctx = ((PlayerContext.Holder)sp).smartmoving$getContext();
        n.put("SmartMoving", ctx.toNbt());
    }
    @Inject(method="readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V", at=@At("HEAD"))
    private void smartmoving$load(NbtCompound n, CallbackInfo ci){
        if (!((Object)this instanceof ServerPlayerEntity sp)) return;
        var el = n.get("SmartMoving");
        if (el instanceof NbtCompound sm) {
            PlayerContext ctx = ((PlayerContext.Holder)sp).smartmoving$getContext();
            ctx.fromNbt(sm);
        }
    }
}
