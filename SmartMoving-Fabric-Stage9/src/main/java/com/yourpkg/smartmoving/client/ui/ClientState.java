package com.yourpkg.smartmoving.client.ui;

public class ClientState {
    // Blue arrows (charge via crouch)
    public static float jumpCharge = 0f;   // 0..100
    // Lightning (grab energy drains while grabbing)
    public static float grabEnergy = 100f; // 0..100
    public static boolean grabbing = false;
    public static boolean crawling = false;
}
