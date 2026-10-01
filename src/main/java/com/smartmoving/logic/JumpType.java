package com.smartmoving.logic;

/** 원작 SmartMovingClientConfig 의 점프 종류 (벽 점프·각도 점프는 아직 없음) */
public enum JumpType {
    /** 일반 점프 */
    UP,
    /** 모아 뛰기 */
    CHARGE_UP,
    /** 헤드 점프 */
    HEAD_UP,
    /** 슬라이딩 시작 (위로 뜨지 않고 앞으로 튀어나감) */
    SLIDE_DOWN,
    /** 벽 타다가 위로 */
    CLIMB_UP,
    CLIMB_UP_HANDS_ONLY,
    /** 벽 타다가 뒤로 */
    CLIMB_BACK_UP,
    CLIMB_BACK_UP_HANDS_ONLY,
    /** 벽 타다가 잡기 누른 채 뒤로 = 헤드 점프 */
    CLIMB_BACK_HEAD,
    CLIMB_BACK_HEAD_HANDS_ONLY;

    public boolean isClimb() {
        return ordinal() >= CLIMB_UP.ordinal();
    }

    public boolean isClimbBack() {
        return this == CLIMB_BACK_UP || this == CLIMB_BACK_UP_HANDS_ONLY
                || this == CLIMB_BACK_HEAD || this == CLIMB_BACK_HEAD_HANDS_ONLY;
    }

    public boolean isHandsOnly() {
        return this == CLIMB_UP_HANDS_ONLY || this == CLIMB_BACK_UP_HANDS_ONLY || this == CLIMB_BACK_HEAD_HANDS_ONLY;
    }
}
