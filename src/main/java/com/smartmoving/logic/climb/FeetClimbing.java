package com.smartmoving.logic.climb;

/** 원작 net.smart.moving.FeetClimbing: 발이 디딘 상태 */
public final class FeetClimbing {
    public static final int DOWN_STEP = 1;
    public static final int NO_STEP = 0;

    public static final FeetClimbing NONE = new FeetClimbing(-3, "None");
    public static final FeetClimbing BASE_HOLD = new FeetClimbing(-2, "BaseHold");
    public static final FeetClimbing BASE_WITH_HANDS = new FeetClimbing(-1, "BaseWithHands");
    public static final FeetClimbing TOP_WITH_HANDS = new FeetClimbing(0, "TopWithHands");
    public static final FeetClimbing SLOW_UP_WITH_HOLD_WITHOUT_HANDS = new FeetClimbing(1, "SlowUpWithHoldWithoutHands");
    public static final FeetClimbing SLOW_UP_WITH_SINK_WITHOUT_HANDS = new FeetClimbing(2, "SlowUpWithSinkWithoutHands");
    public static final FeetClimbing FAST_UP = new FeetClimbing(3, "FastUp");

    private final int value;
    private final String name;

    private FeetClimbing(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public boolean isRelevant() {
        return value > NONE.value;
    }

    public boolean isIndependentlyRelevant() {
        return value > BASE_WITH_HANDS.value;
    }

    public boolean isUp() {
        return this == SLOW_UP_WITH_HOLD_WITHOUT_HANDS || this == SLOW_UP_WITH_SINK_WITHOUT_HANDS || this == FAST_UP;
    }

    public FeetClimbing max(FeetClimbing other, ClimbGap inoutThisClimbGap, ClimbGap otherClimbGap) {
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
