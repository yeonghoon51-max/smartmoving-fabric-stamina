package com.yourpkg.smartmoving;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartMovingMod implements ModInitializer {
    public static final String MOD_ID = "smartmoving";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 서버/클라 공통 초기화 (레지스트리 등)
        // ⚠ 여기에서는 MinecraftClient, InGameHud 같은 "클라이언트 전용" 클래스를 절대 참조하지 마세요.
        LOG.info("SmartMoving (Stage2) common initializer loaded");
    }
}
