package com.smartmoving.state;

import net.minecraft.entity.player.PlayerEntity;

/**
 * PlayerEntity 에 믹스인으로 붙이는 인터페이스("duck interface").
 * 모든 플레이어 객체가 자신의 {@link SmartMovingState} 를 갖게 된다.
 */
public interface SmartMovingPlayer {
    SmartMovingState smartmoving$getState();

    static SmartMovingState of(PlayerEntity player) {
        return ((SmartMovingPlayer) player).smartmoving$getState();
    }
}
