package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.logic.Exhaustion;
import com.smartmoving.logic.JumpType;
import com.smartmoving.logic.Jumps;
import com.smartmoving.logic.MoveSpeed;
import com.smartmoving.logic.WallProbe;
import com.smartmoving.network.StateC2SPayload;
import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.Vec3d;

/**
 * 내 캐릭터의 스마트 무빙 동작을 결정하는 곳.
 * 매 틱 ClientPlayerEntity.tickMovement() 시작 부분에서 호출된다 (바닐라 이동 계산 직전).
 *
 * <p>원작 SmartMovingSelf.updateEntityActionState() / handleJumping() / handleExhaustion() 의 포트.
 */
public final class ClientMovementController {
    /** 슬라이딩 중 이만큼 떨어지면 헤드 점프(활공)로 바뀐다 (원작 SlideToHeadJumpingFallDistance) */
    private static final double SLIDE_TO_HEAD_JUMP_FALL_DISTANCE = 0.05;

    private ClientMovementController() {
    }

    /** 이번 틱의 키 입력 */
    private record Input(boolean forward, boolean back, boolean left, boolean right, boolean jump,
                         boolean sneak, boolean sprint, boolean grab,
                         boolean sneakStart, boolean grabStart, boolean jumpStart) {
        boolean moving() {
            return forward || back || left || right;
        }
    }

    public static void tick(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        SmartMovingState state = SmartMovingPlayer.of(player);

        // 서버에 이 모드가 없으면 자세/낙하 판정이 어긋나므로 모든 기능을 끈다.
        if (!ClientPlayNetworking.canSend(StateC2SPayload.ID)) {
            resetAll(state);
            return;
        }

        boolean sneak = options.sneakKey.isPressed();
        boolean sprint = options.sprintKey.isPressed();
        boolean grab = SmartMovingKeys.GRAB.isPressed() || (cfg.grabUsesSprintKey && sprint);
        boolean jump = options.jumpKey.isPressed();
        Input in = new Input(options.forwardKey.isPressed(), options.backKey.isPressed(),
                options.leftKey.isPressed(), options.rightKey.isPressed(), jump, sneak, sprint, grab,
                sneak && !state.prevSneakKey, grab && !state.prevGrabKey, jump && !state.prevJumpKey);
        state.prevSneakKey = sneak;
        state.prevGrabKey = grab;
        state.prevJumpKey = jump;

        state.maxExhaustionForAction = Float.MAX_VALUE;
        state.maxExhaustionToStartAction = Float.MAX_VALUE;

        boolean onGround = player.isOnGround();
        // 바닐라 특수 상태에서는 스마트 무빙 동작을 하지 않는다.
        boolean special = player.getAbilities().flying || player.hasVehicle() || player.isSpectator()
                || player.isTouchingWater() || player.isGliding() || player.isSleeping() || player.isInLava();
        state.handlesJumps = !special;
        if (special) {
            state.sliding = false;
            state.headJumping = false;
            state.aerodynamic = false;
        }

        Vec3d velocity = player.getVelocity();
        double horizontalSpeedSquare = velocity.x * velocity.x + velocity.z * velocity.z;
        boolean standing = horizontalSpeedSquare < 0.0005;

        if (state.grabCooldown > 0) state.grabCooldown--;

        tickSprint(player, state, cfg, in, onGround, special);
        tickHeadJumpAndSlide(player, state, cfg, in, onGround, horizontalSpeedSquare);
        tickCrawl(player, state, cfg, in, onGround, special);
        tickClimb(player, state, cfg, in, onGround, special);
        if (state.handlesJumps) {
            handleJumping(player, state, cfg, in, onGround, standing);
        }

        // ---------- 빠른 사다리 ----------
        if (cfg.enableFasterLadders && player.isClimbing() && in.forward() && !in.sneak() && player.horizontalCollision) {
            Vec3d v = player.getVelocity();
            if (v.y < cfg.ladderUpSpeed) player.setVelocity(v.x, cfg.ladderUpSpeed, v.z);
        }

        tickExhaustion(player, state, cfg, in, onGround, standing);

        state.prevOnGround = onGround;
        syncToServer(state);
    }

    /** 원작: 달리기 키를 누르고 달리면 isFast(1.5배, 지침 증가), 키 없이 달리면 isRunning(바닐라) */
    private static void tickSprint(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                   Input in, boolean onGround, boolean special) {
        state.wasFast = state.fast;
        state.wasRunning = state.running;

        if (!onGround && state.wasFast && !state.climbing) state.sprintJump = true;
        if (onGround || special) state.sprintJump = false;

        boolean wantSprint = cfg.enableSprint && in.sprint() && in.forward() && !state.sliding && !in.sneak();
        boolean allowed = true;
        if (wantSprint && Exhaustion.enabled()) {
            allowed = state.exhaustion <= cfg.sprintExhaustionStop
                    && (state.wasFast || state.sprintJump || state.exhaustion <= cfg.sprintExhaustionStart);
            if (!state.sprintJump) {
                state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, cfg.sprintExhaustionStop);
                state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, cfg.sprintExhaustionStart);
            }
        }

        // 지쳐서 못 달리면 바닐라 달리기도 막는다 (ClientPlayerEntityMixin.canSprint)
        state.sprintBlocked = wantSprint && !allowed;
        if (state.sprintBlocked && player.isSprinting()) {
            player.setSprinting(false);
        }

        state.fast = wantSprint && allowed && onGround && player.isSprinting() && !state.climbing && !special;
        state.running = player.isSprinting() && !state.fast && onGround;

        // 원작 isClimbSprinting: 벽 타기 중에도 달리기 키를 누르면 빨라진다 (실제로 움직이고 있을 때만)
        double dx = player.getX() - state.lastTickX;
        double dy = player.getY() - state.lastTickY;
        double dz = player.getZ() - state.lastTickZ;
        double tickDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        state.lastTickX = player.getX();
        state.lastTickY = player.getY();
        state.lastTickZ = player.getZ();

        boolean wantClimbSprint = cfg.enableSprint && in.sprint() && !in.sneak() && state.climbing && !special;
        boolean climbSprintAllowed = true;
        if (wantClimbSprint && Exhaustion.enabled()) {
            climbSprintAllowed = state.exhaustion <= cfg.sprintExhaustionStop
                    && (state.climbFast || state.exhaustion <= cfg.sprintExhaustionStart);
            state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, cfg.sprintExhaustionStop);
            state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, cfg.sprintExhaustionStart);
        }
        double minTickDistance = state.wantClimbUp ? 0.07 * cfg.freeClimbingUpSpeedFactor
                : state.wantClimbDown ? 0.11 * cfg.freeClimbingDownSpeedFactor
                : 0.07;
        state.climbFast = wantClimbSprint && climbSprintAllowed && tickDistance >= minTickDistance;
    }

    /** 원작 2403~2450행: 헤드 점프 착지, 슬라이딩 시작/끝, 슬라이딩 → 활공 */
    private static void tickHeadJumpAndSlide(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                             Input in, boolean onGround, double horizontalSpeedSquare) {
        boolean wasHeadJumping = state.headJumping;
        state.headJumping = state.headJumping && !onGround;
        if (!state.headJumping) state.aerodynamic = false;

        // 헤드 점프 착지: 잡기+웅크리기를 누르고 있거나 일어설 공간이 없으면 그 속도 그대로 슬라이딩
        if (wasHeadJumping && !state.headJumping && onGround) {
            boolean keepLow = (in.sneak() && in.grab()) || !WallProbe.canStand(player);
            if (keepLow && cfg.enableSliding) {
                state.sliding = true;
            }
        }

        // 슬라이딩하다 떨어지면 공기 저항이 거의 없는 활공으로 바뀐다. 이것이 "여우 무빙"의 핵심.
        if (state.sliding && player.fallDistance > SLIDE_TO_HEAD_JUMP_FALL_DISTANCE) {
            state.sliding = false;
            state.headJumping = true;
            state.aerodynamic = true;
        }

        // 슬라이딩 시작: 달리면서 잡기 + 웅크리기
        boolean sprintingNow = state.fast || state.wasFast;
        boolean runningNow = state.running || state.wasRunning;
        if (cfg.enableSliding && in.grab() && onGround && (sprintingNow || runningNow)
                && !state.crawling && in.sneakStart() && !player.isTouchingWater()) {
            // 원작처럼 지침 때문에 부스트가 실패해도 슬라이딩 자체는 시작한다.
            Jumps.tryJump(player, state, JumpType.SLIDE_DOWN,
                    sprintingNow ? MoveSpeed.SPRINTING : MoveSpeed.RUNNING, null);
            state.sliding = true;
            state.headJumping = false;
            state.aerodynamic = false;
            player.setSprinting(false);
        }

        // 웅크리기를 떼거나 느려지면 슬라이딩 끝 → 기어가기
        if (state.sliding && (!in.sneak() || horizontalSpeedSquare < cfg.slideSpeedStopFactor * 0.01)) {
            state.sliding = false;
            if (in.sneak() && cfg.enableCrawling) {
                state.crawling = true;
            }
        }

        if (state.sliding && player.fallDistance > cfg.fallingDistanceMinimum) {
            state.sliding = false;
        }
    }

    /** 원작: 웅크린 채 잡기를 누르면 기어가기, 웅크리기를 떼면 일어선다 (공간이 없으면 계속) */
    private static void tickCrawl(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                  Input in, boolean onGround, boolean special) {
        while (SmartMovingKeys.CRAWL.wasPressed()) {
            state.crawlToggled = !state.crawlToggled;
        }

        boolean canCrawl = cfg.enableCrawling && !special && !state.climbing
                && player.fallDistance < cfg.fallingDistanceMinimum;
        if (!canCrawl) state.crawlToggled = false;

        boolean start = in.grabStart() && in.sneak() && onGround;
        boolean keep = state.crawling && (in.sneak() || state.crawlToggled || !WallProbe.canStand(player));
        state.crawling = canCrawl && !state.sliding && !state.headJumping && (state.crawlToggled || start || keep);
    }

    /**
     * 원작 updateEntityActionState() 의 wantClimb / wantClimbUp / wantClimbDown / isClimbHolding.
     * 실제로 벽을 타는지는 이동 직후 logic.climb.FreeClimbing 이 손/발로 잡을 곳을 찾아 정한다.
     */
    private static void tickClimb(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                  Input in, boolean onGround, boolean special) {
        boolean forward = in.forward() && !in.back();
        boolean wouldWantClimb = (in.grab() || (state.climbHolding && in.sneak()))
                && (!state.sliding || in.grab() && forward)
                && !state.headJumping;
        boolean wantClimb = cfg.enableClimbing && !special && wouldWantClimb;
        boolean wantCrawl = cfg.enableCrawling && (state.crawling || (in.grabStart() && in.sneak() && onGround));

        state.wantClimbUp = wantClimb && forward;
        state.wantClimbDown = wantClimb && !forward && !wantCrawl;

        boolean wantClimbHolding = (state.climbHolding && in.sneak())
                || (wantClimb && !state.crawling && (in.sneak() || state.crawlToggled));
        state.climbHolding = wantClimbHolding && state.climbing;

        // 이동 직후 벽 타기 판정에서 쓴다 (꼭대기에서 점프, 뒤로 점프)
        state.jumpStartKey = in.jumpStart();
        state.grabKey = in.grab();
    }

    /** 원작 handleJumping(): 일반 점프, 모아 뛰기, 헤드 점프 */
    private static void handleJumping(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                      Input in, boolean onGround, boolean standing) {
        if (state.climbing) return;

        boolean jump = onGround && in.jump();
        // 원작 wouldIsSneaking: 달리기 키나 잡기와 함께 누르면 웅크리기가 아니다
        boolean sneaking = in.sneak() && !in.sprint() && !(cfg.enableCrawling && in.grab())
                && !state.sliding && !state.headJumping && !state.crawling;
        MoveSpeed speed = state.fast ? MoveSpeed.SPRINTING
                : state.running ? MoveSpeed.RUNNING
                : sneaking && onGround ? MoveSpeed.SNEAKING
                : standing ? MoveSpeed.STANDING
                : MoveSpeed.WALKING;

        // ---- 모아 뛰기: 웅크린 채 점프 키를 누르고 있다가 떼면 뛴다 ----
        boolean jumpCharging = false;
        if (cfg.enableChargedJump) {
            boolean possible = onGround && standing && !state.crawling && !state.sliding;
            jumpCharging = possible && sneaking;
            if (possible) {
                if (in.jump() && sneaking) {
                    state.jumpCharge++;
                } else {
                    if (state.jumpCharge > 0) {
                        Jumps.tryJump(player, state, JumpType.CHARGE_UP, MoveSpeed.STANDING, null);
                    }
                    state.jumpCharge = 0;
                }
            } else {
                if (state.jumpCharge > 0) state.blockJumpTillButtonRelease = true;
                state.jumpCharge = 0;
            }
        }

        // ---- 헤드 점프: 달리면서 잡기 + 점프 키를 누르고 있다가 떼면 뛴다 ----
        boolean headJumpCharging = false;
        if (cfg.enableHeadJump) {
            headJumpCharging = in.grab() && (state.fast || state.sprintJump || (state.running && onGround))
                    && !state.crawling;
            if (headJumpCharging) {
                if (in.jump()) {
                    state.headJumpCharge++;
                } else {
                    if (state.headJumpCharge > 0 && onGround) {
                        Jumps.tryJump(player, state, JumpType.HEAD_UP, speed, null);
                    }
                    state.headJumpCharge = 0;
                }
            } else {
                if (state.headJumpCharge > 0) state.blockJumpTillButtonRelease = true;
                state.headJumpCharge = 0;
            }
        }

        if (!in.jump()) state.blockJumpTillButtonRelease = false;

        if (jump && !state.blockJumpTillButtonRelease && !jumpCharging && !headJumpCharging) {
            Jumps.tryJump(player, state, JumpType.UP, speed, null);
        }
    }

    /** 원작 handleExhaustion(): 행동하면 오르고, 배고픔이 충분하면 쉬는 동안 내려간다 */
    private static void tickExhaustion(ClientPlayerEntity player, SmartMovingState state, SmartMovingConfig cfg,
                                       Input in, boolean onGround, boolean standing) {
        if (!Exhaustion.enabled()) {
            state.exhaustion = 0f;
            return;
        }

        Vec3d v = player.getVelocity();
        boolean verticalStill = Math.abs(v.y) < 0.007;
        boolean still = standing && verticalStill && !in.moving();

        float additional = 0f;
        if (state.climbing && !still && cfg.climbExhaustion) {
            if (in.forward()) additional = cfg.climbUpExhaustionGain;
            else if (in.back()) additional = cfg.climbDownExhaustionGain;
            else additional = cfg.climbStrafeExhaustionGain;
        }
        if (state.fast || state.climbFast) {
            if (additional == 0f) additional = 1f;
            additional *= cfg.sprintExhaustionGain;
        }
        state.exhaustion += additional;

        if (state.exhaustion > 0f && player.getHungerManager().getFoodLevel() > cfg.exhaustionLossFoodMinimum) {
            boolean sneaking = in.sneak() && !state.crawling && !state.sliding;
            state.exhaustion -= Exhaustion.lossFactor(onGround, standing, still, sneaking,
                    state.running, state.fast, state.climbing);
        }
        state.exhaustion = Math.max(0f, state.exhaustion);
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
        state.aerodynamic = false;
        state.crawlToggled = false;
        state.handlesJumps = false;
        state.sprintBlocked = false;
        state.fast = false;
        state.climbFast = false;
        state.climbHolding = false;
        state.wantClimbUp = false;
        state.wantClimbDown = false;
        state.jumpCharge = 0;
        state.headJumpCharge = 0;
    }
}
