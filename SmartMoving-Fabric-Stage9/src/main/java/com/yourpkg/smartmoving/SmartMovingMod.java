package com.yourpkg.smartmoving;

import com.yourpkg.smartmoving.config.SmartMovingConfig;
import com.yourpkg.smartmoving.network.ServerNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartMovingMod implements ModInitializer {
    public static final String MOD_ID = "smartmoving";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    public static SmartMovingConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = SmartMovingConfig.load();
        ServerNetworking.register();
        LOG.info("SmartMoving Stage9 (Original Behavior) init");
    }
}
