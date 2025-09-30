package com.yourpkg.smartmoving.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientInit implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("smartmoving");

    @Override
    public void onInitializeClient() {
        // 클라 전용 초기화 (HUD 렌더, 키 입력, ClientPlayNetworking 등록 등)
        LOG.info("SmartMoving client initializer loaded");
    }
}
