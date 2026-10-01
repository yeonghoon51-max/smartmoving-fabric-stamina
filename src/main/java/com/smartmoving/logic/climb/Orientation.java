// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (net.smart.moving.Orientation), GPL-3.0-or-later.
package com.smartmoving.logic.climb;

import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallShape;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 원작 net.smart.moving.Orientation 의 포트.
 *
 * <p>플레이어 기준 8방향(+ 제자리) 중 하나를 나타내고, 그 방향 앞 칸(remote)과 내 칸(base)의 블록을 반 블록 단위로
 * 살펴서 "손/발로 잡을 수 있는 곳"과 "그 위에 들어갈 틈의 크기"를 계산한다.
 * 평평한 벽은 잡을 곳이 없어서 오를 수 없고, 블록 모서리·반블록·계단·울타리·사다리·덩굴 같은 곳만 잡을 수 있다.
 *
 * <p>원작처럼 클라이언트 한 스레드에서만 쓰는 정적 작업 변수를 쓴다. 다른 모드 호환 코드(RedPower, 밧줄 등)는 뺐다.
 */
public final class Orientation {
    private static final Map<Direction, Orientation> FACING_TO_ORIENTATION = new EnumMap<>(Direction.class);
    private static final List<Orientation> ORTHOGONALS = new ArrayList<>();
    private static final List<Orientation> DIAGONALS = new ArrayList<>();

    public static final Orientation ZZ = new Orientation(0, 0);

    public static final Orientation PZ = new Orientation(1, 0, Direction.WEST);
    public static final Orientation ZP = new Orientation(0, 1, Direction.NORTH);
    public static final Orientation NZ = new Orientation(-1, 0, Direction.EAST);
    public static final Orientation ZN = new Orientation(0, -1, Direction.SOUTH);

    public static final Orientation PP = new Orientation(1, 1, Direction.NORTH, Direction.WEST);
    public static final Orientation NN = new Orientation(-1, -1, Direction.SOUTH, Direction.EAST);
    public static final Orientation PN = new Orientation(1, -1, Direction.SOUTH, Direction.WEST);
    public static final Orientation NP = new Orientation(-1, 1, Direction.NORTH, Direction.EAST);

    public static final int DEFAULT_META = -1;
    public static final int VINE_FRONT_META = 0;
    public static final int VINE_SIDE_META = 1;

    private static final int TOP = 2;
    private static final int MIDDLE = 1;
    private static final int BASE = 0;
    private static final int SUB = -1;
    private static final int SUB_SUB = -2;

    private static final int NO_GRAB = 0;
    private static final int HALF_GRAB = 1;
    private static final int AROUND_GRAB = 2;

    /** 원작 move.climb.free.direction.orthogonal.angle / diagonal.angle 기본값 */
    private static final float ORTHOGONAL_DIRECTION_ANGLE = 90F;
    private static final float DIAGONAL_DIRECTION_ANGLE = 80F;

    /** 원작 _handClimbingHoldGap = min(0.25, 0.06 * max(위/아래 속도 배율 = 1)) */
    private static final double HAND_CLIMBING_HOLD_GAP = 0.06;

    final int i;
    final int k;
    private final boolean isDiagonal;
    private final Set<Direction> facings;
    private final Direction facing;
    private float directionAngle;
    private float minimumClimbingAngle;
    private float maximumClimbingAngle;

    private Orientation(int i, int k, Direction... facings) {
        this.i = i;
        this.k = k;
        this.isDiagonal = i != 0 && k != 0;
        setClimbingAngles();

        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        Collections.addAll(set, facings);
        this.facings = set;

        if (facings.length == 1) {
            this.facing = facings[0];
            ORTHOGONALS.add(this);
            FACING_TO_ORIENTATION.put(this.facing, this);
        } else {
            this.facing = null;
            if (facings.length > 1) DIAGONALS.add(this);
        }
    }

    public static List<Orientation> orthogonals() {
        return ORTHOGONALS;
    }

    public static List<Orientation> diagonals() {
        return DIAGONALS;
    }

    // ------------------------------------------------------------------
    // 회전
    // ------------------------------------------------------------------

    public Orientation rotate(int angle) {
        if (this == ZZ) throw new IllegalStateException("unrotatable orientation");
        switch (angle) {
            case 0:
                return this;
            case 45:
                if (this == PZ) return PP;
                if (this == PP) return ZP;
                if (this == ZP) return NP;
                if (this == NP) return NZ;
                if (this == NZ) return NN;
                if (this == NN) return ZN;
                if (this == ZN) return PN;
                if (this == PN) return PZ;
                break;
            case -45:
                if (this == PZ) return PN;
                if (this == PN) return ZN;
                if (this == ZN) return NN;
                if (this == NN) return NZ;
                if (this == NZ) return NP;
                if (this == NP) return ZP;
                if (this == ZP) return PP;
                if (this == PP) return PZ;
                break;
            case 90:
                return rotate(45).rotate(45);
            case -90:
                return rotate(-45).rotate(-45);
            case 135:
                return rotate(180).rotate(-45);
            case -135:
                return rotate(-180).rotate(45);
            case 180:
            case -180:
                if (this == PZ) return NZ;
                if (this == PN) return NP;
                if (this == ZN) return ZP;
                if (this == NN) return PP;
                if (this == NZ) return PZ;
                if (this == NP) return PN;
                if (this == ZP) return ZN;
                if (this == PP) return NN;
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("angle " + angle + " not supported");
    }

    // ------------------------------------------------------------------
    // 플레이어가 바라보는 방향
    // ------------------------------------------------------------------

    /** yaw 를 0~360 으로 정규화 (원작 rotationYaw % 360) */
    public static float normalizeYaw(float yaw) {
        float rotation = yaw % 360F;
        if (rotation < 0) rotation += 360F;
        return rotation;
    }

    public boolean isRotationForClimbing(float rotation) {
        return isWithinAngle(rotation, minimumClimbingAngle, maximumClimbingAngle);
    }

    private void setClimbingAngles() {
        switch (i) {
            case -1 -> {
                switch (k) {
                    case -1 -> setClimbingAngles(135F); // NN
                    case 0 -> setClimbingAngles(90F); // NZ
                    case 1 -> setClimbingAngles(45F); // NP
                    default -> { }
                }
            }
            case 0 -> {
                switch (k) {
                    case -1 -> setClimbingAngles(180F); // ZN
                    case 0 -> setClimbingAngles(0F, 360F); // ZZ
                    case 1 -> setClimbingAngles(0F); // ZP
                    default -> { }
                }
            }
            case 1 -> {
                switch (k) {
                    case -1 -> setClimbingAngles(225F); // PN
                    case 0 -> setClimbingAngles(270F); // PZ
                    case 1 -> setClimbingAngles(315F); // PP
                    default -> { }
                }
            }
            default -> { }
        }
    }

    private void setClimbingAngles(float directionAngle) {
        this.directionAngle = directionAngle;
        float halfAreaAngle = (isDiagonal ? DIAGONAL_DIRECTION_ANGLE : ORTHOGONAL_DIRECTION_ANGLE) / 2F;
        setClimbingAngles(directionAngle - halfAreaAngle, directionAngle + halfAreaAngle);
    }

    private void setClimbingAngles(float minimum, float maximum) {
        if (minimum < 0F) minimum += 360F;
        if (maximum > 360F) maximum -= 360F;
        this.minimumClimbingAngle = minimum;
        this.maximumClimbingAngle = maximum;
    }

    private static boolean isWithinAngle(float rotation, float minimumRotation, float maximumRotation) {
        if (minimumRotation > maximumRotation) {
            return rotation >= minimumRotation || rotation <= maximumRotation;
        }
        return rotation >= minimumRotation && rotation <= maximumRotation;
    }

    /** 원작 getHorizontalBorderGap: 이 방향 벽까지 칸 안에서 남은 거리 */
    public double getHorizontalBorderGap(double x, double z) {
        if (this == NZ) return frac(x);
        if (this == PZ) return 1 - frac(x);
        if (this == ZN) return frac(z);
        if (this == ZP) return 1 - frac(z);
        return 0D;
    }

    private double getHorizontalBorderGap() {
        return getHorizontalBorderGap(baseId, baseKd);
    }

    private static double frac(double d) {
        return d - Math.floor(d);
    }

    // ------------------------------------------------------------------
    // 공개 진입점
    // ------------------------------------------------------------------

    /**
     * 원작 seekClimbGap. 이 방향이 바라보는 각도 안에 있으면 손/발이 잡을 곳을 찾아 결과를 갱신한다.
     *
     * @param jhd 발 높이 * 2 + 1 (반 블록 단위)
     */
    public void seekClimbGap(float rotation, World w, int bi, double id, double jhd, int bk, double kd,
                             boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling,
                             HandsClimbing[] inoutHandsClimbing, FeetClimbing[] inoutFeetClimbing,
                             ClimbGap outHandsClimbGap, ClimbGap outFeetClimbGap) {
        if (!isRotationForClimbing(rotation)) return;
        initialize(w, bi, id, jhd, bk, kd);
        inoutHandsClimbing[0] = inoutHandsClimbing[0].max(
                handsClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, CLIMB_GAP_OUTER_TEMP),
                outHandsClimbGap, CLIMB_GAP_OUTER_TEMP);
        inoutFeetClimbing[0] = inoutFeetClimbing[0].max(
                feetClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, CLIMB_GAP_OUTER_TEMP),
                outFeetClimbGap, CLIMB_GAP_OUTER_TEMP);
    }

    public static boolean isLadder(BlockState state) {
        return state.getBlock() instanceof LadderBlock;
    }

    public static boolean isVine(BlockState state) {
        return state.getBlock() instanceof VineBlock;
    }

    public static boolean isLadderOrVine(BlockState state) {
        return isLadder(state) || isVine(state);
    }

    public static boolean isClimbable(World w, int x, int y, int z) {
        return w.getBlockState(new BlockPos(x, y, z)).isIn(BlockTags.CLIMBABLE);
    }

    public static Orientation getKnownLadderOrientation(World w, int x, int y, int z) {
        BlockState state = w.getBlockState(new BlockPos(x, y, z));
        if (!isLadder(state)) return null;
        return FACING_TO_ORIENTATION.get(state.get(LadderBlock.FACING));
    }

    // ------------------------------------------------------------------
    // 손 / 발
    // ------------------------------------------------------------------

    private HandsClimbing handsClimbing(boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling, ClimbGap outClimbGap) {
        outClimbGap.reset();
        CLIMB_GAP_TEMP.reset();

        initializeOffset(3D, isClimbCrawling, isCrawlClimbing, isCrawling);

        HandsClimbing result = HandsClimbing.NONE;
        int gap;

        if ((gap = isLadderSubstitute(MIDDLE, CLIMB_GAP_TEMP)) > 0) {
            if (jhOffset > 1D - HAND_CLIMBING_HOLD_GAP)
                result = result.max(HandsClimbing.UP, outClimbGap, CLIMB_GAP_TEMP);
            else
                result = result.max(HandsClimbing.NONE, outClimbGap, CLIMB_GAP_TEMP); // 손이 닿지 않음 (위)
        }

        if ((gap = isLadderSubstitute(BASE, CLIMB_GAP_TEMP)) > 0) {
            if (jhOffset < HAND_CLIMBING_HOLD_GAP)
                result = result.max(HandsClimbing.BOTTOM_HOLD, outClimbGap, CLIMB_GAP_TEMP);
            else
                result = result.max(HandsClimbing.UP, outClimbGap, CLIMB_GAP_TEMP);
        }

        CLIMB_GAP_TEMP.skipGaps = isClimbCrawling || isCrawlClimbing;

        if ((gap = isLadderSubstitute(SUB, CLIMB_GAP_TEMP)) > 0 && !(isCrawling && gap > 1)) {
            if (!isClimbCrawling && gap > 2) {
                result = result.max(HandsClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP); // 윗몸을 틈으로 끌어올림
            } else if (isClimbCrawling && gap > 1) {
                result = result.max(HandsClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP); // 위 틈으로 기어 들어감
            } else if (jhOffset < HAND_CLIMBING_HOLD_GAP) {
                result = result.max(grabType == AROUND_GRAB ? HandsClimbing.UP : HandsClimbing.TOP_HOLD, outClimbGap, CLIMB_GAP_TEMP);
            } else {
                result = result.max(grabType == AROUND_GRAB ? HandsClimbing.TOP_HOLD : HandsClimbing.SINK, outClimbGap, CLIMB_GAP_TEMP);
            }
        }

        if ((gap = isLadderSubstitute(SUB_SUB, CLIMB_GAP_TEMP)) > 0 && !isCrawling) {
            if ((gap > 2 && !isCrawlClimbing) || grabType == AROUND_GRAB || (gap > 1 && isClimbCrawling)) { // 손이 닿지 않음 (아래)
                if (jhOffset < HAND_CLIMBING_HOLD_GAP && !isClimbCrawling)
                    result = result.max(HandsClimbing.TOP_HOLD, outClimbGap, CLIMB_GAP_TEMP);
                else if (isClimbCrawling)
                    result = result.max(HandsClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP);
                else
                    result = result.max(HandsClimbing.SINK, outClimbGap, CLIMB_GAP_TEMP);
            }
        }

        return result;
    }

    private FeetClimbing feetClimbing(boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling, ClimbGap outClimbGap) {
        outClimbGap.reset();
        CLIMB_GAP_TEMP.reset();

        initializeOffset(0D, isClimbCrawling, isCrawlClimbing, isCrawling);
        FeetClimbing result = FeetClimbing.NONE;
        int gap;

        if (isLadderSubstitute(TOP, CLIMB_GAP_TEMP) > 0) // 발이 닿지 않음 (위)
            result = result.max(FeetClimbing.NONE, outClimbGap, CLIMB_GAP_TEMP);

        CLIMB_GAP_TEMP.skipGaps = isClimbCrawling || isCrawlClimbing;

        if ((gap = isLadderSubstitute(MIDDLE, CLIMB_GAP_TEMP)) > 0 && !isCrawling) {
            if (gap > 3 && !isClimbCrawling) {
                result = result.max(isCrawlClimbing ? FeetClimbing.NONE : FeetClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP);
            } else if ((isClimbCrawling || isCrawlClimbing) && gap > 1) {
                result = result.max(isCrawlClimbing ? FeetClimbing.BASE_WITH_HANDS : FeetClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP);
            } else if (gap > 2) {
                result = result.max(isClimbCrawling ? FeetClimbing.NONE : FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS, outClimbGap, CLIMB_GAP_TEMP);
            } else {
                result = result.max(FeetClimbing.TOP_WITH_HANDS, outClimbGap, CLIMB_GAP_TEMP); // 손으로만
            }
        }

        if ((gap = isLadderSubstitute(BASE, CLIMB_GAP_TEMP)) > 0) {
            if (gap > 3 && !isCrawling && !isCrawlClimbing) {
                result = result.max(FeetClimbing.FAST_UP, outClimbGap, CLIMB_GAP_TEMP); // 큰 틈으로 몸 전체를 밀어 올림
            } else if (gap > 2 && !isCrawling) {
                if (!isClimbCrawling) {
                    result = result.max(jhOffset < HAND_CLIMBING_HOLD_GAP
                            ? FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS
                            : FeetClimbing.SLOW_UP_WITH_SINK_WITHOUT_HANDS, outClimbGap, CLIMB_GAP_TEMP);
                } else {
                    result = result.max(FeetClimbing.NONE, outClimbGap, CLIMB_GAP_TEMP);
                }
            } else if (jhOffset < 1D - HAND_CLIMBING_HOLD_GAP) {
                result = result.max(FeetClimbing.BASE_WITH_HANDS, outClimbGap, CLIMB_GAP_TEMP);
            } else {
                result = result.max(FeetClimbing.BASE_HOLD, outClimbGap, CLIMB_GAP_TEMP);
            }
        }

        if (isLadderSubstitute(SUB, CLIMB_GAP_TEMP) > 0) // 발이 닿지 않음 (아래)
            result = result.max(FeetClimbing.NONE, outClimbGap, CLIMB_GAP_TEMP);

        if (isCrawlClimbing || isCrawling)
            result = result.max(FeetClimbing.BASE_WITH_HANDS, outClimbGap, CLIMB_GAP_TEMP);

        return result;
    }

    /** 원작 isLadderSubstitute(int, ClimbGap): 잡을 곳이 있으면 그 위 틈의 크기(1~5)를, 없으면 0 */
    private int isLadderSubstitute(int localOffset, ClimbGap outClimbGap) {
        initializeLocal(localOffset);

        int gap;
        if (localHalf == 1) {
            if (hasHalfHold()) {
                if (!grabRemote) {
                    boolean overLadder = isOnLadderOrVine(0) || isOnOpenTrapDoor(0);
                    boolean overOverLadder = isOnLadderOrVine(1) || isOnOpenTrapDoor(1);
                    boolean overAccessible = isBaseAccessible(1, false, true);
                    boolean overFullAccessible = overAccessible && isFullAccessible(1, grabRemote);
                    boolean overOverFullAccessible = overAccessible && isFullExtentAccessible(2, grabRemote);

                    if (overLadder) gap = 1;
                    else if (overAccessible) {
                        if (overFullAccessible) gap = overOverFullAccessible ? 5 : (crawl ? 3 : 5);
                        else if (overOverLadder) gap = 5;
                        else gap = 1;
                    } else gap = 1;
                } else if (isBaseAccessible(0)) {
                    if (isUpperHalfFrontEmpty(remoteI, 0, remoteK)) {
                        if (isFullAccessible(1, grabRemote)) {
                            if (isFullExtentAccessible(2, grabRemote)) gap = 5;
                            else if (isJustLowerHalfExtentAccessible(2)) gap = 4;
                            else gap = 3;
                        } else if (isLowerHalfAccessible(1, grabRemote)) gap = 2;
                        else gap = 1;
                    } else gap = 1;
                } else gap = 0;
            } else gap = 0;
        } else {
            if (hasBottomHold()) {
                if (!grabRemote) {
                    boolean overLadder = isOnLadderOrVine(0) || isOnOpenTrapDoor(0);
                    boolean overOverLadder = isOnLadderOrVine(1) || isOnOpenTrapDoor(1);
                    boolean overAccessible = isBaseAccessible(0, false, true);
                    boolean overOverAccessible = isBaseAccessible(1, false, true);
                    boolean overFullAccessible = overAccessible && isFullAccessible(0, grabRemote);
                    boolean overOverFullAccessible = overAccessible && isFullExtentAccessible(1, grabRemote);

                    if (overLadder) gap = 1;
                    else if (overAccessible) {
                        if (overFullAccessible) {
                            if (overOverAccessible) gap = overOverFullAccessible ? 4 : (crawl ? 2 : 4);
                            else gap = 2;
                        } else if (overOverLadder) gap = 2;
                        else gap = 1;
                    } else gap = 1;
                } else if (isBaseAccessible(0)) {
                    if (isFullAccessible(0, grabRemote)) gap = isFullExtentAccessible(1, grabRemote) ? 4 : 2;
                    else gap = 1;
                } else gap = 0;
            } else gap = 0;
        }

        if (outClimbGap != null && gap > 0) {
            outClimbGap.block = grabBlock;
            outClimbGap.meta = grabMeta;
            outClimbGap.canStand = gap > 3;
            outClimbGap.mustCrawl = gap > 1 && gap < 4;
            outClimbGap.direction = this;
        }
        return gap;
    }

    // ------------------------------------------------------------------
    // 잡을 곳 찾기
    // ------------------------------------------------------------------

    private boolean hasHalfHold() {
        if (isOnLadder(0) && isOnLadderFront(0))
            return setHalfGrabType(AROUND_GRAB, getBaseBlock(0), false);

        if (remoteLadderClimbing(0))
            return setHalfGrabType(AROUND_GRAB, getRemoteBlock(0), true);

        BlockState remoteState = getRemoteBlockState(0);
        if (isEmpty(baseI, 0, baseK)) {
            if (isIronBars(remoteState) && headedToFrontWall(remoteI, 0, remoteK, remoteState))
                return setHalfGrabType(HALF_GRAB, remoteState);
        }

        BlockState wallState = getWallBlockState(baseI, 0, baseK);
        if (wallState != null && isIronBars(wallState) && headedToBaseWall(0, wallState))
            return setHalfGrabType(HALF_GRAB, wallState, false);

        // 울타리 넘기 (원작 move.climb.free.fence 기본값 켜짐)
        if (isFence(remoteState) && headedToFrontWall(remoteI, 0, remoteK, remoteState)) {
            if (!isFence(getBaseBlockState(0)))
                return setHalfGrabType(HALF_GRAB, remoteState);
            else if (headedToFrontSideWall(remoteI, 0, remoteK, remoteState))
                return setHalfGrabType(HALF_GRAB, remoteState);
        }

        BlockState remoteBelowState = getRemoteBlockState(-1);
        if (isFence(remoteBelowState) && headedToFrontWall(remoteI, -1, remoteK, remoteBelowState)) {
            if (!isFence(getBaseBlockState(-1)))
                return setHalfGrabType(HALF_GRAB, remoteState);
            else if (headedToFrontSideWall(remoteI, -1, remoteK, remoteBelowState))
                return setHalfGrabType(HALF_GRAB, remoteState);
        }

        if (isFence(wallState) && headedToBaseWall(0, wallState))
            return setHalfGrabType(HALF_GRAB, wallState, false);

        BlockState belowWallState = getWallBlockState(baseI, -1, baseK);
        if (isFence(belowWallState) && headedToBaseWall(-1, belowWallState))
            return setHalfGrabType(HALF_GRAB, belowWallState, false);

        if (isStoneWall(remoteState) && !headedToRemoteFlatWall(remoteState, 0))
            return setHalfGrabType(HALF_GRAB, remoteState);

        if (isStoneWall(remoteBelowState) && !headedToRemoteFlatWall(remoteBelowState, -1))
            return setHalfGrabType(HALF_GRAB, remoteBelowState);

        if (isBottomHalfBlock(remoteState)
                || (isStairCompact(remoteState) && isBottomStairCompactNotBack(remoteState)
                && !(isStairCompact(getBaseBlockState(-1)) && isBottomStairCompactFront(getBaseBlockState(-1)))))
            return setHalfGrabType(HALF_GRAB, remoteState);

        if (isTrapDoor(remoteState) && isClosedTrapDoor(remoteState))
            return setHalfGrabType(HALF_GRAB, remoteState);

        BlockState baseState = getBaseBlockState(0);
        if (isTrapDoor(baseState) && !isClosedTrapDoor(baseState))
            return setHalfGrabType(HALF_GRAB, baseState, false);

        int meta = baseVineClimbing(0);
        if (meta > -1)
            return setHalfGrabType(HALF_GRAB, getBaseBlock(0), false, meta);
        meta = remoteVineClimbing(0);
        if (meta > -1)
            return setHalfGrabType(HALF_GRAB, getRemoteBlock(0), false, meta);

        return setHalfGrabType(NO_GRAB, (Block) null, true);
    }

    private boolean hasBottomHold() {
        if (isOnLadder(-1) && isOnLadderFront(-1))
            return setBottomGrabType(AROUND_GRAB, getBaseBlock(-1), false);

        if (isOnLadder(0) && isOnLadderFront(0))
            return setBottomGrabType(AROUND_GRAB, getBaseBlock(0), false);

        if (remoteLadderClimbing(-1))
            return setBottomGrabType(AROUND_GRAB, getRemoteBlock(-1), true);

        if (remoteLadderClimbing(0))
            return setBottomGrabType(AROUND_GRAB, getRemoteBlock(0), true);

        BlockState remoteState = getRemoteBlockState(0);
        BlockState remoteBelowState = getRemoteBlockState(-1);
        boolean remoteLowerHalfEmpty = isLowerHalfFrontFullEmpty(remoteI, 0, remoteK);

        if (isEmpty(baseI, -1, baseK)) {
            if (isIronBars(remoteBelowState) && headedToFrontWall(remoteI, -1, remoteK, remoteBelowState))
                return setBottomGrabType(HALF_GRAB, remoteBelowState);
        }

        BlockState baseBelowStateForFence = getBaseBlockState(-1);
        if (isFence(remoteBelowState) && headedToFrontWall(remoteI, -1, remoteK, remoteBelowState)) {
            if (!isFence(baseBelowStateForFence))
                return setBottomGrabType(HALF_GRAB, remoteBelowState);
            else if (headedToFrontSideWall(remoteI, -1, remoteK, remoteBelowState))
                return setBottomGrabType(HALF_GRAB, remoteBelowState);
        }

        if (isStoneWall(remoteBelowState) && !headedToRemoteFlatWall(remoteBelowState, -1))
            return setHalfGrabType(HALF_GRAB, remoteBelowState);

        if (isStoneWall(remoteState) && !headedToRemoteFlatWall(remoteState, 0))
            return setHalfGrabType(HALF_GRAB, remoteState);

        BlockState belowWallState = getWallBlockState(baseI, -1, baseK);
        if (belowWallState != null) {
            if (isEmpty(baseI - i, 0, baseK - k) && isEmpty(baseI - i, -1, baseK - k)) {
                if (isIronBars(belowWallState) && headedToBaseWall(-1, belowWallState))
                    return setBottomGrabType(HALF_GRAB, belowWallState, false);

                if (headedToBaseGrabWall(-1, belowWallState))
                    return setBottomGrabType(HALF_GRAB, belowWallState, false);
            }

            if (isFence(belowWallState) && headedToBaseWall(-1, belowWallState))
                return setBottomGrabType(HALF_GRAB, belowWallState, false);

            return false;
        }

        // 가장 흔한 경우: 앞 칸 아래 블록의 윗모서리 (그 위 반 칸이 비어 있음)
        if (remoteLowerHalfEmpty && isBaseAccessible(-1, true, false)
                && isUpperHalfFrontAnySolid(remoteI, -1, remoteK)
                && !isBottomHalfBlock(remoteBelowState)
                && (!isStairCompact(remoteBelowState) || !isBottomStairCompactFront(remoteBelowState))
                && (!isDoor(remoteBelowState) || isDoorTop(remoteBelowState))
                && (!isDoor(getBaseBlockState(0)) || !isDoorFrontBlocked(baseI, 0, baseK)))
            return setBottomGrabType(HALF_GRAB, remoteBelowState);

        if (isStairCompact(remoteState)) {
            if (isTopStairCompact(remoteState) && !isTopStairCompactBack(remoteState) && isUpperHalfFrontFullSolid(remoteI, -1, remoteK))
                return setBottomGrabType(HALF_GRAB, remoteBelowState);
        }

        BlockState baseBelowState = getBaseBlockState(-1);

        // 열린 다락문 윗부분
        if (isTrapDoor(baseBelowState) && !isClosedTrapDoor(baseBelowState))
            return setBottomGrabType(HALF_GRAB, baseBelowState, false);

        if (isDoor(baseBelowState) && isDoorTop(baseBelowState) && isDoorFrontBlocked(baseI, -1, baseK) && isBaseAccessible(0))
            return setBottomGrabType(HALF_GRAB, baseBelowState, false);

        int meta = baseVineClimbing(-1);
        if (meta != DEFAULT_META)
            return setHalfGrabType(HALF_GRAB, getBaseBlock(-1), false, meta);
        meta = baseVineClimbing(0);
        if (meta != DEFAULT_META)
            return setHalfGrabType(HALF_GRAB, getBaseBlock(0), false, meta);
        meta = remoteVineClimbing(-1);
        if (meta != DEFAULT_META)
            return setHalfGrabType(HALF_GRAB, getRemoteBlock(-1), false, meta);
        meta = remoteVineClimbing(0);
        if (meta != DEFAULT_META)
            return setHalfGrabType(HALF_GRAB, getRemoteBlock(0), false, meta);

        return setBottomGrabType(NO_GRAB, (Block) null, true);
    }

    private boolean setHalfGrabType(int type, BlockState state) {
        return setHalfGrabType(type, state.getBlock(), true);
    }

    private boolean setHalfGrabType(int type, BlockState state, boolean remote) {
        return setHalfGrabType(type, state.getBlock(), remote);
    }

    private boolean setHalfGrabType(int type, Block block, boolean remote) {
        return setHalfGrabType(type, block, remote, -1);
    }

    private boolean setHalfGrabType(int type, Block block, boolean remote, int metaClimb) {
        boolean hasGrab = type != NO_GRAB;
        if (hasGrab && remote && isDiagonal) {
            boolean edgeConnectCCW = rotate(90).isUpperHalfFrontEmpty(baseI, 0, remoteK);
            boolean edgeConnectCW = rotate(-90).isUpperHalfFrontEmpty(remoteI, 0, baseK);
            hasGrab = edgeConnectCCW && edgeConnectCW;
        }
        return setGrabType(type, block, remote, hasGrab, metaClimb);
    }

    private boolean setBottomGrabType(int type, BlockState state) {
        return setBottomGrabType(type, state.getBlock(), true);
    }

    private boolean setBottomGrabType(int type, BlockState state, boolean remote) {
        return setBottomGrabType(type, state.getBlock(), remote);
    }

    private boolean setBottomGrabType(int type, Block block, boolean remote) {
        boolean hasGrab = type != NO_GRAB;
        if (hasGrab && remote && isDiagonal) {
            boolean edgeConnectCCW = rotate(90).isLowerHalfFrontFullEmpty(baseI, 0, remoteK);
            boolean edgeConnectCW = rotate(-90).isLowerHalfFrontFullEmpty(remoteI, 0, baseK);
            hasGrab = edgeConnectCCW && edgeConnectCW;
        }
        return setGrabType(type, block, remote, hasGrab, -1);
    }

    private static boolean setGrabType(int type, Block block, boolean remote, boolean hasGrab, int metaClimb) {
        grabRemote = remote;
        grabType = hasGrab ? type : NO_GRAB;
        grabBlock = block;
        grabMeta = metaClimb;
        return hasGrab;
    }

    // ------------------------------------------------------------------
    // 사다리 / 덩굴
    // ------------------------------------------------------------------

    private int baseVineClimbing(int jOffset) {
        if (isOnVine(jOffset)) {
            if (isOnVineFront(jOffset)) return VINE_FRONT_META;
            if (baseVineClimbing(jOffset, PZ) || baseVineClimbing(jOffset, NZ)
                    || baseVineClimbing(jOffset, ZP) || baseVineClimbing(jOffset, ZN))
                return VINE_SIDE_META;
        }
        return DEFAULT_META;
    }

    private boolean baseVineClimbing(int jOffset, Orientation orientation) {
        if (orientation == this) return false;
        return orientation.rotate(180).hasVineOrientation(baseI, localOffset + jOffset, baseK)
                && orientation.getHorizontalBorderGap() >= 0.65;
    }

    private boolean remoteLadderClimbing(int jOffset) {
        return isBehindLadder(jOffset) && isOnLadderBack(jOffset);
    }

    private int remoteVineClimbing(int jOffset) {
        if (isBehindVine(jOffset) && isOnVineBack(jOffset)) return VINE_FRONT_META;
        if (remoteVineClimbing(jOffset, PZ) || remoteVineClimbing(jOffset, NZ)
                || remoteVineClimbing(jOffset, ZP) || remoteVineClimbing(jOffset, ZN))
            return VINE_SIDE_META;
        return DEFAULT_META;
    }

    private boolean remoteVineClimbing(int jOffset, Orientation orientation) {
        if (orientation == this) return false;
        int vi = baseI - orientation.i;
        int vk = baseK - orientation.k;
        return isVine(getBlockState(vi, jOffset, vk))
                && orientation.hasVineOrientation(vi, localOffset + jOffset, vk)
                && orientation.getHorizontalBorderGap() >= 0.65F;
    }

    private static boolean isOnLadder(int jOffset) {
        BlockState state = getBaseBlockState(jOffset);
        if (isLadder(state)) return true;
        if (isVine(state)) return false;
        return state.isIn(BlockTags.CLIMBABLE) && !(state.getBlock() instanceof ScaffoldingBlock);
    }

    private static boolean isBehindLadder(int jOffset) {
        BlockState state = getRemoteBlockState(jOffset);
        if (isLadder(state)) return true;
        if (isVine(state)) return false;
        return state.isIn(BlockTags.CLIMBABLE) && !(state.getBlock() instanceof ScaffoldingBlock);
    }

    private static boolean isOnVine(int jOffset) {
        return isVine(getBaseBlockState(jOffset));
    }

    private static boolean isBehindVine(int jOffset) {
        return isVine(getRemoteBlockState(jOffset));
    }

    private static boolean isOnLadderOrVine(int jOffset) {
        return isLadderOrVine(getBaseBlockState(jOffset)) || grabBlock instanceof VineBlock;
    }

    private boolean isOnLadderFront(int jOffset) {
        return hasLadderOrientation(baseI, jOffset, baseK);
    }

    private boolean isOnLadderBack(int jOffset) {
        return rotate(180).hasLadderOrientation(remoteI, jOffset, remoteK);
    }

    private boolean isOnVineFront(int jOffset) {
        return hasVineOrientation(baseI, localOffset + jOffset, baseK);
    }

    private boolean isOnVineBack(int jOffset) {
        return rotate(180).hasVineOrientation(remoteI, localOffset + jOffset, remoteK);
    }

    private boolean hasVineOrientation(int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        if (!isVine(state)) return false;
        if (this == NZ) return state.get(VineBlock.EAST);
        if (this == PZ) return state.get(VineBlock.WEST);
        if (this == ZP) return state.get(VineBlock.NORTH);
        if (this == ZN) return state.get(VineBlock.SOUTH);
        return false;
    }

    private boolean hasLadderOrientation(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        if (!isLadder(state)) {
            // 사다리 태그만 있는 블록(예: 다른 모드)은 방향을 모르므로 앞이라고 본다
            return state.isIn(BlockTags.CLIMBABLE) && !isVine(state) && !(state.getBlock() instanceof ScaffoldingBlock);
        }
        return state.get(LadderBlock.FACING) == this.facing;
    }

    private static boolean isOnOpenTrapDoor(int jOffset) {
        BlockState state = getBaseBlockState(jOffset);
        return isTrapDoor(state) && !isClosedTrapDoor(state);
    }

    private boolean isTrapDoorFront(BlockState state) {
        return facings.contains(state.get(TrapdoorBlock.FACING));
    }

    // ------------------------------------------------------------------
    // 계단
    // ------------------------------------------------------------------

    private boolean isBottomStairCompactNotBack(BlockState state) {
        return !isTopStairCompact(state) && !isStairCompactBack(state);
    }

    private boolean isBottomStairCompactFront(BlockState state) {
        return !isTopStairCompact(state) && isStairCompactFront(state);
    }

    private boolean isTopStairCompactFront(BlockState state) {
        return isTopStairCompact(state) && isStairCompactFront(state);
    }

    private boolean isTopStairCompactBack(BlockState state) {
        return isTopStairCompact(state) && isStairCompactBack(state);
    }

    private boolean isStairCompactFront(BlockState state) {
        Direction f = state.get(StairsBlock.FACING);
        StairShape s = state.get(StairsBlock.SHAPE);
        boolean north = f == Direction.NORTH, south = f == Direction.SOUTH, east = f == Direction.EAST, west = f == Direction.WEST;
        boolean ol = s == StairShape.OUTER_LEFT, or = s == StairShape.OUTER_RIGHT, il = s == StairShape.INNER_LEFT,
                ir = s == StairShape.INNER_RIGHT, st = s == StairShape.STRAIGHT;

        if (this == NZ) return (north && ol) || (south && or) || (west && (st || or || ol));
        if (this == PZ) return (north && or) || (south && ol) || (east && (st || or || ol));
        if (this == ZP) return (east && or) || (west && ol) || (south && (st || or || ol));
        if (this == ZN) return (east && ol) || (west && or) || (north && (st || or || ol));
        if (this == PN) return (south && ol) || (west && or) || (east && !ir) || (north && !il);
        if (this == PP) return (north && or) || (west && ol) || (east && !il) || (south && !ir);
        if (this == NN) return (east && ol) || (south && or) || (north && !ir) || (west && !il);
        if (this == NP) return (east && or) || (north && ol) || (south && !il) || (west && !ir);
        return false;
    }

    private boolean isStairCompactBack(BlockState state) {
        Direction f = state.get(StairsBlock.FACING);
        StairShape s = state.get(StairsBlock.SHAPE);
        boolean north = f == Direction.NORTH, south = f == Direction.SOUTH, east = f == Direction.EAST, west = f == Direction.WEST;
        boolean ol = s == StairShape.OUTER_LEFT, or = s == StairShape.OUTER_RIGHT, il = s == StairShape.INNER_LEFT,
                ir = s == StairShape.INNER_RIGHT, st = s == StairShape.STRAIGHT;

        if (this == NZ) return (north && ir) || (south && il) || (east && (st || il || ir));
        if (this == PZ) return (north && il) || (south && ir) || (west && (st || il || ir));
        if (this == ZP) return (east && il) || (west && ir) || (north && (st || il || ir));
        if (this == ZN) return (east && ir) || (west && il) || (south && (st || il || ir));
        if (this == PN) return (east && ir) || (north && il) || (south && !ol) || (west && !or);
        if (this == PP) return (east && il) || (south && ir) || (north && !or) || (west && !ol);
        if (this == NN) return (north && ir) || (west && il) || (east && !ol) || (south && !or);
        if (this == NP) return (south && il) || (west && ir) || (east && !or) || (north && !ol);
        return false;
    }

    private static boolean isTopStairCompact(BlockState state) {
        return state.get(StairsBlock.HALF) == BlockHalf.TOP;
    }

    // ------------------------------------------------------------------
    // 울타리 / 벽 / 판유리
    // ------------------------------------------------------------------

    private boolean isFenceGateFront(BlockState state) {
        Orientation orientation = FACING_TO_ORIENTATION.get(state.get(FenceGateBlock.FACING));
        return orientation != null && (this == orientation.rotate(90) || this == orientation.rotate(-90));
    }

    private boolean headedToFrontWall(int x, int jOffset, int z, BlockState state) {
        boolean zn = getWallFlag(ZN, state);
        boolean zp = getWallFlag(ZP, state);
        boolean nz = getWallFlag(NZ, state);
        boolean pz = getWallFlag(PZ, state);
        if (getAllWallsOnNoWall(state) && !zn && !zp && !nz && !pz) zn = zp = nz = pz = true;

        return headedToWall(NZ, pz) || headedToWall(PZ, nz) || headedToWall(ZN, zp) || headedToWall(ZP, zn);
    }

    private boolean headedToFrontSideWall(int x, int jOffset, int z, BlockState state) {
        boolean zn = getWallFlag(ZN, state);
        boolean zp = getWallFlag(ZP, state);
        boolean nz = getWallFlag(NZ, state);
        boolean pz = getWallFlag(PZ, state);
        if (getAllWallsOnNoWall(state) && !zn && !zp && !nz && !pz) zn = zp = nz = pz = true;

        boolean iTop = isTopHalf(baseId);
        boolean kTop = isTopHalf(baseKd);
        if (iTop) {
            if (kTop) return headedToWall(NZ, zp) || headedToWall(PZ, zp) || headedToWall(ZN, pz) || headedToWall(ZP, pz);
            return headedToWall(NZ, zn) || headedToWall(PZ, zn) || headedToWall(ZN, pz) || headedToWall(ZP, pz);
        }
        if (kTop) return headedToWall(NZ, zp) || headedToWall(PZ, zp) || headedToWall(ZN, nz) || headedToWall(ZP, nz);
        return headedToWall(NZ, zn) || headedToWall(PZ, zn) || headedToWall(ZN, nz) || headedToWall(ZP, nz);
    }

    private boolean headedToWall(Orientation base, boolean result) {
        if (this == base || this == base.rotate(45) || this == base.rotate(-45)) return result;
        return false;
    }

    private boolean headedToBaseWall(int jOffset, BlockState state) {
        boolean zn = getWallFlag(ZN, state);
        boolean zp = getWallFlag(ZP, state);
        boolean nz = getWallFlag(NZ, state);
        boolean pz = getWallFlag(PZ, state);
        boolean allOnNone = getAllWallsOnNoWall(state);
        if (allOnNone && !zn && !zp && !nz && !pz) zn = zp = nz = pz = true;

        boolean leaf = zn || zp || nz || pz;
        boolean coreOnly = !allOnNone && !leaf;

        boolean iTop = isTopHalf(baseId);
        boolean kTop = isTopHalf(baseKd);
        if (iTop) {
            if (kTop) return headedToBaseWall(NN, NZ, ZN, zp, nz, pz, zn, coreOnly, leaf);
            return headedToBaseWall(NP, NZ, ZP, zn, nz, pz, zp, coreOnly, leaf);
        }
        if (kTop) return headedToBaseWall(PN, PZ, ZN, zp, pz, nz, zn, coreOnly, leaf);
        return headedToBaseWall(PP, PZ, ZP, zn, pz, nz, zp, coreOnly, leaf);
    }

    private boolean headedToBaseWall(Orientation diagonal, Orientation left, Orientation right,
                                     boolean leftFront, boolean rightFrontOpposite, boolean rightFront,
                                     boolean leftFrontOpposite, boolean co, boolean leaf) {
        if (this == diagonal) return leaf || co;
        if (this == left) return headedToBaseWall(leftFront, rightFrontOpposite, rightFront, leftFrontOpposite, co);
        if (this == right) return headedToBaseWall(rightFront, leftFrontOpposite, leftFront, rightFrontOpposite, co);
        return false;
    }

    private static boolean headedToBaseWall(boolean front, boolean sideOpposite, boolean side, boolean frontOpposite, boolean coreOnly) {
        return front || sideOpposite && !side || frontOpposite && !front && !side || coreOnly;
    }

    private boolean headedToBaseGrabWall(int jOffset, BlockState state) {
        boolean zn = getWallFlag(ZN, state);
        boolean zp = getWallFlag(ZP, state);
        boolean nz = getWallFlag(NZ, state);
        boolean pz = getWallFlag(PZ, state);
        if (getAllWallsOnNoWall(state) && !zn && !zp && !nz && !pz) zn = zp = nz = pz = true;

        boolean azn, azp, anz, apz;
        BlockState above = getBlockState(baseI, jOffset + 1, baseK);
        if (isFullEmpty(above, baseI, jOffset + 1, baseK)) {
            azn = azp = anz = apz = false;
        } else if (isWallBlock(above)) {
            azn = getWallFlag(ZN, above);
            azp = getWallFlag(ZP, above);
            anz = getWallFlag(NZ, above);
            apz = getWallFlag(PZ, above);
            if (getAllWallsOnNoWall(above) && !azn && !azp && !anz && !apz) azn = azp = anz = apz = true;
        } else {
            azn = azp = anz = apz = true;
        }

        boolean iTop = isTopHalf(baseId);
        boolean kTop = isTopHalf(baseKd);
        if (iTop) {
            if (kTop) return headedToBaseGrabWall(-this.i, -this.k, zp, pz, nz, zn, azp, apz, anz, azn);
            return headedToBaseGrabWall(-this.i, this.k, pz, zn, zp, nz, apz, azn, azp, anz);
        }
        if (kTop) return headedToBaseGrabWall(this.i, -this.k, nz, zp, zn, pz, anz, azp, azn, apz);
        return headedToBaseGrabWall(this.i, this.k, zn, nz, pz, zp, azn, anz, apz, azp);
    }

    private static boolean headedToBaseGrabWall(int i, int k, boolean front, boolean side, boolean frontOpposite,
                                                boolean sideOpposite, boolean aboveFront, boolean aboveSide,
                                                boolean aboveFrontOpposite, boolean aboveSideOpposite) {
        if (sideOpposite && !aboveSideOpposite && !front && !aboveFront && i == 1) return true;
        if (frontOpposite && !aboveFrontOpposite && !side && !aboveSide && k == 1) return true;
        if (side && !aboveSide && k >= 0) return true;
        if (front && !aboveFront && k >= 0) return true;
        if (frontOpposite && !aboveFrontOpposite && !aboveFront && i == 1 && k >= 0) return true;
        return sideOpposite && !aboveSideOpposite && !aboveSide && k == 1 && i >= 0;
    }

    private boolean headedToRemoteFlatWall(BlockState state, int jOffset) {
        return !getWallFlag(this, state)
                && getWallFlag(this.rotate(90), state)
                && !getWallFlag(this.rotate(180), state)
                && getWallFlag(this.rotate(-90), state);
    }

    /** 원작 getWallFlag: 이 블록이 direction 쪽으로 이어져 있는가 (1.21 은 블록 상태에 저장돼 있다) */
    private static boolean getWallFlag(Orientation direction, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof HorizontalConnectingBlock) { // 울타리, 판유리, 철창
            if (direction == NZ) return state.get(HorizontalConnectingBlock.WEST);
            if (direction == PZ) return state.get(HorizontalConnectingBlock.EAST);
            if (direction == ZN) return state.get(HorizontalConnectingBlock.NORTH);
            if (direction == ZP) return state.get(HorizontalConnectingBlock.SOUTH);
            return false;
        }
        if (block instanceof WallBlock) {
            if (direction == NZ) return state.get(WallBlock.WEST_WALL_SHAPE) != WallShape.NONE;
            if (direction == PZ) return state.get(WallBlock.EAST_WALL_SHAPE) != WallShape.NONE;
            if (direction == ZN) return state.get(WallBlock.NORTH_WALL_SHAPE) != WallShape.NONE;
            if (direction == ZP) return state.get(WallBlock.SOUTH_WALL_SHAPE) != WallShape.NONE;
            return false;
        }
        if (block instanceof FenceGateBlock) {
            return !state.get(FenceGateBlock.OPEN) && direction.isFenceGateFront(state);
        }
        return false;
    }

    private static boolean getAllWallsOnNoWall(BlockState state) {
        return state.getBlock() instanceof PaneBlock;
    }

    private static boolean isTopHalf(double d) {
        return (int) Math.abs(Math.floor(d * 2D)) % 2 == 1;
    }

    // ------------------------------------------------------------------
    // 블록 종류
    // ------------------------------------------------------------------

    private static boolean isBottomHalfBlock(BlockState state) {
        return (isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.BOTTOM) || state.getBlock() instanceof BedBlock;
    }

    private static boolean isTopHalfBlock(BlockState state) {
        return isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.TOP;
    }

    private static boolean isHalfBlock(BlockState state) {
        return state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE;
    }

    private static boolean isStairCompact(BlockState state) {
        return state.getBlock() instanceof StairsBlock;
    }

    private static boolean isIronBars(BlockState state) {
        return state != null && state.getBlock() instanceof PaneBlock;
    }

    private static boolean isStoneWall(BlockState state) {
        return state != null && state.getBlock() instanceof WallBlock;
    }

    private static boolean isFenceBase(BlockState state) {
        return state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock;
    }

    private static boolean isFence(BlockState state) {
        return state != null && (isFenceBase(state) || isClosedFenceGate(state));
    }

    private static boolean isFenceGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock;
    }

    private static boolean isClosedFenceGate(BlockState state) {
        return isFenceGate(state) && !state.get(FenceGateBlock.OPEN);
    }

    /** 원작의 버그까지 그대로: 이름은 "열림"이지만 닫힌 울타리 문을 검사한다 */
    private static boolean isOpenFenceGate(BlockState state) {
        return isFenceGate(state) && !state.get(FenceGateBlock.OPEN);
    }

    public static boolean isTrapDoor(BlockState state) {
        return state.getBlock() instanceof TrapdoorBlock;
    }

    public static boolean isClosedTrapDoor(BlockState state) {
        return !state.get(TrapdoorBlock.OPEN);
    }

    private static boolean isOpenTrapDoor(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        return isTrapDoor(state) && !isClosedTrapDoor(state);
    }

    private static boolean isClosedTrapDoor(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        return isTrapDoor(state) && isClosedTrapDoor(state);
    }

    private static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock;
    }

    private static boolean isDoorTop(BlockState state) {
        return state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    private boolean isDoorFrontBlocked(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        if (!isDoor(state)) return true;
        if (isDoorTop(state)) return isDoorFrontBlocked(x, jOffset - 1, z);

        return switch (getDoorFacing(state)) {
            case SOUTH -> this.k < 0;
            case WEST -> this.i > 0;
            case NORTH -> this.k > 0;
            case EAST -> this.i < 0;
            default -> true;
        };
    }

    private static Direction getDoorFacing(BlockState state) {
        Direction f = state.get(DoorBlock.FACING);
        if (!state.get(DoorBlock.OPEN)) return f;
        if (state.get(DoorBlock.HINGE) == DoorHinge.LEFT) {
            return switch (f) {
                case EAST -> Direction.NORTH;
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                default -> f;
            };
        }
        return switch (f) {
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            default -> f;
        };
    }

    private static BlockState getWallBlockState(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        return isWallBlock(state) ? state : null;
    }

    private static boolean isWallBlock(BlockState state) {
        return state.getBlock() instanceof PaneBlock || isFence(state);
    }

    // ------------------------------------------------------------------
    // 비어 있음 / 단단함
    // ------------------------------------------------------------------

    /** 원작 Material.isSolid() && blocksMovement() 에 해당하는 판정 */
    private static boolean isSolid(BlockState state, int x, int y, int z) {
        Block block = state.getBlock();
        if (block instanceof LadderBlock || block instanceof CarpetBlock || block instanceof SnowBlock
                || block instanceof ScaffoldingBlock || block instanceof AbstractSignBlock
                || block instanceof AbstractPressurePlateBlock) {
            return false;
        }
        if (!state.blocksMovement()) return false;
        return !state.getCollisionShape(world, new BlockPos(x, y, z)).isEmpty();
    }

    private static boolean isFullEmpty(BlockState state, int x, int jOffset, int z) {
        if (state == null) return true;
        return !isSolid(state, x, localOffset + jOffset, z);
    }

    private static boolean isEmpty(int x, int jOffset, int z) {
        return isFullEmpty(getBlockState(x, jOffset, z), x, jOffset, z) && !isFence(getBlockState(x, jOffset - 1, z));
    }

    private static boolean isLowerHalfEmpty(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        boolean empty = isFullEmpty(state, x, jOffset, z);
        if (!empty && isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.TOP) empty = true;
        return empty;
    }

    private boolean isLowerHalfFrontFullEmpty(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        boolean empty = isFullEmpty(state, x, jOffset, z);

        if (!empty && isStairCompact(state) && isTopStairCompactFront(state)) empty = true;
        if (!empty && isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.TOP) empty = true;
        if (!empty && isWallBlock(state) && !headedToFrontWall(x, jOffset, z, state)) empty = true;
        if (!empty && isDoor(state) && !rotate(180).isDoorFrontBlocked(x, jOffset, z)) empty = true;
        if (!empty && isTrapDoor(state) && (isClosedTrapDoor(state) || !rotate(180).isTrapDoorFront(state))) empty = true;
        return empty;
    }

    private boolean isUpperHalfFrontAnySolid(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        boolean solid = isUpperHalfFrontFullSolid(x, jOffset, z);
        if (solid && isWallBlock(state) && !headedToFrontWall(x, jOffset, z, state)) solid = false;
        return solid;
    }

    private static boolean isUpperHalfFrontFullSolid(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        boolean solid = isSolid(state, x, localOffset + jOffset, z);
        if (solid && isTrapDoor(state)) solid = false;
        if (solid && isOpenFenceGate(state)) solid = false;
        return solid;
    }

    private boolean isUpperHalfFrontEmpty(int x, int jOffset, int z) {
        BlockState state = getBlockState(x, jOffset, z);
        boolean empty = isFullEmpty(state, x, jOffset, z);

        if (!empty) {
            if (isBottomHalfBlock(state)) empty = true;
            if (!empty && isStairCompact(state) && isBottomStairCompactFront(state)) empty = true;
        }
        if (!empty && isTrapDoor(state)) empty = true;
        if (!empty) {
            BlockState wall = getWallBlockState(x, jOffset, z);
            if (wall != null && (!headedToFrontWall(x, jOffset, z, wall)
                    || isWallBlock(getBlockState(x - this.i, jOffset, z - this.k))))
                empty = true;
        }
        return empty;
    }

    // ------------------------------------------------------------------
    // 들어갈 수 있는가
    // ------------------------------------------------------------------

    private static boolean isBaseAccessible(int jOffset) {
        return isBaseAccessible(jOffset, false, false);
    }

    private static boolean isBaseAccessible(int jOffset, boolean bottom, boolean full) {
        BlockState state = getBaseBlockState(jOffset);
        boolean accessible = isEmpty(baseI, jOffset, baseK);

        if (!accessible && isFullEmpty(state, baseI, jOffset, baseK)) accessible = true;
        if (!accessible && isOpenTrapDoor(baseI, jOffset, baseK)) accessible = true;
        if (!accessible && bottom && isClosedTrapDoor(baseI, jOffset, baseK)) accessible = true;
        if (!accessible && !full && isWallBlock(state)) accessible = true;
        if (!accessible && isDoor(state)) accessible = true;
        return accessible;
    }

    private boolean isRemoteAccessible(int jOffset) {
        boolean accessible = isEmpty(remoteI, jOffset, remoteK);

        if (accessible) {
            BlockState baseState = getBaseBlockState(jOffset);
            if (isTrapDoor(baseState)) accessible = !isTrapDoorFront(baseState);
            if (accessible && isDoor(baseState)) accessible = !isDoorFrontBlocked(baseI, jOffset, baseK);
            if (remoteLadderClimbing(jOffset)) accessible = false;
        }

        if (!accessible && isTrapDoor(getRemoteBlockState(jOffset)))
            accessible = isClosedTrapDoor(getRemoteBlockState(jOffset));

        if (!accessible) {
            BlockState remoteState = getRemoteBlockState(jOffset);
            if (isWallBlock(remoteState) && !headedToFrontWall(remoteI, jOffset, remoteK, remoteState)
                    && !isFence(getRemoteBlockState(jOffset - 1)))
                accessible = true;

            BlockState remoteBelowState = getRemoteBlockState(jOffset - 1);
            if (!accessible && isFence(remoteBelowState)
                    && (!headedToFrontWall(remoteI, jOffset - 1, remoteK, remoteBelowState)
                    || isWallBlock(getBaseBlockState(jOffset - 1)))) {
                if (!isStoneWall(remoteBelowState) || headedToRemoteFlatWall(remoteBelowState, -1))
                    accessible = true;
            }

            if (!accessible && isDoor(remoteState) && !rotate(180).isDoorFrontBlocked(remoteI, jOffset, remoteK))
                accessible = true;
        }

        return accessible;
    }

    private boolean isAccessAccessible(int jOffset) {
        if (!isDiagonal) return true;
        return isEmpty(remoteI, jOffset, baseK) && isEmpty(baseI, jOffset, remoteK);
    }

    private boolean isFullExtentAccessible(int jOffset, boolean remote) {
        return isFullAccessible(jOffset, remote);
    }

    private boolean isJustLowerHalfExtentAccessible(int jOffset) {
        BlockState remoteState = getRemoteBlockState(jOffset);
        return isTopHalfBlock(remoteState) || (isStairCompact(remoteState) && isTopStairCompactFront(remoteState));
    }

    private boolean isFullAccessible(int jOffset, boolean remote) {
        if (remote) return isBaseAccessible(jOffset) && isRemoteAccessible(jOffset) && isAccessAccessible(jOffset);
        return isEmpty(baseI, jOffset, baseK);
    }

    private boolean isLowerHalfAccessible(int jOffset, boolean remote) {
        if (remote) {
            return isBaseAccessible(1, true, false)
                    && rotate(180).isLowerHalfFrontFullEmpty(baseI, 1, baseK)
                    && isLowerHalfFrontFullEmpty(remoteI, 1, remoteK);
        }
        return isLowerHalfEmpty(baseI, jOffset, baseK);
    }

    // ------------------------------------------------------------------
    // 블록 읽기 / 위치 초기화
    // ------------------------------------------------------------------

    private static BlockState getBlockState(int x, int jOffset, int z) {
        return world.getBlockState(new BlockPos(x, localOffset + jOffset, z));
    }

    private static Block getBaseBlock(int jOffset) {
        return getBaseBlockState(jOffset).getBlock();
    }

    private static BlockState getBaseBlockState(int jOffset) {
        return getBlockState(baseI, jOffset, baseK);
    }

    private static Block getRemoteBlock(int jOffset) {
        return getRemoteBlockState(jOffset).getBlock();
    }

    private static BlockState getRemoteBlockState(int jOffset) {
        return getBlockState(remoteI, jOffset, remoteK);
    }

    private void initialize(World w, int bi, double id, double jhd, int bk, double kd) {
        world = w;
        baseI = bi;
        baseId = id;
        baseJhd = jhd;
        baseK = bk;
        baseKd = kd;
        remoteI = bi + this.i;
        remoteK = bk + this.k;
    }

    private static void initializeOffset(double offsetHalfs, boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling) {
        crawl = isClimbCrawling || isCrawlClimbing || isCrawling;

        double offsetJhd = baseJhd + offsetHalfs;
        int offsetJh = (int) Math.floor(offsetJhd);
        jhOffset = offsetJhd - offsetJh;

        // 원작은 y >= 0 만 고려해 / 와 % 를 썼다. 1.21 은 음수 높이가 있으므로 floorDiv/floorMod 를 쓴다.
        allJ = Math.floorDiv(offsetJh, 2);
        allOffset = Math.floorMod(offsetJh, 2);
    }

    private static void initializeLocal(int localHalfOffsetBase) {
        localHalfOffset = localHalfOffsetBase + allOffset;
        localHalf = Math.abs(localHalfOffset) % 2;
        localOffset = allJ + (localHalfOffset - localHalf) / 2;
    }

    private static final ClimbGap CLIMB_GAP_TEMP = new ClimbGap();
    private static final ClimbGap CLIMB_GAP_OUTER_TEMP = new ClimbGap();

    private static World world;
    private static double baseJhd;
    private static double jhOffset;
    private static int allJ;
    private static int allOffset;
    private static int baseI;
    private static int baseK;
    private static double baseId;
    private static double baseKd;
    private static int remoteI;
    private static int remoteK;
    private static boolean crawl;

    private static int localHalfOffset;
    private static int localHalf;
    private static int localOffset;

    private static boolean grabRemote;
    private static int grabType;
    private static Block grabBlock;
    private static int grabMeta;

    @Override
    public String toString() {
        if (this == ZZ) return "ZZ";
        if (this == NZ) return "NZ";
        if (this == PZ) return "PZ";
        if (this == ZP) return "ZP";
        if (this == ZN) return "ZN";
        if (this == PN) return "PN";
        if (this == PP) return "PP";
        if (this == NN) return "NN";
        if (this == NP) return "NP";
        return "UNKNOWN(" + i + "," + k + ")";
    }
}
