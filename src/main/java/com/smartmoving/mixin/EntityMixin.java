package com.smartmoving.mixin;

import com.smartmoving.logic.WallJumping;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** 벽 점프: 이동하기 직전에 어느 쪽 벽에 막힐지 기록한다 (원작 beforeMoveEntity). */
    @Inject(method = "move", at = @At("HEAD"))
    private void smartmoving$beforeMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (type != MovementType.SELF) return;
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!player.getWorld().isClient || !player.isLogicalSideForUpdatingMovement()) return;
        SmartMovingState state = SmartMovingPlayer.of(player);
        if (state.wantWallJumping) {
            WallJumping.beforeMove(player, state, movement);
        }
    }
}
