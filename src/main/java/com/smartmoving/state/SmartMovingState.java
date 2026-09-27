package com.smartmoving.state;

/**
 * 플레이어 한 명의 스마트 무빙 상태.
 *
 * <p>위쪽 세 개(crawling/climbing/sliding)는 네트워크로 동기화되는 값이고,
 * 나머지는 자기 캐릭터를 조종하는 클라이언트에서만 쓰는 값이다.
 */
public class SmartMovingState {
    public static final byte FLAG_CRAWLING = 1;
    public static final byte FLAG_CLIMBING = 1 << 1;
    public static final byte FLAG_SLIDING = 1 << 2;
    public static final byte FLAG_HEAD_JUMPING = 1 << 3;

    // ---- 동기화되는 상태 ----
    public boolean crawling;
    public boolean climbing;
    public boolean sliding;
    /** 머리부터 앞으로 날아가는 점프(헤드 점프) 중 */
    public boolean headJumping;

    // ---- 클라이언트 전용 ----
    /** 음수면 아직 초기화 전 (첫 틱에 최대치로 채운다) */
    public float stamina = -1f;
    public boolean exhausted;
    /** 0~1, 모아 뛰기 충전량 */
    public float jumpCharge;
    public int slideTicks;
    /** 벽 점프 직후 다시 벽에 달라붙지 않도록 하는 쿨다운 */
    public int grabCooldown;
    public boolean crawlToggled;
    /** 슬라이딩 후 웅크리기 키를 계속 누르고 있으면 기어가기 유지 */
    public boolean crawlFromSlide;
    /** 이번 점프를 헤드 점프로 바꿀지 (컨트롤러가 켜고, jump() 믹스인이 사용) */
    public boolean headJumpArmed;
    public boolean prevOnGround = true;
    public boolean prevJumpKey;
    public boolean prevSneakKey;
    public byte lastSentFlags;
    public int resendTimer;

    public byte flags() {
        byte f = 0;
        if (crawling) f |= FLAG_CRAWLING;
        if (climbing) f |= FLAG_CLIMBING;
        if (sliding) f |= FLAG_SLIDING;
        if (headJumping) f |= FLAG_HEAD_JUMPING;
        return f;
    }

    public void applyFlags(byte f) {
        crawling = (f & FLAG_CRAWLING) != 0;
        climbing = (f & FLAG_CLIMBING) != 0;
        sliding = (f & FLAG_SLIDING) != 0;
        headJumping = (f & FLAG_HEAD_JUMPING) != 0;
    }

    /** 누워 있는 자세(기어가기, 슬라이딩, 헤드 점프)가 필요한가 */
    public boolean wantsLowPose() {
        return crawling || sliding || headJumping;
    }
}
