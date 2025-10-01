package com.yourpkg.smartmoving.client.input;

import com.yourpkg.smartmoving.client.net.ClientNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyBinding grabKey;
    private static boolean grabDownSent = false;

    public static void register() {
        grabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.smartmoving.grab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.smartmoving"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean nowGrab = grabKey.isPressed();
            if (nowGrab && !grabDownSent) { grabDownSent = true;  ClientNetworking.sendAction((byte)1); }
            else if (!nowGrab && grabDownSent) { grabDownSent = false; ClientNetworking.sendAction((byte)2); }
        });
    }
}
