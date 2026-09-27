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
 */
public class SmartMovingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- 기능 on/off ----
    public boolean enableStamina = true;
    public boolean enableClimbing = true;
    public boolean enableCrawling = true;
    public boolean enableSliding = true;
    public boolean enableChargedJump = true;
    public boolean enableWallJump = true;
    public boolean enableHeadJump = true;
    public boolean enableFasterLadders = true;
    public boolean showHud = true;

    // ---- 스태미나(지구력) ----
    public float maxStamina = 100f;
    /** 가만히 있거나 걸을 때 틱당 회복량 */
    public float staminaRegenPerTick = 0.5f;
    /** 공중에 있을 때 틱당 회복량 */
    public float staminaRegenAirPerTick = 0.1f;
    /** 달리기 틱당 소모량 */
    public float sprintStaminaPerTick = 0.15f;
    /** 지친 상태에서 풀려나기 위해 필요한 스태미나 비율 (0~1) */
    public float exhaustedRecoverFraction = 0.3f;

    // ---- 벽 타기 ----
    public float climbUpSpeed = 0.12f;
    public float climbDownSpeed = 0.15f;
    public float climbSideSpeed = 0.07f;
    public float climbHoldStaminaPerTick = 0.2f;
    public float climbMoveStaminaPerTick = 0.45f;
    /** 서버에서 벽을 타는 동안 틱당 더하는 허기 소모 */
    public float climbHungerExhaustion = 0.005f;
    /** 벽 위 턱을 넘을 때 위로 튀어오르는 속도 */
    public float ledgeClimbBoost = 0.42f;
    /** 벽에 매달린 채 점프 → 위로 도약 */
    public float climbJumpUp = 0.5f;
    public float climbJumpStaminaCost = 10f;

    // ---- 벽 점프 (벽에 매달린 채 뒤 + 점프) ----
    public float wallJumpUp = 0.55f;
    public float wallJumpPush = 0.45f;
    public float wallJumpStaminaCost = 15f;

    // ---- 슬라이딩 (달리다가 웅크리기) ----
    public float slideBoost = 0.3f;
    public float slideFriction = 0.96f;
    public int slideMaxTicks = 30;
    public float slideStaminaCost = 10f;
    /** 슬라이딩 시작 부스트는 이 속도보다 느릴 때만 붙는다 (무한 가속 방지) */
    public float slideMaxBoostedSpeed = 0.45f;
    /** 공중에서 웅크리기를 누른 채 착지하면 속도를 유지한 채 슬라이딩으로 이어진다 */
    public float landingSlideMinSpeed = 0.15f;
    public float landingSlideStaminaCost = 4f;

    // ---- 헤드 점프 / 여우 점프 (달리기 + 잡기 + 점프) ----
    /** 앞으로 더해지는 속도 */
    public float headJumpBoost = 0.35f;
    public float headJumpUp = 0.36f;
    /** 헤드 점프 수평 최고 속도 (슬라이딩 → 헤드 점프 연계 시 상한) */
    public float headJumpMaxSpeed = 1.0f;
    /** 공중 수평 감속 (1 = 감속 없음, 바닐라 공중은 0.91) */
    public float headJumpAirDrag = 0.985f;
    /** 공중에서 바라보는 방향으로 꺾이는 정도 (0~1) */
    public float headJumpSteering = 0.08f;
    /** 헤드 점프를 쓰려면 이 정도 수평 속도가 있어야 한다 (달리는 중이면 무시) */
    public float headJumpMinSpeed = 0.2f;
    public float headJumpStaminaCost = 15f;

    // ---- 모아 뛰기 (웅크린 채 가만히 있다가 점프) ----
    public int chargedJumpTicks = 20;
    public float chargedJumpMaxMultiplier = 1.9f;
    public float chargedJumpStaminaCost = 20f;

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
