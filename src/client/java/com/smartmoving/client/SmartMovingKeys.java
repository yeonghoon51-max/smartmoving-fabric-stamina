package com.smartmoving.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** 조작 설정 화면의 "Smart Moving" 항목에 나오는 키들 */
public final class SmartMovingKeys {
    private static final String CATEGORY = "key.categories.smartmoving";

    /**
     * 잡기: 벽 타기, 슬라이딩(달리며 + 웅크리기), 헤드 점프(달리며 + 점프), 기어가기(웅크린 채 + 잡기).
     * 원작은 달리기 키와 같은 Ctrl 이 기본값이었다. 마인크래프트는 한 키에 두 동작을 걸 수 없으므로
     * 기본은 "지정 안 함" + 설정 grabUsesSprintKey=true (달리기 키가 잡기도 겸함)로 같은 조작감을 낸다.
     */
    public static KeyBinding GRAB;
    /** 기어가기 켜기/끄기 */
    public static KeyBinding CRAWL;

    private SmartMovingKeys() {
    }

    public static void register() {
        GRAB = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.smartmoving.grab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        CRAWL = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.smartmoving.crawl", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));
    }
}
