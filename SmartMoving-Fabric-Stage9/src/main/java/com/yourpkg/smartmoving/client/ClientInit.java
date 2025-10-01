package com.yourpkg.smartmoving.client;

import com.yourpkg.smartmoving.client.input.Keybinds;
import com.yourpkg.smartmoving.client.net.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class ClientInit implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientNetworking.register();
        Keybinds.register();
    }
}
