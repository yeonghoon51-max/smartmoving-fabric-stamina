package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;

/**
 * 원작의 지침(exhaustion) 계산. 행동할수록 올라가고 쉬면 내려간다.
 * 각 행동에는 "이 값 이하일 때만 시작"(start)과 "이 값을 넘으면 중단"(stop) 기준이 있다.
 */
public final class Exhaustion {
    private Exhaustion() {
    }

    public static boolean enabled() {
        return SmartMoving.CONFIG.enableExhaustion;
    }

    /** 이 행동을 시작/계속할 수 있는지 확인하고, HUD 에 표시할 기준선을 기록한다. */
    public static boolean allows(SmartMovingState state, float stop, float gain) {
        if (!enabled()) return true;
        if (state.exhaustion > stop) return false;
        state.maxExhaustionToStartAction = Math.min(state.maxExhaustionToStartAction, stop);
        state.maxExhaustionForAction = Math.min(state.maxExhaustionForAction, stop + gain);
        return true;
    }

    public static void add(SmartMovingState state, float amount) {
        if (enabled()) state.exhaustion += amount;
    }

    /** 원작 getFactor(false, ...): 이번 틱의 회복 배율 */
    public static float lossFactor(boolean onGround, boolean standing, boolean still, boolean sneaking,
                                   boolean running, boolean sprinting, boolean climbing) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        boolean airBorne = !onGround && !climbing;
        boolean isStanding = climbing ? still : standing;
        float factor = cfg.exhaustionLossBase;
        if (airBorne) factor *= cfg.exhaustionLossFalling;
        else if (sprinting) factor *= cfg.exhaustionLossSprinting;
        else if (running) factor *= cfg.exhaustionLossRunning;
        else if (sneaking && !isStanding) factor *= cfg.exhaustionLossSneaking;
        else if (isStanding) factor *= cfg.exhaustionLossStanding;
        else factor *= cfg.exhaustionLossWalking;
        return factor;
    }

    /** HUD 의 최대치: 켜져 있는 행동들의 (stop + gain) 중 최댓값 */
    public static float maximum() {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        float max = 0f;
        if (cfg.enableSprint) max = Math.max(max, cfg.sprintExhaustionStop);
        max = Math.max(max, cfg.sprintJumpExhaustionStop + cfg.sprintJumpExhaustionGain);
        max = Math.max(max, cfg.runJumpExhaustionStop + cfg.runJumpExhaustionGain);
        if (cfg.enableChargedJump) max = Math.max(max, cfg.jumpChargeExhaustionStop);
        if (cfg.enableSliding) max = Math.max(max, cfg.slideExhaustionStop + cfg.slideExhaustionGain);
        if (cfg.enableWallJump && cfg.wallJumpExhaustion) {
            max = Math.max(max, cfg.wallUpJumpExhaustionStop + cfg.wallUpJumpExhaustionGain);
            if (cfg.enableWallHeadJump) max = Math.max(max, cfg.wallHeadJumpExhaustionStop + cfg.wallHeadJumpExhaustionGain);
        }
        if (cfg.enableClimbing && cfg.climbExhaustion) max = Math.max(max, cfg.climbExhaustionStop);
        return Math.max(1f, max);
    }
}
