package com.yourpkg.smartmoving.client.ui;

/** 서버에서 동기화된 플레이어 상태 (클라이언트 전용 미러) */
public final class ClientState {
    public static float   jumpCharge  = 0f;
    public static float   grabEnergy  = 100f;
    public static boolean grabbing    = false;
    public static boolean crawling    = false;
    public static boolean grabKeyDown = false;
}
