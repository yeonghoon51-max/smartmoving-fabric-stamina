package com.smartmoving.mixin;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements SmartMovingPlayer {
    @Unique
    private final SmartMovingState smartmoving$state = new SmartMovingState();

    @Override
    public SmartMovingState smartmoving$getState() {
        return smartmoving$state;
    }

    /**
     * 기어가기/슬라이딩 중에는 바닐라의 "기어가기" 자세(SWIMMING 포즈, 높이 0.6)를 강제한다.
     * 이 자세는 1칸 높이 틈을 지나갈 수 있고, 다른 플레이어에게도 엎드린 모습으로 보인다.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void smartmoving$forceLowPose(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!smartmoving$state.wantsLowPose()) return;
        if (self.getAbilities().flying || self.isGliding() || self.isSleeping()
                || self.hasVehicle() || self.isSpectator() || self.isSwimming()) {
            return;
        }
        self.setPose(EntityPose.SWIMMING);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartmoving$tick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        boolean climbing = smartmoving$state.climbing;
        if (climbing) {
            if (self.getWorld() instanceof ServerWorld world) {
                // 원작 handleCrash(): 떨어지다가 벽을 잡으면 (2칸 넘게 떨어졌을 때) 대미지
                SmartMovingConfig cfg = SmartMoving.CONFIG;
                if (!smartmoving$state.serverWasClimbing && self.fallDistance >= cfg.freeClimbFallDamageStartDistance) {
                    float damage = (float) Math.ceil((self.fallDistance - cfg.freeClimbFallDamageStartDistance) * cfg.freeClimbFallDamageFactor);
                    if (damage > 0) self.damage(world, self.getDamageSources().fall(), damage);
                }
                self.addExhaustion(cfg.climbHungerExhaustion);
            }
            // 벽을 타는 동안에는 낙하 거리가 쌓이지 않는다 (서버의 낙하 대미지 판정용).
            self.onLanding();
        }
        smartmoving$state.serverWasClimbing = climbing;
    }
}
