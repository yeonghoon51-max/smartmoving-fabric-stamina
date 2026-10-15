package com.smartmoving;

import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.network.SmartMovingNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 공용(서버+클라이언트) 진입점.
 * 설정을 읽고 네트워크 패킷을 등록한다.
 */
public class SmartMoving implements ModInitializer {
    public static final String MOD_ID = "smartmoving";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static SmartMovingConfig CONFIG = new SmartMovingConfig();

    @Override
    public void onInitialize() {
        CONFIG = SmartMovingConfig.load();
        SmartMovingNetworking.registerCommon();
        LOGGER.info("Smart Moving loaded");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
