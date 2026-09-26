package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;

/** 스태미나(지구력) 계산 도우미 */
public final class Stamina {
    private Stamina() {
    }

    public static boolean has(SmartMovingState state, float amount) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        return !cfg.enableStamina || (!state.exhausted && state.stamina >= amount);
    }

    public static void use(SmartMovingState state, float amount) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        if (!cfg.enableStamina) return;
        state.stamina = Math.max(0f, state.stamina - amount);
        if (state.stamina <= 0f) {
            state.exhausted = true;
        }
    }

    public static void regen(SmartMovingState state, float amount) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        state.stamina = Math.min(cfg.maxStamina, state.stamina + amount);
        if (state.exhausted && state.stamina >= cfg.maxStamina * cfg.exhaustedRecoverFraction) {
            state.exhausted = false;
        }
    }
}
