package com.yourpkg.smartmoving;

import com.yourpkg.smartmoving.network.ServerNetworking;
import net.fabricmc.api.ModInitializer;

public class SmartMovingMod implements ModInitializer {
    public static final Config CONFIG = new Config();

    @Override public void onInitialize() { ServerNetworking.register(); }

    public static class Config {
        public int hudSegments = 10;
        public float jumpChargePerTick = 0.9f;  // crouch charge fill speed
        public float jumpMinToTrigger = 10f;
        public float jumpBoostMax = 0.8f;       // max extra Y when fully charged
        public float grabDrainPerTick = 0.6f;   // grabbing drains energy
        public float grabRegenPerTick = 0.5f;   // idle regen
    }
}
