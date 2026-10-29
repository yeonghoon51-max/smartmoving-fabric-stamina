package com.smartmoving.mixin;

import com.smartmoving.logic.WallJumping;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** 벽 점프: 이동하기 직전에 어느 쪽 벽에 막힐지 기록한다 (원작 beforeMoveEntity). */
    @Inject(method = "move", at = @At("HEAD"))
    private void smartmoving$beforeMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (type != MoverType.SELF) return;
        if (!((Object) this instanceof Player player)) return;
        if (!player.level().isClientSide() || !player.isLocalInstanceAuthoritative()) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        if (state.wantWallJumping) {
            WallJumping.beforeMove(player, state, movement);
        }
    }
}
