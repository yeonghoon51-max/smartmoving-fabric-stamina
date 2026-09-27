package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Stamina;
import com.smartmoving.logic.WallProbe;
import com.smartmoving.network.StateC2SPayload;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * 내 캐릭터의 스마트 무빙 동작을 결정하는 곳.
 * 매 틱 ClientPlayerEntity.tickMovement() 시작 부분에서 호출된다 (바닐라 이동 계산 직전).
 */
public final class ClientMovementController {
    private ClientMovementController() {
    }

    public static void tick(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        SmartMovingState state = SmartMovingPlayer.of(player);

        if (state.stamina < 0f) state.stamina = cfg.maxStamina;

        // 서버에 이 모드가 없으면 자세/낙하 판정이 어긋나므로 모든 기능을 끈다.
        if (!ClientPlayNetworking.canSend(StateC2SPayload.ID)) {
            resetAll(state);
            return;
        }

        boolean sneakKey = options.sneakKey.isPressed();
        boolean jumpKey = options.jumpKey.isPressed();
        boolean forwardKey = options.forwardKey.isPressed();
        boolean backKey = options.backKey.isPressed();
        boolean grabKey = SmartMovingKeys.GRAB.isPressed();
        boolean jumpPressed = jumpKey && !state.prevJumpKey;
        boolean sneakPressed = sneakKey && !state.prevSneakKey;
        state.prevJumpKey = jumpKey;
        state.prevSneakKey = sneakKey;

        boolean onGround = player.isOnGround();
        // 바닐라 특수 상태에서는 스마트 무빙 동작을 하지 않는다.
        boolean special = player.getAbilities().flying || player.hasVehicle() || player.isSpectator()
                || player.isTouchingWater() || player.isGliding() || player.isSleeping() || player.isInLava();

        if (state.grabCooldown > 0) state.grabCooldown--;

        // ---------- 기어가기 ----------
        while (SmartMovingKeys.CRAWL.wasPressed()) {
            state.crawlToggled = !state.crawlToggled;
        }
        if (!cfg.enableCrawling || special) state.crawlToggled = false;
        if (state.crawlFromSlide && !sneakKey) state.crawlFromSlide = false;
        state.crawling = state.crawlToggled || state.crawlFromSlide;

        // ---------- 헤드 점프 착지 ----------
        if (state.headJumping && (onGround || special || player.isClimbing())) {
            state.headJumping = false;
        }

        // ---------- 슬라이딩 ----------
        tickSlide(player, state, cfg, sneakKey, sneakPressed, jumpKey, onGround, special);

        // ---------- 헤드 점프 (달리기 + 잡기 + 점프) ----------
        // 여기서는 "이번 점프를 헤드 점프로" 표시만 하고, 실제 발사는 바닐라 jump() 직후에 한다.
        state.headJumpArmed = cfg.enableHeadJump && grabKey && jumpKey && onGround && !special
                && !state.crawling && !player.isClimbing()
                && (player.isSprinting() || player.getVelocity().horizontalLength() >= cfg.headJumpMinSpeed)
                && !WallProbe.hasAnyWall(player, player.getHorizontalFacing())
                && Stamina.has(state, cfg.headJumpStaminaCost);

        // ---------- 벽 타기 / 벽 점프 ----------
        tickClimb(player, state, cfg, grabKey, forwardKey, backKey, jumpPressed, onGround, special);

        // ---------- 모아 뛰기 ----------
        boolean standingStill = player.getVelocity().horizontalLengthSquared() < 0.001
                && !forwardKey && !backKey && !options.leftKey.isPressed() && !options.rightKey.isPressed();
        if (cfg.enableChargedJump && onGround && sneakKey && standingStill && !state.wantsLowPose()
                && !state.climbing && !special && Stamina.has(state, 1f)) {
            state.jumpCharge = Math.min(1f, state.jumpCharge + 1f / Math.max(1, cfg.chargedJumpTicks));
        } else if (!(onGround && sneakKey)) {
            state.jumpCharge = 0f;
        }

        // ---------- 빠른 사다리 ----------
        if (cfg.enableFasterLadders && player.isClimbing() && forwardKey && !sneakKey && player.horizontalCollision) {
            Vec3d v = player.getVelocity();
            if (v.y < cfg.ladderUpSpeed) player.setVelocity(v.x, cfg.ladderUpSpeed, v.z);
        }

        // ---------- 스태미나 ----------
        tickStamina(player, state, cfg, onGround, forwardKey, backKey);

        state.prevOnGround = onGround;
        syncToServer(state);
    }

    private static void tickSlide(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                  boolean sneakKey, boolean sneakPressed, boolean jumpKey,
                                  boolean onGround, boolean special) {
        if (!state.sliding) {
            if (!cfg.enableSliding || special || !onGround || state.crawlToggled) return;
            double speed = player.getVelocity().horizontalLength();

            // 공중에서 웅크리기를 누른 채 착지: 속도를 그대로 살려 슬라이딩 (연계 무빙의 핵심)
            boolean landing = !state.prevOnGround && sneakKey && speed >= cfg.landingSlideMinSpeed
                    && Stamina.has(state, cfg.landingSlideStaminaCost);
            // 달리다가 웅크리기: 앞으로 부스트를 받으며 슬라이딩
            boolean fromSprint = player.isSprinting() && sneakPressed && !state.crawling && speed > 0.1
                    && Stamina.has(state, cfg.slideStaminaCost);

            if (landing) {
                Stamina.use(state, cfg.landingSlideStaminaCost);
            } else if (fromSprint) {
                if (speed < cfg.slideMaxBoostedSpeed) {
                    Vec3d look = Vec3d.fromPolar(0f, player.getYaw());
                    player.addVelocity(look.x * cfg.slideBoost, 0, look.z * cfg.slideBoost);
                }
                Stamina.use(state, cfg.slideStaminaCost);
            } else {
                return;
            }
            state.sliding = true;
            state.slideTicks = 0;
            state.crawlFromSlide = false;
            return;
        }

        state.slideTicks++;
        boolean stop = special || !sneakKey || jumpKey
                || state.slideTicks >= cfg.slideMaxTicks
                || (state.slideTicks > 3 && player.getVelocity().horizontalLength() < 0.06);
        if (stop) {
            state.sliding = false;
            player.setSprinting(false);
            // 웅크리기를 계속 누르고 있으면 그대로 기어가기로 이어진다 (원작 동작).
            // 점프로 끝낸 경우는 슬라이드 점프/헤드 점프로 이어지므로 제외.
            if (sneakKey && !jumpKey && !special && cfg.enableCrawling) {
                state.crawlFromSlide = true;
                state.crawling = true;
            }
        }
    }

    private static void tickClimb(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                  boolean grabKey, boolean forwardKey, boolean backKey, boolean jumpPressed,
                                  boolean onGround, boolean special) {
        Direction facing = player.getHorizontalFacing();
        boolean wall = WallProbe.hasAnyWall(player, facing);
        boolean wasClimbing = state.climbing;

        boolean canClimb = cfg.enableClimbing && grabKey && wall && !special && !player.isClimbing()
                && !state.wantsLowPose() && state.grabCooldown == 0 && Stamina.has(state, 0.01f);
        // 땅에 서 있을 때는 앞으로 가려고 할 때만 벽에 달라붙는다.
        state.climbing = canClimb && (!onGround || forwardKey || wasClimbing);

        if (!state.climbing) {
            // 벽을 오르다가 꼭대기를 넘었다: 턱 위로 올라서도록 살짝 밀어 올린다.
            if (wasClimbing && forwardKey && !wall) {
                ledgeHop(player, facing, cfg);
            }
            return;
        }

        if (forwardKey && WallProbe.isAtLedge(player, facing)) {
            state.climbing = false;
            state.grabCooldown = 6;
            ledgeHop(player, facing, cfg);
            return;
        }

        if (jumpPressed) {
            if (backKey && cfg.enableWallJump && Stamina.has(state, cfg.wallJumpStaminaCost)) {
                // 벽 점프: 벽을 박차고 뒤쪽 위로 뛴다.
                Stamina.use(state, cfg.wallJumpStaminaCost);
                player.setVelocity(-facing.getOffsetX() * cfg.wallJumpPush, cfg.wallJumpUp,
                        -facing.getOffsetZ() * cfg.wallJumpPush);
                state.climbing = false;
                state.grabCooldown = 10;
            } else if (!backKey && Stamina.has(state, cfg.climbJumpStaminaCost)) {
                // 벽을 짚고 위로 도약
                Stamina.use(state, cfg.climbJumpStaminaCost);
                player.setVelocity(0, cfg.climbJumpUp, 0);
                state.climbing = false;
                state.grabCooldown = 5;
            }
        }
    }

    private static void ledgeHop(ClientPlayerEntity player, Direction facing, SmartMovingConfig cfg) {
        player.setVelocity(facing.getOffsetX() * 0.12, cfg.ledgeClimbBoost, facing.getOffsetZ() * 0.12);
    }

    private static void tickStamina(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                    boolean onGround, boolean forwardKey, boolean backKey) {
        if (!cfg.enableStamina) {
            state.stamina = cfg.maxStamina;
            state.exhausted = false;
            return;
        }
        if (state.climbing) {
            boolean moving = forwardKey || backKey
                    || MinecraftClient.getInstance().options.leftKey.isPressed()
                    || MinecraftClient.getInstance().options.rightKey.isPressed();
            Stamina.use(state, moving ? cfg.climbMoveStaminaPerTick : cfg.climbHoldStaminaPerTick);
            if (state.exhausted) {
                state.climbing = false; // 힘이 빠져서 떨어진다
            }
        } else if (player.isSprinting() && !player.getAbilities().allowFlying) {
            Stamina.use(state, cfg.sprintStaminaPerTick);
        } else if (!state.sliding && !state.headJumping && state.jumpCharge <= 0f) {
            Stamina.regen(state, onGround ? cfg.staminaRegenPerTick : cfg.staminaRegenAirPerTick);
        }

        if (state.exhausted && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private static void syncToServer(SmartMovingState state) {
        byte flags = state.flags();
        // 값이 바뀌었을 때 + 켜져 있는 동안 1초마다 한 번씩 (리스폰/차원 이동 대비)
        boolean periodic = flags != 0 && ++state.resendTimer >= 20;
        if (flags != state.lastSentFlags || periodic) {
            state.resendTimer = 0;
            state.lastSentFlags = flags;
            ClientPlayNetworking.send(new StateC2SPayload(flags));
        }
    }

    private static void resetAll(SmartMovingState state) {
        state.crawling = false;
        state.climbing = false;
        state.sliding = false;
        state.headJumping = false;
        state.headJumpArmed = false;
        state.crawlToggled = false;
        state.crawlFromSlide = false;
        state.jumpCharge = 0f;
    }
}
