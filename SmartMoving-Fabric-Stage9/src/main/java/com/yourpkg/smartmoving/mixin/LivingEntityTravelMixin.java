package com.yourpkg.smartmoving.mixin;

import com.yourpkg.smartmoving.logic.ClimbHandler;
import com.yourpkg.smartmoving.logic.SwimHandler;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d travel(Vec3d in) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof PlayerEntity p)) return in;
        if(self.getWorld().isClient()) return in;
        if(p instanceof PlayerContext.Holder h){
            var c=h.smartmoving$getContext();
            in = ClimbHandler.apply(p, c, in);
            in = SwimHandler.apply(p, c, in);
        }
        return in;
    }
}
