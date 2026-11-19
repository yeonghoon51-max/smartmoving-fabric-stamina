package com.smartmoving.logic;

/** 원작의 점프 속도 구분 (getJumpSpeed) */
public enum MoveSpeed {
    /** 달리기 키를 누르고 달림 (원작 isFast) */
    SPRINTING,
    /** 달리기 키 없이 W 두 번으로 달림 (원작 isRunning) */
    RUNNING,
    WALKING,
    SNEAKING,
    STANDING
}
