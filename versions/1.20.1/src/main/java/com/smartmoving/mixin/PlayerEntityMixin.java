package com.smartmoving.mixin;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.SmartFlying;
import com.smartmoving.logic.SmartSwimming;
import com.smartmoving.logic.climb.FreeClimbing;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
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
     * 기어가기/슬라이딩/잠수/원작 비행 중에는 바닐라의 "기어가기" 자세(SWIMMING 포즈, 높이 0.6)를 강제한다.
     * 이 자세는 1칸 높이 틈을 지나갈 수 있고, 다른 플레이어에게도 엎드린 모습으로 보인다.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void smartmoving$forceLowPose(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!smartmoving$state.wantsLowPose()) return;
        if ((self.getAbilities().flying && !smartmoving$state.flying) || self.isFallFlying() || self.isSleeping()
                || self.hasVehicle() || self.isSpectator() || self.isSwimming()) {
            return;
        }
        self.setPose(EntityPose.SWIMMING);
        ci.cancel();
    }

    /**
     * 원작 수영/잠수와 원작 비행. 바닐라 PlayerEntity.travel() 의 수영·비행 처리보다 먼저 가로챈다.
     * 자기 캐릭터를 조종하는 클라이언트에서만.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void smartmoving$swimAndFly(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!self.getWorld().isClient || !self.isLogicalSideForUpdatingMovement()) return;
        SmartMovingState state = smartmoving$state;
        if (!state.modActive) return;
        if (SmartSwimming.travel(self, state, movementInput) || SmartFlying.travel(self, state, movementInput)) {
            // 땅 위 이동(벽 타기, 슬라이딩 등)은 이번 틱에 일어나지 않는다.
            FreeClimbing.reset(state);
            state.sliding = false;
            state.headJumping = false;
            state.aerodynamic = false;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartmoving$tick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        boolean climbing = smartmoving$state.climbing;
        if (climbing) {
            if (!self.getWorld().isClient) {
                // 원작 handleCrash(): 떨어지다가 벽을 잡으면 (2칸 넘게 떨어졌을 때) 대미지
                SmartMovingConfig cfg = SmartMoving.CONFIG;
                if (!smartmoving$state.serverWasClimbing && self.fallDistance >= cfg.freeClimbFallDamageStartDistance) {
                    float damage = (float) Math.ceil((self.fallDistance - cfg.freeClimbFallDamageStartDistance) * cfg.freeClimbFallDamageFactor);
                    if (damage > 0) self.damage(self.getDamageSources().fall(), damage);
                }
                self.addExhaustion(cfg.climbHungerExhaustion);
            }
            // 벽을 타는 동안에는 낙하 거리가 쌓이지 않는다 (서버의 낙하 대미지 판정용).
            self.onLanding();
        }
        smartmoving$state.serverWasClimbing = climbing;
        if (smartmoving$state.ceilingClimbing) {
            // 천장에 매달린 동안에도 낙하 거리가 쌓이지 않는다.
            self.onLanding();
        }
    }
}
