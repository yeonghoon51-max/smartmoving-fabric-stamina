package com.smartmoving.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** 조작 설정 화면의 "Smart Moving" 항목에 나오는 키들 */
public final class SmartMovingKeys {
    private static final String CATEGORY = "key.categories.smartmoving";

    /** 누르고 있는 동안 앞의 벽을 붙잡고 탄다 (원작 기본값 R) */
    public static KeyBinding GRAB;
    /** 기어가기 켜기/끄기 */
    public static KeyBinding CRAWL;

    private SmartMovingKeys() {
    }

    public static void register() {
        GRAB = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.smartmoving.grab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
        CRAWL = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.smartmoving.crawl", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));
    }
}
