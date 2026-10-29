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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;

/**
 * 내 캐릭터의 스마트 무빙 동작을 결정하는 곳.
 * 매 틱 LocalPlayer.tickMovement() 시작 부분에서 호출된다 (바닐라 이동 계산 직전).
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
                         boolean sneakStart, boolean grabStart, boolean jumpStart,
                         boolean leftStart, boolean rightStart, boolean backStart) {
        boolean moving() {
            return forward || back || left || right;
        }
    }

    public static void tick(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        Options options = client.options;
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        SmartMovingState state = SmartMovingPlayer.of(player);

        // 서버에 이 모드가 없으면 자세/낙하 판정이 어긋나므로 모든 기능을 끈다.
        state.modActive = ClientPlayNetworking.canSend(StateC2SPayload.ID);
        if (!state.modActive) {
            resetAll(state);
            return;
        }

        boolean sneak = options.keyShift.isDown();
        boolean sprint = options.keySprint.isDown();
        boolean grab = SmartMovingKeys.GRAB.isDown() || (cfg.grabUsesSprintKey && sprint);
        boolean jump = options.keyJump.isDown();
        boolean left = options.keyLeft.isDown();
        boolean right = options.keyRight.isDown();
        boolean back = options.keyDown.isDown();
        Input in = new Input(options.keyUp.isDown(), back, left, right, jump, sneak, sprint, grab,
                sneak && !state.prevSneakKey, grab && !state.prevGrabKey, jump && !state.prevJumpKey,
                left && !state.prevLeftKey, right && !state.prevRightKey, back && !state.prevBackKey);
        state.prevSneakKey = sneak;
        state.prevGrabKey = grab;
        state.prevJumpKey = jump;
        state.prevLeftKey = left;
        state.prevRightKey = right;
        state.prevBackKey = back;

        // 원작 beforeOnUpdate(): 틱 시작 시점에 이미 벽에 붙어 있었나
        state.wasCollidedHorizontally = player.horizontalCollision;
        state.horizontalCollisionAngle = Float.NaN;

        state.maxExhaustionForAction = Float.MAX_VALUE;
        state.maxExhaustionToStartAction = Float.MAX_VALUE;

        boolean onGround = player.onGround();
        // 바닐라 특수 상태에서는 스마트 무빙 동작을 하지 않는다.
        boolean special = player.getAbilities().flying || player.isPassenger() || player.isSpectator()
                || player.isInWater() || player.isFallFlying() || player.isSleeping() || player.isInLava();
        state.handlesJumps = !special;
        if (special) {
            state.sliding = false;
            state.headJumping = false;
            state.aerodynamic = false;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeedSquare = velocity.x * velocity.x + velocity.z * velocity.z;
        boolean standing = horizontalSpeedSquare < 0.0005;

        if (state.grabCooldown > 0) state.grabCooldown--;

        tickSprint(player, state, cfg, in, onGround, special);
        tickHeadJumpAndSlide(player, state, cfg, in, onGround, horizontalSpeedSquare);
        tickCrawl(player, state, cfg, in, onGround, special);
        tickClimb(player, state, cfg, in, onGround, special);
        ClimbCrawling.tick(player, state, in.forward() && !in.back(), in.sneak() || state.crawlToggled, special);
        tickWallAndAngleJumps(player, state, cfg, in, onGround, special);
        Vec3 jumpMotion = player.getDeltaMovement();
        state.jumpMotionX = jumpMotion.x;
        state.jumpMotionZ = jumpMotion.z;
        if (state.handlesJumps) {
            handleJumping(player, state, cfg, in, onGround, standing);
        }

        // ---------- 빠른 사다리 ----------
        if (cfg.enableFasterLadders && player.onClimbable() && in.forward() && !in.sneak() && player.horizontalCollision) {
            Vec3 v = player.getDeltaMovement();
            if (v.y < cfg.ladderUpSpeed) player.setDeltaMovement(v.x, cfg.ladderUpSpeed, v.z);
        }

        tickExhaustion(player, state, cfg, in, onGround, standing);

        state.prevOnGround = onGround;
        syncToServer(state);
    }

    /** 원작: 달리기 키를 누르고 달리면 isFast(1.5배, 지침 증가), 키 없이 달리면 isRunning(바닐라) */
    private static void tickSprint(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
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
        // 원작 isSwimSprinting / isDiveSprinting / isFlyingSprinting
        boolean wantLiquidSprint = cfg.enableSprint && in.sprint() && !in.sneak()
                && (state.swimming || state.diving || state.flying);
        boolean liquidSprintAllowed = !Exhaustion.enabled() || player.getAbilities().invulnerable
                || (state.exhaustion <= cfg.sprintExhaustionStop
                && (state.liquidFast || state.exhaustion <= cfg.sprintExhaustionStart));
        state.liquidFast = wantLiquidSprint && liquidSprintAllowed;

        double minTickDistance = state.wantClimbUp ? 0.07 * cfg.freeClimbingUpSpeedFactor
                : state.wantClimbDown ? 0.11 * cfg.freeClimbingDownSpeedFactor
                : 0.07;
        state.climbFast = wantClimbSprint && climbSprintAllowed && tickDistance >= minTickDistance;
    }

    /** 원작 2403~2450행: 헤드 점프 착지, 슬라이딩 시작/끝, 슬라이딩 → 활공 */
    private static void tickHeadJumpAndSlide(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
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

        // 슬라이딩 시작: 달리면서 잡기 + 웅크리기.
        // 원작은 "지난 틱에 땅에서 달리고 있었나"(isGroundSprinting)를 본다. 그래서 점프한 바로 다음 틱에
        // 웅크리기를 누르면 공중에서도 슬라이딩이 시작되어 점프 속도에 슬라이딩 부스트가 더해지고,
        // 떨어지기 시작하면 활공으로 바뀐다. 이것이 여우 무빙이다.
        boolean groundSprintingLastTick = state.wasFast;
        boolean sprintingNow = groundSprintingLastTick || (onGround && state.fast);
        boolean runningNow = onGround && (state.running || state.wasRunning);
        if (cfg.enableSliding && in.grab() && (sprintingNow || runningNow)
                && !state.crawling && in.sneakStart() && !player.isInWater()) {
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
    private static void tickCrawl(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
                                  Input in, boolean onGround, boolean special) {
        while (SmartMovingKeys.CRAWL.consumeClick()) {
            state.crawlToggled = !state.crawlToggled;
        }

        boolean canCrawl = cfg.enableCrawling && !special && !state.climbing
                && player.fallDistance < cfg.fallingDistanceMinimum;
        if (!canCrawl) state.crawlToggled = false;

        boolean start = in.grabStart() && in.sneak() && onGround;
        boolean keep = state.crawling && (in.sneak() || state.crawlToggled || !WallProbe.canStand(player));
        state.wasCrawling = state.crawling;
        state.crawling = canCrawl && !state.sliding && !state.headJumping && (state.crawlToggled || start || keep);
    }

    /**
     * 원작 updateEntityActionState() 의 wantClimb / wantClimbUp / wantClimbDown / isClimbHolding.
     * 실제로 벽을 타는지는 이동 직후 logic.climb.FreeClimbing 이 손/발로 잡을 곳을 찾아 정한다.
     */
    private static void tickClimb(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
                                  Input in, boolean onGround, boolean special) {
        boolean forward = in.forward() && !in.back();

        // 잡기 키로 기어가기를 막 시작했으면, 그 키를 계속 누르고 벽에 부딪혀도 오르지 않고 기어간다.
        state.wantCrawlNotClimb = (state.wantCrawlNotClimb || (in.grabStart() && !state.wasCrawling))
                && in.grab() && forward && state.crawling && player.horizontalCollision;

        boolean wouldWantClimb = (in.grab() || (state.climbHolding && in.sneak()))
                && (!state.sliding || in.grab() && forward)
                && !state.headJumping
                && !state.wantCrawlNotClimb;
        boolean wantClimb = cfg.enableClimbing && !special && wouldWantClimb;
        boolean wantCrawl = cfg.enableCrawling && (state.crawling || (in.grabStart() && in.sneak() && onGround));

        state.wantClimbUp = wantClimb && forward;
        state.wantClimbDown = wantClimb && !forward && !wantCrawl;

        boolean wantClimbHolding = (state.climbHolding && in.sneak())
                || (wantClimb && !state.crawling && (in.sneak() || state.crawlToggled));
        state.wantClimbHolding = wantClimbHolding;
        state.climbHolding = wantClimbHolding && state.climbing;

        // 원작 wantClimbCeiling: 잡기 키, 웅크리지 않음
        state.wantClimbCeiling = cfg.enableCeilingClimbing && !special && in.grab() && !in.sneak();

        // 이동 직후 벽 타기 판정에서 쓴다 (꼭대기에서 점프, 뒤로 점프)
        state.jumpStartKey = in.jumpStart();
        state.grabKey = in.grab();
    }

    /** 원작 updateEntityActionState() 의 벽 점프 / 옆·뒤 점프 판정 */
    private static void tickWallAndAngleJumps(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
                                              Input in, boolean onGround, boolean special) {
        // ---- 벽 점프 ----
        if (state.continueWallJumping && (onGround || state.climbing || !in.jump())) {
            state.continueWallJumping = false;
        }

        boolean canWallJumping = cfg.enableWallJump && !state.headJumping && !onGround && !state.climbing && !special;
        boolean triggerWallJumping = false;
        if (cfg.wallJumpDoubleClick) {
            if (canWallJumping) {
                if (in.jumpStart()) {
                    if (state.wallJumpCount == 0) {
                        state.wallJumpCount = cfg.wallJumpDoubleClickTicks;
                    } else {
                        triggerWallJumping = true;
                        state.wallJumpCount = 0;
                    }
                } else if (state.wallJumpCount > 0) {
                    state.wallJumpCount--;
                }
            } else {
                state.wallJumpCount = 0;
            }
        } else {
            triggerWallJumping = in.jumpStart();
        }

        state.wantWallJumping = canWallJumping
                && (triggerWallJumping || state.continueWallJumping
                || (state.wantWallJumping && in.jump() && !player.horizontalCollision));

        // ---- 옆/뒤 점프: 키를 빠르게 두 번 ----
        boolean canAngleJump = onGround && !state.crawling && !state.climbing && !state.sliding && !special;
        boolean canSideJump = cfg.enableSideJump && canAngleJump;
        boolean canLeftJump = canSideJump && !in.right();
        boolean canRightJump = canSideJump && !in.left();
        boolean canBackJump = cfg.enableBackJump && canAngleJump && !in.forward() && !state.fast && !state.running;

        state.leftJumpCount = countDoubleClick(canLeftJump, in.leftStart(), state.leftJumpCount, cfg);
        state.rightJumpCount = countDoubleClick(canRightJump, in.rightStart(), state.rightJumpCount, cfg);
        state.backJumpCount = countDoubleClick(canBackJump, in.backStart(), state.backJumpCount, cfg);

        // 옆 + 뒤를 함께 두 번 누르면 대각선 뒤로 (한쪽이 아직 대기 중이면 잠깐 보류: -2)
        if (state.rightJumpCount == -2 && state.backJumpCount <= 0) state.rightJumpCount = -1;
        if (state.leftJumpCount == -2 && state.backJumpCount <= 0) state.leftJumpCount = -1;
        if (state.backJumpCount == -2 && (state.leftJumpCount <= 0 || state.rightJumpCount <= 0)) state.backJumpCount = -1;

        if (state.rightJumpCount == -1 && state.backJumpCount > 0) state.rightJumpCount = -2;
        if (state.leftJumpCount == -1 && state.backJumpCount > 0) state.leftJumpCount = -2;
        if (state.backJumpCount == -1 && (state.leftJumpCount > 0 || state.rightJumpCount > 0)) state.backJumpCount = -2;
    }

    private static int countDoubleClick(boolean can, boolean started, int count, SmartMovingConfig cfg) {
        if (!can) return 0;
        if (started) return count == 0 ? cfg.angleJumpDoubleClickTicks : -1;
        return count > 0 ? count - 1 : count;
    }

    /** 원작 handleJumping(): 일반 점프, 모아 뛰기, 헤드 점프 */
    private static void handleJumping(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
                                      Input in, boolean onGround, boolean standing) {
        if (state.climbing) return;

        // 원작: 슬라이딩·기어가기 중에는 점프하지 않는다
        boolean jump = onGround && in.jump() && !state.sliding && !state.crawling;
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

        // ---- 옆/뒤 점프 ----
        int left = 0;
        int back = 0;
        if (state.leftJumpCount == -1) left++;
        if (state.rightJumpCount == -1) left--;
        if (state.backJumpCount == -1) back++;

        if (left != 0 || back != 0) {
            int angle;
            if (left > 0) angle = back == 0 ? 270 : 225;
            else if (left < 0) angle = back == 0 ? 90 : 135;
            else angle = 180;

            // 원작 getJumpSpeed(): 각도 점프는 달리기 속도로 치지 않는다
            MoveSpeed angleSpeed = sneaking && onGround ? MoveSpeed.SNEAKING
                    : standing ? MoveSpeed.STANDING
                    : MoveSpeed.WALKING;
            Jumps.tryJump(player, state, JumpType.ANGLE, angleSpeed, player.getYRot() + angle);

            state.leftJumpCount = 0;
            state.rightJumpCount = 0;
            state.backJumpCount = 0;
        }
    }

    /** 원작 handleExhaustion(): 행동하면 오르고, 배고픔이 충분하면 쉬는 동안 내려간다 */
    private static void tickExhaustion(LocalPlayer player, SmartMovingState state, SmartMovingConfig cfg,
                                       Input in, boolean onGround, boolean standing) {
        if (!Exhaustion.enabled()) {
            state.exhaustion = 0f;
            return;
        }

        Vec3 v = player.getDeltaMovement();
        boolean verticalStill = Math.abs(v.y) < 0.007;
        boolean still = standing && verticalStill && !in.moving();

        float additional = 0f;
        if (state.climbing && !still && cfg.climbExhaustion) {
            if (in.forward()) additional = cfg.climbUpExhaustionGain;
            else if (in.back()) additional = cfg.climbDownExhaustionGain;
            else additional = cfg.climbStrafeExhaustionGain;
        }
        if (state.ceilingClimbing && !standing && cfg.ceilingClimbExhaustion) {
            additional += cfg.ceilingClimbExhaustionGain;
        }
        if (state.fast || state.climbFast || state.liquidFast) {
            if (additional == 0f) additional = 1f;
            additional *= cfg.sprintExhaustionGain;
        }
        state.exhaustion += additional;

        if (state.exhaustion > 0f && player.getFoodData().getFoodLevel() > cfg.exhaustionLossFoodMinimum) {
            boolean sneaking = in.sneak() && !state.crawling && !state.sliding;
            state.exhaustion -= Exhaustion.lossFactor(onGround, standing, still, sneaking,
                    state.running, state.fast || state.climbFast || state.liquidFast,
                    state.climbing || state.ceilingClimbing || state.swimming || state.diving);
        }
        state.exhaustion = Math.max(0f, state.exhaustion);
        // 원작: 무적(크리에이티브)이면 지치지 않는다
        if (player.getAbilities().invulnerable) state.exhaustion = 0f;
    }

    private static void syncToServer(SmartMovingState state) {
        int flags = state.flags();
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
        state.ceilingClimbing = false;
        state.wantClimbCeiling = false;
        state.sliding = false;
        state.headJumping = false;
        state.aerodynamic = false;
        state.crawlToggled = false;
        state.handlesJumps = false;
        state.sprintBlocked = false;
        state.fast = false;
        state.climbFast = false;
        state.climbHolding = false;
        state.climbCrawling = false;
        state.crawlClimbing = false;
        state.swimming = false;
        state.diving = false;
        state.flying = false;
        state.liquidFast = false;
        state.climbIntoCount = 0;
        state.wantWallJumping = false;
        state.continueWallJumping = false;
        state.wantClimbUp = false;
        state.wantClimbDown = false;
        state.jumpCharge = 0;
        state.headJumpCharge = 0;
    }
}
