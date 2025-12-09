package com.yourpkg.smartmoving.client;

import com.yourpkg.smartmoving.client.net.ClientNetworking;
import com.yourpkg.smartmoving.client.ui.ClientState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientInit implements ClientModInitializer {
    private static KeyBinding grabKey;

    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        grabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smartmoving.grab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.smartmoving.category"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean pressed = grabKey.isPressed();
            if (pressed != ClientState.grabKeyDown) {
                ClientState.grabKeyDown = pressed;
                ClientNetworking.sendAction((byte)(pressed ? 1 : 2));
            }
        });
    }
}
