package com.smartmoving.logic.climb;

/** 원작 net.smart.moving.HandsClimbing: 손이 잡은 상태 (값이 클수록 위로 잘 올라감) */
public final class HandsClimbing {
    public static final int MIDDLE_GRAB = 2;
    public static final int UP_GRAB = 1;
    public static final int NO_GRAB = 0;

    public static final HandsClimbing NONE = new HandsClimbing(-3, "None");
    public static final HandsClimbing SINK = new HandsClimbing(-2, "Sink");
    public static final HandsClimbing TOP_HOLD = new HandsClimbing(-1, "TopHold");
    public static final HandsClimbing BOTTOM_HOLD = new HandsClimbing(0, "BottomHold");
    public static final HandsClimbing UP = new HandsClimbing(1, "Up");
    public static final HandsClimbing FAST_UP = new HandsClimbing(2, "FastUp");

    private final int value;
    private final String name;

    private HandsClimbing(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public boolean isRelevant() {
        return value > NONE.value;
    }

    public boolean isUp() {
        return this == UP || this == FAST_UP;
    }

    public HandsClimbing toUp() {
        return this == BOTTOM_HOLD ? UP : this;
    }

    public HandsClimbing toDown() {
        return this == TOP_HOLD ? SINK : this;
    }

    public HandsClimbing max(HandsClimbing other, ClimbGap inoutThisClimbGap, ClimbGap otherClimbGap) {
        if (!otherClimbGap.skipGaps) {
            inoutThisClimbGap.canStand |= otherClimbGap.canStand;
            inoutThisClimbGap.mustCrawl |= otherClimbGap.mustCrawl;
        }
        if (value < other.value) {
            inoutThisClimbGap.block = otherClimbGap.block;
            inoutThisClimbGap.meta = otherClimbGap.meta;
            inoutThisClimbGap.direction = otherClimbGap.direction;
        }
        return value >= other.value ? this : other;
    }

    @Override
    public String toString() {
        return name;
    }
}
