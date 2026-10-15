package com.smartmoving.client;

import com.smartmoving.SmartMoving;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

/** 조작 설정 화면의 "Smart Moving" 항목에 나오는 키들 */
public final class SmartMovingKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SmartMoving.id("main"));

    /**
     * 잡기: 벽 타기, 슬라이딩(달리며 + 웅크리기), 헤드 점프(달리며 + 점프), 기어가기(웅크린 채 + 잡기).
     * 원작은 달리기 키와 같은 Ctrl 이 기본값이었다. 마인크래프트는 한 키에 두 동작을 걸 수 없으므로
     * 기본은 "지정 안 함" + 설정 grabUsesSprintKey=true (달리기 키가 잡기도 겸함)로 같은 조작감을 낸다.
     */
    public static KeyMapping GRAB;
    /** 키 지정 안 함 (GLFW_KEY_UNKNOWN) */
    private static final int UNBOUND = -1;
    /** 기어가기 켜기/끄기 */
    public static KeyMapping CRAWL;

    private SmartMovingKeys() {
    }

    public static void register() {
        GRAB = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.smartmoving.grab", InputConstants.Type.KEYBOARD, UNBOUND, CATEGORY));
        CRAWL = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.smartmoving.crawl", InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, CATEGORY));
    }
}
