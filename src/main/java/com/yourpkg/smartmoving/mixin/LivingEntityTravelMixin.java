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

    /**
     * travel() 의 movementInput 파라미터를 수정해 클라이밍/수영 속도 보정을 적용한다.
     * 서버 전용 (클라이언트 예측 방해 방지).
     */
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d smModifyTravel(Vec3d movementInput) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity player)) return movementInput;
        if (self.getWorld().isClient()) return movementInput;
        if (!(player instanceof PlayerContext.Holder h)) return movementInput;

        PlayerContext ctx = h.smartmoving$getContext();
        movementInput = ClimbHandler.apply(player, ctx, movementInput);
        movementInput = SwimHandler.apply(player, ctx, movementInput);
        return movementInput;
    }
}
