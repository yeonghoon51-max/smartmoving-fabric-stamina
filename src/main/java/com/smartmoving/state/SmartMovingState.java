package com.smartmoving.state;

/**
 * 플레이어 한 명의 스마트 무빙 상태.
 *
 * <p>위쪽 네 개(crawling/climbing/sliding/headJumping)는 네트워크로 동기화되는 값이고,
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
    /** 머리부터 날아가는 중 (헤드 점프, 또는 슬라이딩하다 떨어짐) */
    public boolean headJumping;

    // ---- 서버 전용 ----
    /** 헤드 점프가 끝난 서버 틱 (머리부터 착지할 때 낙하 대미지 계산용) */
    public int headJumpEndAge = Integer.MIN_VALUE;
    /** 지난 틱에 벽을 타고 있었다 (벽을 잡는 순간의 낙하 대미지 판정용) */
    public boolean serverWasClimbing;

    // ---- 클라이언트 전용 ----
    /** 공기 저항이 거의 없는 비행 (슬라이딩하다 떨어졌을 때). 원작의 isAerodynamic */
    public boolean aerodynamic;
    /** 원작의 exhaustion. 0 = 멀쩡함, 올라갈수록 지침 */
    public float exhaustion;
    /** 이번 틱에 하고 있는 행동을 계속할 수 있는 최대 지침 (HUD 표시용) */
    public float maxExhaustionForAction = Float.MAX_VALUE;
    /** 이번 틱에 하려는 행동을 시작할 수 있는 최대 지침 (HUD 표시용) */
    public float maxExhaustionToStartAction = Float.MAX_VALUE;
    /** 모아 뛰기 충전 틱 수 */
    public int jumpCharge;
    /** 헤드 점프 충전 틱 수 */
    public int headJumpCharge;
    /** 모으다 취소한 점프가 키를 뗄 때까지 다시 나가지 않도록 */
    public boolean blockJumpTillButtonRelease;
    /** 달리기 점프로 공중에 있는 중 (헤드 점프 충전 조건) */
    public boolean sprintJump;
    /** 원작의 isFast: 달리기 키를 누르고 달리는 중 */
    public boolean fast;
    public boolean wasFast;
    /** 원작의 isRunning: 달리기 키 없이(W 두 번) 달리는 중 */
    public boolean running;
    public boolean wasRunning;
    /** 지쳐서 달리기를 막고 있다 */
    public boolean sprintBlocked;
    /** 이 모드가 점프를 직접 처리한다 (바닐라 jump() 를 막음) */
    public boolean handlesJumps;
    public boolean crawlToggled;
    public boolean prevOnGround = true;
    public boolean prevSneakKey;
    public boolean prevGrabKey;
    public boolean prevJumpKey;
    /** 벽 점프 직후 다시 벽에 달라붙지 않도록 하는 쿨다운 */
    public int grabCooldown;

    // ---- 벽 타기 (원작 handleClimbing) ----
    /** 원작 wantClimbUp / wantClimbDown: 잡기 키를 누른 채 앞키를 누르고 있나 / 아닌가 */
    public boolean wantClimbUp;
    public boolean wantClimbDown;
    /** 원작 isClimbHolding: 벽에 매달린 채 웅크리기 키로 버티는 중 */
    public boolean climbHolding;
    /** 원작 isNeighborClimbing: 바로 옆(정면 방향)에 손/발로 잡을 곳이 있다 */
    public boolean neighborClimbing;
    /** 원작 isFast 의 벽 타기 부분 (isClimbSprinting) */
    public boolean climbFast;
    /** 이번 틱에 점프 키를 새로 눌렀다 / 잡기 키를 누르고 있다 (이동 후 벽 타기 판정에서 쓴다) */
    public boolean jumpStartKey;
    public boolean grabKey;
    /** 이번 틱 travel() 시작 시점의 정보 (원작 moveEntityWithHeading 시작 부분) */
    public boolean wasClimbingThisTravel;
    public double travelHorizontalDamping = 0.91;
    public double lastHorizontalCollisionX;
    public double lastHorizontalCollisionZ;
    /** 지난 틱 위치 (원작 getTickDistance) */
    public double lastTickX;
    public double lastTickY;
    public double lastTickZ;
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

    /** 누워 있는 자세(기어가기, 슬라이딩, 헤드 점프)가 필요한가. 원작의 heightOffset = -1 */
    public boolean wantsLowPose() {
        return crawling || sliding || headJumping;
    }
}
