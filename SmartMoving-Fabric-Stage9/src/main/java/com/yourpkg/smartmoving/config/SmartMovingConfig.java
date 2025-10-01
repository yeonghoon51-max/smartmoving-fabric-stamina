package com.yourpkg.smartmoving.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SmartMovingConfig {
    // Original behavior knobs
    public float jumpChargePerTick = 1.8f;   // crouch hold to charge blue arrows (0..100)
    public float jumpBoostMax = 0.9f;        // extra vertical velocity at full charge
    public float jumpMinToTrigger = 10f;     // minimum charge to apply boost

    public float grabDrainPerTick = 0.8f;    // lightning drains while grabbing
    public float grabRegenPerTick = 0.35f;   // lightning regens when not grabbing

    // Climb/Swim assist (kept modest to focus on original jump/grab semantics)
    public float climbHorizontalBoost = 1.08f;
    public float climbVerticalAssist = 0.018f;
    public float swimBoost = 1.06f;

    // HUD
    public int hudSegments = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File file() { return new File("config/smartmoving.json"); }

    public static SmartMovingConfig load() {
        try {
            var f = file();
            if (f.exists()) try (FileReader r = new FileReader(f)) { return GSON.fromJson(r, SmartMovingConfig.class); }
            var cfg = new SmartMovingConfig(); save(cfg); return cfg;
        } catch (Exception e) { e.printStackTrace(); return new SmartMovingConfig(); }
    }
    public static void save(SmartMovingConfig cfg) {
        try { var f = file(); f.getParentFile().mkdirs(); try (FileWriter w = new FileWriter(f)) { GSON.toJson(cfg, w); } }
        catch (Exception e) { e.printStackTrace(); }
    }
}
