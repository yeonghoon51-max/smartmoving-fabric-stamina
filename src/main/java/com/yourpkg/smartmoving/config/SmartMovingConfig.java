package com.yourpkg.smartmoving.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SmartMovingConfig {
    // 충전 점프: 쪼그려 앉으면 파란 바 충전, 점프 시 추가 상승
    public float jumpChargePerTick  = 1.8f;   // 틱당 충전량 (0~100)
    public float jumpBoostMax       = 0.9f;   // 최대 추가 Y 속도 (완충 시)
    public float jumpMinToTrigger   = 10f;    // 최소 충전량 (이하면 무시)

    // 잡기/클라이밍 스태미나
    public float grabDrainPerTick   = 0.8f;   // 잡는 동안 소모량
    public float grabRegenPerTick   = 0.35f;  // 비활성 시 회복량

    // 벽 클라이밍 보조
    public float climbVerticalAssist   = 0.022f; // 잡고 있을 때 상승 보조 속도
    public float climbHorizontalBoost  = 1.05f;  // 수평 이동 배율

    // 수영 부스트
    public float swimBoost = 1.08f;   // 수영 이동 속도 배율

    // HUD
    public int hudSegments = 10;  // 바 칸 수 (1~20)

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SmartMovingConfig load() {
        try {
            File f = file();
            if (f.exists()) {
                try (FileReader r = new FileReader(f)) {
                    SmartMovingConfig cfg = GSON.fromJson(r, SmartMovingConfig.class);
                    if (cfg != null) return cfg;
                }
            }
            SmartMovingConfig def = new SmartMovingConfig();
            save(def);
            return def;
        } catch (Exception e) {
            return new SmartMovingConfig();
        }
    }

    public static void save(SmartMovingConfig cfg) {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(f)) { GSON.toJson(cfg, w); }
        } catch (Exception ignored) {}
    }

    private static File file() { return new File("config/smartmoving.json"); }
}
