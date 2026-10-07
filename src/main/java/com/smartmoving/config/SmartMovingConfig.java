package com.smartmoving.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartmoving.SmartMoving;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/smartmoving.json 에 저장되는 설정값.
 * 파일이 없으면 기본값으로 새로 만든다. 값을 바꾸고 게임을 재시작하면 적용된다.
 *
 * <p>기본값은 원작 Smart Moving 16.3 (MC 1.8.9)의 기본 설정("normal" 난이도)을 그대로 옮긴 것이다.
 * 괄호 안은 원작 smart_moving_options.txt 의 키 이름.
 */
public class SmartMovingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- 기능 on/off ----
    public boolean enableClimbing = true;       // move.climb.free
    public boolean enableCrawling = true;       // move.crawl
    public boolean enableSliding = true;        // move.slide
    public boolean enableChargedJump = true;    // move.jump.charge
    public boolean enableHeadJump = true;       // move.jump.head.charge
    public boolean enableSprint = true;         // move.sprint
    public boolean enableFasterLadders = true;
    public boolean showExhaustionBar = true;
    public boolean showJumpChargeBar = true;
    /** 원작처럼 달리기 키(기본 Ctrl)가 "잡기" 키 역할도 한다. 원작은 두 키 기본값이 같았다. */
    public boolean grabUsesSprintKey = true;

    // ---- 달리기 ----
    /** 달리기 키를 누른 채 달릴 때 속도 배율, 걷기 기준 (move.sprint.factor). 바닐라 달리기는 1.3 */
    public float sprintFactor = 1.5f;

    // ---- 지침(exhaustion): 0에서 시작해 행동하면 올라가고, 쉬면 내려간다 ----
    public boolean enableExhaustion = true;
    /** 달리기 중 틱당 증가량 (move.exhaustion.sprint.gain.factor) */
    public float sprintExhaustionGain = 2f;
    /** 이 값 이하일 때만 달리기 시작 가능 (move.exhaustion.sprint.start) */
    public float sprintExhaustionStart = 50f;
    /** 이 값을 넘으면 달리기가 끊긴다 (move.exhaustion.sprint.stop) */
    public float sprintExhaustionStop = 100f;
    /** 틱당 회복량: 기본 x 상태별 배율 (move.exhaustion.*.loss.factor) */
    public float exhaustionLossBase = 1f;
    public float exhaustionLossSprinting = 0f;
    public float exhaustionLossRunning = 0.5f;
    public float exhaustionLossWalking = 1f;
    public float exhaustionLossSneaking = 1.5f;
    public float exhaustionLossStanding = 2f;
    public float exhaustionLossFalling = 2.5f;
    /** 배고픔이 이 값 이하면 지침이 회복되지 않는다 (move.exhaustion.food.minimum) */
    public int exhaustionLossFoodMinimum = 4;

    // ---- 점프 ----
    /** 달리기/뛰기 점프의 수평 속도 배율 (move.jump.sprint/run.horizontal.factor) */
    public float sprintJumpHorizontalFactor = 2f;
    public float runJumpHorizontalFactor = 2f;
    /** 점프 지침: 증가량 / 이 값 이하일 때만 점프 가능 (move.jump.sprint/run.exhaustion.*) */
    public float sprintJumpExhaustionGain = 65f;
    public float sprintJumpExhaustionStop = 35f;
    public float runJumpExhaustionGain = 60f;
    public float runJumpExhaustionStop = 40f;

    // ---- 모아 뛰기: 웅크리고 제자리에서 점프 키를 누르고 있다가 뗀다 ----
    public int jumpChargeMaximum = 20;          // move.jump.charge.maximum (틱)
    public float jumpChargeFactor = 1.3f;       // move.jump.charge.factor (최대 충전 시 높이 배율)
    public float jumpChargeExhaustionGain = 30f;
    public float jumpChargeExhaustionStop = 100f;

    // ---- 헤드 점프: 달리면서 잡기 + 점프 키를 누르고 있다가 뗀다 ----
    /** 이 틱 이상 모으면 일반 점프 높이, 짧게 모을수록 낮고 납작하게 날아간다 (move.jump.head.charge.maximum) */
    public int headJumpChargeMaximum = 10;
    /** 헤드 점프 중 공중 조작 배율 (move.jump.head.control.factor) */
    public float headJumpControlFactor = 0.2f;
    /** 머리부터 착지할 때 낙하 대미지 (move.fall.head.damage.*) */
    public float headFallDamageStartDistance = 2f;
    public float headFallDamageFactor = 2f;

    // ---- 옆/뒤 점프: 땅에서 좌·우·뒤 키를 빠르게 두 번 ----
    public boolean enableSideJump = true;           // move.jump.angle.side
    public boolean enableBackJump = true;           // move.jump.angle.back
    public float angleJumpHorizontalFactor = 0.3f;  // move.jump.angle.horizontal.factor
    public float angleJumpVerticalFactor = 0.2f;    // move.jump.angle.vertical.factor
    /** 두 번 누르는 간격 (틱) (move.jump.angle.double.click.ticks) */
    public int angleJumpDoubleClickTicks = 3;

    // ---- 벽 점프: 공중에서 점프 키를 두 번 누르고 누른 채 벽에 부딪히면 튕겨 나간다 ----
    public boolean enableWallJump = true;           // move.jump.wall
    public boolean enableWallHeadJump = true;       // move.jump.wall.head (잡기 키를 누르고 있으면)
    /** 점프 키를 두 번 눌러야 한다 (move.jump.wall.double.click). false 면 한 번 */
    public boolean wallJumpDoubleClick = true;
    public int wallJumpDoubleClickTicks = 3;
    public float wallUpJumpVerticalFactor = 0.4f;   // move.jump.wall.vertical.factor
    public float wallUpJumpHorizontalFactor = 0.15f;
    public float wallHeadJumpVerticalFactor = 0.3f; // 벽 점프 높이에 곱해진다
    public float wallHeadJumpHorizontalFactor = 0.15f;
    /** 이 거리 이상 떨어지는 중에는 벽 점프를 할 수 없다 */
    public float wallUpJumpFallMaximumDistance = 2f;
    public float wallHeadJumpFallMaximumDistance = 3f;
    /** 벽과 거의 수직이면 이 각도(도) 안에서 똑바로 튕겨 나간다 (move.jump.wall.orthogonal.tolerance) */
    public float wallJumpOrthogonalTolerance = 5f;
    public boolean wallJumpExhaustion = true;       // move.jump.wall.exhaustion
    public float wallUpJumpExhaustionGain = 40f;
    public float wallUpJumpExhaustionStop = 60f;
    public float wallHeadJumpExhaustionGain = 20f;
    public float wallHeadJumpExhaustionStop = 80f;

    // ---- 슬라이딩: 달리면서 잡기 + 웅크리기 ----
    public float slideExhaustionGain = 10f;     // move.jump.slide.exhaustion.gain.factor
    public float slideExhaustionStop = 90f;     // move.jump.slide.exhaustion.stop.factor
    /** 미끄러짐 배율, 클수록 멀리 미끄러진다 (move.slide.glide.factor) */
    public float slideGlideFactor = 1f;
    /** 좌우 키로 방향을 트는 각도(도/틱) (move.slide.control.angle) */
    public float slideControlDegrees = 1f;
    /** 이 속도 배율 아래로 느려지면 기어가기로 바뀐다 (move.slide.speed.stop.factor) */
    public float slideSpeedStopFactor = 1f;
    /** 슬라이딩 중 이 거리 이상 떨어지면 슬라이딩이 끝난다 (move.fall.distance.minimum) */
    public float fallingDistanceMinimum = 3f;

    // ---- 벽 타기: 잡기 키를 누른 채 블록 모서리·반블록·계단·울타리 같은 "잡을 곳"을 오른다 ----
    /** 오르기/내려가기 속도 배율 (move.climb.free.up/down.speed.factor) */
    public float freeClimbingUpSpeedFactor = 1f;
    public float freeClimbingDownSpeedFactor = 1f;
    /** 이 거리 이상 떨어지다가 벽을 잡으면 대미지 (move.climb.fall.damage.start.distance / factor) */
    public float freeClimbFallDamageStartDistance = 2f;
    public float freeClimbFallDamageFactor = 2f;
    /** 이 거리 이상 떨어지는 중에는 벽을 잡을 수 없다 (move.climb.fall.maximum.distance) */
    public float freeClimbFallMaximumDistance = 3f;
    /** 벽 타기 지침 (move.climb.exhaustion, 원작 기본값 꺼짐) */
    public boolean climbExhaustion = false;
    public float climbExhaustionStart = 60f;
    public float climbExhaustionStop = 80f;
    public float climbUpExhaustionGain = 1.2f;
    public float climbDownExhaustionGain = 1.05f;
    public float climbStrafeExhaustionGain = 1.1f;
    /** 서버에서 벽을 타는 동안 틱당 더하는 허기 소모 */
    public float climbHungerExhaustion = 0.005f;
    /** 벽에서 뒤로 점프 (move.jump.climb.back.up.vertical/horizontal.factor) */
    public float climbBackJumpVertical = 0.2f;
    public float climbBackJumpHorizontal = 0.3f;
    /** 손으로만 매달려 점프할 때 높이 배율 (move.jump.climb.*.hands.only.vertical.factor) */
    public float climbJumpHandsOnlyVerticalFactor = 0.8f;
    /** 뒤로 점프할 때 잡기 키를 누르고 있으면 헤드 점프 (move.jump.climb.back.head.on.grab) */
    public boolean climbJumpBackHeadOnGrab = true;

    // ---- 천장 매달리기: 철창이나 닫힌 다락문 아래에서 잡기 키 ----
    public boolean enableCeilingClimbing = true;        // move.climb.ceiling
    /** 매달린 채 움직이는 속도 배율 (move.climb.ceiling.speed.factor) */
    public float ceilingClimbingSpeedFactor = 0.2f;
    /** 천장 매달리기 지침 (move.climb.ceiling.exhaustion, 원작 기본값 꺼짐) */
    public boolean ceilingClimbExhaustion = false;
    public float ceilingClimbExhaustionStart = 40f;
    public float ceilingClimbExhaustionStop = 60f;
    public float ceilingClimbExhaustionGain = 1.3f;

    // ---- 사다리 ----
    public float ladderUpSpeed = 0.3f;

    public static SmartMovingConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("smartmoving.json");
        SmartMovingConfig config = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                config = GSON.fromJson(reader, SmartMovingConfig.class);
            } catch (Exception e) {
                SmartMoving.LOGGER.error("Could not read {}, using defaults", path, e);
            }
        }
        if (config == null) {
            config = new SmartMovingConfig();
        }
        // 새 버전에서 추가된 항목도 파일에 써 두기 위해 항상 다시 저장한다.
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            SmartMoving.LOGGER.error("Could not write {}", path, e);
        }
        return config;
    }
}
