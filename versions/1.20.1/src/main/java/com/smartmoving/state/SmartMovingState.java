package com.smartmoving.state;

/**
 * 플레이어 한 명의 스마트 무빙 상태.
 *
 * <p>위쪽 열 개(crawling ~ flying)는 네트워크로 동기화되는 값이고,
 * 나머지는 자기 캐릭터를 조종하는 클라이언트에서만 쓰는 값이다.
 */
public class SmartMovingState {
    public static final int FLAG_CRAWLING = 1;
    public static final int FLAG_CLIMBING = 1 << 1;
    public static final int FLAG_SLIDING = 1 << 2;
    public static final int FLAG_HEAD_JUMPING = 1 << 3;
    public static final int FLAG_CEILING_CLIMBING = 1 << 4;
    public static final int FLAG_CLIMB_CRAWLING = 1 << 5;
    public static final int FLAG_CRAWL_CLIMBING = 1 << 6;
    public static final int FLAG_SWIMMING = 1 << 7;
    public static final int FLAG_DIVING = 1 << 8;
    public static final int FLAG_FLYING = 1 << 9;

    // ---- 동기화되는 상태 ----
    public boolean crawling;
    public boolean climbing;
    public boolean sliding;
    /** 머리부터 날아가는 중 (헤드 점프, 또는 슬라이딩하다 떨어짐) */
    public boolean headJumping;
    /** 천장(철창, 닫힌 다락문)에 매달린 중 */
    public boolean ceilingClimbing;
    /**
     * 원작 isClimbCrawling: 벽에 매달린 채 윗몸만 앞의 낮은 틈으로 밀어 넣는 중.
     * 몸(충돌 상자)은 납작해지고 원래 몸의 윗부분 높이에 있다 (원작 heightOffset = -1).
     */
    public boolean climbCrawling;
    /** 원작 isCrawlClimbing: 기어가던 자세 그대로 턱을 오르는 중 */
    public boolean crawlClimbing;
    /** 원작 isSwimming: 물 위에서 머리를 내놓고 헤엄치는 중 */
    public boolean swimming;
    /** 원작 isDiving: 물속에서 보는 방향으로 잠수하는 중 (납작한 자세) */
    public boolean diving;
    /** 원작 isFlying: 원작 비행 (보는 방향으로 나는 비행, 납작한 자세) */
    public boolean flying;

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
    public boolean prevLeftKey;
    public boolean prevRightKey;
    public boolean prevBackKey;
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
    /** 원작 wantClimbHolding: 잡기 + 웅크리기로 매달리려고 한다 (기어가는 중 제외) */
    public boolean wantClimbHolding;
    /** 원작 wantCrawlNotClimb: 잡기 키로 기어가기를 막 시작했으면 벽에 부딪혀도 오르지 않는다 */
    public boolean wantCrawlNotClimb;
    /** 원작 wasCrawling: 이번 틱 판정 전에 기어가고 있었다 */
    public boolean wasCrawling;
    /** 원작 hasClimbGap / hasClimbCrawlGap: 손/발 위로 일어설 수 있는 틈 / 기어야 들어가는 틈이 있다 */
    public boolean hasClimbGap;
    public boolean hasClimbCrawlGap;
    /** 원작 climbIntoCount: 틈으로 들어간 뒤 몇 틱 동안 매달린 자세를 유지한다 */
    public int climbIntoCount;
    /** 원작 wantClimbCeiling: 잡기 키를 누르고 있고 웅크리지 않았다 */
    public boolean wantClimbCeiling;
    public boolean wasCeilingClimbingThisTravel;
    /** 이번 틱에 점프 키를 새로 눌렀다 / 잡기 키를 누르고 있다 (이동 후 벽 타기 판정에서 쓴다) */
    public boolean jumpStartKey;
    public boolean grabKey;
    /** 이번 틱 travel() 시작 시점의 정보 (원작 moveEntityWithHeading 시작 부분) */
    public boolean wasClimbingThisTravel;
    public double travelHorizontalDamping = 0.91;
    public double lastHorizontalCollisionX;
    public double lastHorizontalCollisionZ;
    // ---- 벽 점프 / 옆·뒤 점프 ----
    /** 원작 wantWallJumping: 이번 틱에 벽에 부딪히면 벽 점프를 한다 */
    public boolean wantWallJumping;
    /** 원작 continueWallJumping: 벽 점프 후 점프 키를 계속 누르면 다음 벽에서도 튕긴다 */
    public boolean continueWallJumping;
    public int wallJumpCount;
    /** 이번 이동에서 부딪힌 벽 방향 (yaw 각도, 없으면 NaN) */
    public float horizontalCollisionAngle = Float.NaN;
    /** 틱 시작 시점에 이미 벽에 붙어 있었다 (원작 wasCollidedHorizontally) */
    public boolean wasCollidedHorizontally;
    /** 원작 jumpMotionX/Z: 틱 시작 시점의 수평 속도 (이동 후 점프 계산용) */
    public double jumpMotionX;
    public double jumpMotionZ;
    /** 좌/우/뒤 키 두 번 누르기 카운터 (원작 leftJumpCount 등: 양수 = 대기 틱, -1 = 발동, -2 = 보류) */
    public int leftJumpCount;
    public int rightJumpCount;
    public int backJumpCount;

    /** 서버에도 이 모드가 있어 스마트 무빙이 켜져 있다 (ClientMovementController 가 매 틱 정한다) */
    public boolean modActive;

    // ---- 수영 / 잠수 / 비행 ----
    /** 원작 isSwimSprinting / isDiveSprinting / isFlyingSprinting: 물속이나 비행 중 달리기 키 */
    public boolean liquidFast;
    /** 원작 isDipping: 얕은 물에서 걷는 중 */
    public boolean dipping;
    /** 원작 isLevitating: 물속에서 가만히 떠 있는 중 */
    public boolean levitating;
    /** 원작 isJumpingOutOfWater: 물가 벽을 짚고 물 밖으로 뛰어오르는 중 */
    public boolean jumpingOutOfWater;
    public int waterMovementTicks;

    // ---- 애니메이션 통계 (원작 Smart Render 의 SmartStatistics). 모든 플레이어에 대해 클라이언트에서 계산한다 ----
    public final AnimationStat animHorizontal = new AnimationStat();
    public final AnimationStat animVertical = new AnimationStat();
    public final AnimationStat animAll = new AnimationStat();
    public double animLastX;
    public double animLastY;
    public double animLastZ;
    public boolean animHasLast;
    /** 움직이는 방향 (라디안, 원작 currentHorizontalAngle / currentVerticalAngle) */
    public float animHorizontalAngle;
    public float animVerticalAngle;
    public float animHorizontalDistance;

    /** 원작 SmartStatisticsData: 바닐라 팔다리 흔들기와 같은 방식(거리 x 4, 0.4 로 따라가기)으로 누적 */
    public static final class AnimationStat {
        public float speed;
        public float total;

        public void add(double distance) {
            float d = (float) distance * 4F;
            speed += (d - speed) * 0.4F;
            total += speed;
        }

        public float currentSpeed() {
            return Math.min(1F, speed);
        }
    }

    /** 지난 틱 위치 (원작 getTickDistance) */
    public double lastTickX;
    public double lastTickY;
    public double lastTickZ;
    public int lastSentFlags;
    public int resendTimer;

    public int flags() {
        int f = 0;
        if (crawling) f |= FLAG_CRAWLING;
        if (climbing) f |= FLAG_CLIMBING;
        if (sliding) f |= FLAG_SLIDING;
        if (headJumping) f |= FLAG_HEAD_JUMPING;
        if (ceilingClimbing) f |= FLAG_CEILING_CLIMBING;
        if (climbCrawling) f |= FLAG_CLIMB_CRAWLING;
        if (crawlClimbing) f |= FLAG_CRAWL_CLIMBING;
        if (swimming) f |= FLAG_SWIMMING;
        if (diving) f |= FLAG_DIVING;
        if (flying) f |= FLAG_FLYING;
        return f;
    }

    public void applyFlags(int f) {
        crawling = (f & FLAG_CRAWLING) != 0;
        climbing = (f & FLAG_CLIMBING) != 0;
        sliding = (f & FLAG_SLIDING) != 0;
        headJumping = (f & FLAG_HEAD_JUMPING) != 0;
        ceilingClimbing = (f & FLAG_CEILING_CLIMBING) != 0;
        climbCrawling = (f & FLAG_CLIMB_CRAWLING) != 0;
        crawlClimbing = (f & FLAG_CRAWL_CLIMBING) != 0;
        swimming = (f & FLAG_SWIMMING) != 0;
        diving = (f & FLAG_DIVING) != 0;
        flying = (f & FLAG_FLYING) != 0;
    }

    /** 누워 있는 자세(기어가기, 슬라이딩, 헤드 점프, 틈으로 기어 오르기, 잠수, 비행)가 필요한가. 원작의 heightOffset = -1 */
    public boolean wantsLowPose() {
        return crawling || sliding || headJumping || climbCrawling || crawlClimbing || diving || flying;
    }
}
