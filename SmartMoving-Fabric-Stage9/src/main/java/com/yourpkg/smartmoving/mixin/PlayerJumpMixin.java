package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerJumpMixin {
    @Inject(method = "jump()V", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerContext.Holder h)) return;
        var ctx = h.smartmoving$getContext();

        float charge = ctx.jumpCharge; // 0..100
        if (charge >= SmartMovingMod.CONFIG.jumpMinToTrigger) {
            double boost = (charge / 100.0) * SmartMovingMod.CONFIG.jumpBoostMax;
            var v = self.getVelocity();
            self.setVelocity(v.x, v.y + boost, v.z);
            ctx.jumpCharge = 0f; // consume
        }
    }
}
