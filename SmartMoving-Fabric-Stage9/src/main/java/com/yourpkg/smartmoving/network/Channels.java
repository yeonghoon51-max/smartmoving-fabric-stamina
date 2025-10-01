package com.yourpkg.smartmoving.network;

import net.minecraft.util.Identifier;

public class Channels {
    public static final Identifier SYNC = Identifier.of("smartmoving", "sync");    // S2C context
    public static final Identifier ACTION = Identifier.of("smartmoving", "action"); // C2S inputs (grab on/off)
}
