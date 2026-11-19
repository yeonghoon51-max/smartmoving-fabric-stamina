package com.smartmoving.logic;

/** 원작 SmartMovingClientConfig 의 점프 종류 */
public enum JumpType {
    /** 일반 점프 */
    UP,
    /** 모아 뛰기 */
    CHARGE_UP,
    /** 헤드 점프 */
    HEAD_UP,
    /** 슬라이딩 시작 (위로 뜨지 않고 앞으로 튀어나감) */
    SLIDE_DOWN,
    /** 옆/뒤 점프: 좌우·뒤 키를 두 번 */
    ANGLE,
    /** 공중에서 벽을 차고 튀어나감 (잡기 키를 누르면 헤드 점프) */
    WALL_UP,
    WALL_HEAD,
    /** 이미 벽에 붙어 있을 때: 위로 뜨지 않는다 */
    WALL_UP_SLIDE,
    WALL_HEAD_SLIDE,
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
        return this == CLIMB_UP || this == CLIMB_UP_HANDS_ONLY || isClimbBack();
    }

    public boolean isClimbBack() {
        return this == CLIMB_BACK_UP || this == CLIMB_BACK_UP_HANDS_ONLY
                || this == CLIMB_BACK_HEAD || this == CLIMB_BACK_HEAD_HANDS_ONLY;
    }

    public boolean isWall() {
        return this == WALL_UP || this == WALL_HEAD || this == WALL_UP_SLIDE || this == WALL_HEAD_SLIDE;
    }

    public boolean isHandsOnly() {
        return this == CLIMB_UP_HANDS_ONLY || this == CLIMB_BACK_UP_HANDS_ONLY || this == CLIMB_BACK_HEAD_HANDS_ONLY;
    }
}
