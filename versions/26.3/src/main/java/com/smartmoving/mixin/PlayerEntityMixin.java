package com.smartmoving.mixin;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.SmartFlying;
import com.smartmoving.logic.SmartSwimming;
import com.smartmoving.logic.climb.FreeClimbing;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
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
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void smartmoving$forceLowPose(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!smartmoving$state.wantsLowPose()) return;
        if ((self.getAbilities().flying && !smartmoving$state.flying) || self.isFallFlying() || self.isSleeping()
                || self.isPassenger() || self.isSpectator() || self.isSwimming()) {
            return;
        }
        self.setPose(Pose.SWIMMING);
        ci.cancel();
    }

    /**
     * 원작 수영/잠수와 원작 비행. 바닐라 Player.travel() 의 수영·비행 처리보다 먼저 가로챈다.
     * 자기 캐릭터를 조종하는 클라이언트에서만.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void smartmoving$swimAndFly(Vec3 movementInput, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() || !self.isLocalInstanceAuthoritative()) return;
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

    /** 지쳐 있으면 달릴 수 없다 (sprintBlocked 는 자기 캐릭터를 조종하는 클라이언트에서만 켜진다). */
    @Inject(method = "canSprint", at = @At("RETURN"), cancellable = true)
    private void smartmoving$noSprintWhenExhausted(CallbackInfoReturnable<Boolean> cir) {
        if (smartmoving$state.sprintBlocked) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 원작 wouldIsSneaking: 슬라이딩·활공 중에는 Shift 를 누르고 있어도 웅크리기로 치지 않는다.
     * 바닐라는 웅크리면 턱 끝에서 멈추게 하는데, 그러면 슬라이딩하다 턱에서 떨어져 활공으로 넘어갈 수 없다 (여우 무빙).
     */
    @Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true)
    private void smartmoving$noLedgeStopWhileSliding(CallbackInfoReturnable<Boolean> cir) {
        if (smartmoving$state.sliding || smartmoving$state.headJumping) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartmoving$tick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        boolean climbing = smartmoving$state.climbing;
        if (climbing) {
            if (self.level() instanceof ServerLevel world) {
                // 원작 handleCrash(): 떨어지다가 벽을 잡으면 (2칸 넘게 떨어졌을 때) 대미지
                SmartMovingConfig cfg = SmartMoving.CONFIG;
                if (!smartmoving$state.serverWasClimbing && self.fallDistance >= cfg.freeClimbFallDamageStartDistance) {
                    float damage = (float) Math.ceil((self.fallDistance - cfg.freeClimbFallDamageStartDistance) * cfg.freeClimbFallDamageFactor);
                    if (damage > 0) self.hurtServer(world, self.damageSources().fall(), damage);
                }
                self.causeFoodExhaustion(cfg.climbHungerExhaustion);
            }
            // 벽을 타는 동안에는 낙하 거리가 쌓이지 않는다 (서버의 낙하 대미지 판정용).
            self.resetFallDistance();
        }
        smartmoving$state.serverWasClimbing = climbing;
        if (smartmoving$state.ceilingClimbing) {
            // 천장에 매달린 동안에도 낙하 거리가 쌓이지 않는다.
            self.resetFallDistance();
        }
    }
}
