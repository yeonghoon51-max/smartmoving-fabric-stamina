package com.smartmoving.logic;

import com.smartmoving.SmartMoving;
import com.smartmoving.config.SmartMovingConfig;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 헤드 점프(여우 점프): 달리기 + 잡기 + 점프로 몸을 눕혀 앞으로 멀리 날아간다.
 * 지금 가진 수평 속도(달리기, 슬라이딩 관성)에 부스트를 더하므로
 * 슬라이딩 → 헤드 점프 → 착지 슬라이딩 → 헤드 점프 ... 로 연계하면 속도가 유지된다.
 */
public final class HeadJump {
    private HeadJump() {
    }

    /** 바닐라 jump() 직후 호출된다. 바닐라가 정한 속도를 헤드 점프 속도로 덮어쓴다. */
    public static void launch(PlayerEntity player, SmartMovingState state) {
        SmartMovingConfig cfg = SmartMoving.CONFIG;
        state.headJumpArmed = false;

        Vec3d look = Vec3d.fromPolar(0f, player.getYaw());
        Vec3d v = player.getVelocity();
        // 바라보는 방향 성분의 속도만 이어받는다 (옆으로 미끄러지던 속도는 버림).
        double carried = Math.max(0.0, v.x * look.x + v.z * look.z);
        double speed = Math.min(cfg.headJumpMaxSpeed, carried + cfg.headJumpBoost);

        player.setVelocity(look.x * speed, cfg.headJumpUp, look.z * speed);
        player.setSprinting(false);
        Stamina.use(state, cfg.headJumpStaminaCost);
        state.headJumping = true;
        state.sliding = false;
        state.crawlFromSlide = false;
    }
}
