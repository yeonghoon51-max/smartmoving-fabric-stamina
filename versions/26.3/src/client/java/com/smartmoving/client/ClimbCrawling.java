// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (SmartMovingSelf.updateEntityActionState), GPL-3.0-or-later.
package com.smartmoving.client;

import com.smartmoving.logic.Solids;
import com.smartmoving.state.SmartMovingState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 원작의 두 가지 "작은 자세로 오르기".
 *
 * <ul>
 *   <li><b>climb-crawl</b> (isClimbCrawling): 벽에 매달린 채 잡기 + 웅크리기 + 앞키를 누르면, 앞의 낮은(1칸) 틈으로
 *       윗몸을 밀어 넣는다. 몸이 납작해지고 원래 몸의 윗부분 높이로 올라간다. 틈 안에 들어가면 잠깐 매달려 있다가 기어가기로 바뀐다.</li>
 *   <li><b>crawl-climb</b> (isCrawlClimbing): 기어가는(또는 슬라이딩하던) 자세 그대로 웅크리기 + 앞키로 턱을 오른다.
 *       몸 아래가 비면 일어선다.</li>
 * </ul>
 *
 * <p>원작은 이때 충돌 상자의 아래쪽 1칸을 잘라냈다 (heightOffset = -1). 여기서는 바닐라의 납작한 자세(SWIMMING, 높이 0.6)를 쓰고,
 * 위치를 1칸 올려서 같은 자리에 상자를 둔다. 그래서 "원래 발" 높이는 항상 상자 아래 1칸이다.
 */
public final class ClimbCrawling {
    /** 원작 climbIntoCount 초기값 */
    private static final int CLIMB_INTO_TICKS = 6;
    /** 원작 상자 높이 (1.8 + heightOffset) */
    private static final double SMALL_HEIGHT = 0.8;
    private static final double STANDING_HEIGHT = 1.8;

    private ClimbCrawling() {
    }

    /**
     * ClientMovementController 에서 매 틱 이동 전에 부른다 (기어가기·벽 타기 의도 판정 다음).
     *
     * @param forward    앞키 (원작 moveForward > 0)
     * @param crawlInput 웅크리기 키 또는 기어가기 토글 (원작 sneakButton.Pressed || crawlToggled)
     */
    public static void tick(Player player, SmartMovingState state, boolean forward, boolean crawlInput,
                            boolean special) {
        if (special) {
            if (state.climbCrawling || state.crawlClimbing) {
                state.climbCrawling = false;
                state.crawlClimbing = false;
                state.climbIntoCount = 0;
                standUp(player);
            }
            return;
        }

        tickCrawlClimbing(player, state, forward, crawlInput);
        tickClimbCrawling(player, state, crawlInput);
    }

    /** 원작 isCrawlClimbing 부분 */
    private static void tickCrawlClimbing(Player player, SmartMovingState state, boolean forward,
                                          boolean crawlInput) {
        boolean wasCrawlClimbing = state.crawlClimbing;
        state.crawlClimbing = (state.wasCrawling || state.crawlClimbing) && state.climbing && state.neighborClimbing
                && crawlInput && forward;

        if (state.crawlClimbing) {
            double minY = player.getBoundingBox().minY;
            boolean canStandUp = !Solids.anyBetween(player, minY - (state.climbCrawling ? 0.95D : 1D), minY);
            if (canStandUp) {
                // 몸 아래가 비었다: 다리를 펴고 일반 벽 타기로
                wasCrawlClimbing = false;
                state.crawlClimbing = false;
                if (!state.climbCrawling) standUp(player);
            }

            if (!wasCrawlClimbing) {
                state.wasCrawling = false;
                state.crawling = false;
            }
        } else if (wasCrawlClimbing) {
            if (!state.climbing) {
                // 다 올라왔다: 그대로 기어간다
                state.crawling = true;
                double minY = player.getBoundingBox().minY;
                moveVertical(player, Math.floor(minY) - minY);
            } else if (!forward) {
                state.crawling = crawlInput;
                state.wantClimbUp = false;
                state.wantClimbDown = false;
                if (!crawlInput) standUp(player);
                double minY = player.getBoundingBox().minY;
                moveVertical(player, Math.floor(minY) - minY + (crawlInput ? 0D : 1D));
            } else if (!crawlInput) {
                // 웅크리기를 뗐다: 일어서서 벽 타기를 계속한다
                standUp(player);
                double minY = player.getBoundingBox().minY;
                moveVertical(player, Math.ceil(minY) - minY);
            }
        }
    }

    /** 원작 isClimbCrawling 부분 */
    private static void tickClimbCrawling(Player player, SmartMovingState state, boolean crawlInput) {
        boolean wasClimbCrawling = state.climbCrawling;
        boolean needClimbCrawling = state.hasClimbCrawlGap || (state.hasClimbGap && state.climbHolding);
        boolean canClimbCrawling = state.wantClimbHolding && state.wantClimbUp;

        if (state.climbIntoCount > 1) {
            state.climbIntoCount--;
        } else if (state.climbCrawling && !needClimbCrawling && state.climbIntoCount == 0) {
            // 틈 안으로 들어갔다: 몇 틱 더 매달린 채로 앞으로 들어간다
            state.climbIntoCount = CLIMB_INTO_TICKS;
        }

        state.climbCrawling = canClimbCrawling
                && ((needClimbCrawling && state.climbIntoCount == 0) || state.climbIntoCount > 1);

        if (state.climbCrawling && !wasClimbCrawling) {
            boolean alreadySmall = state.crawling || state.crawlClimbing;
            if (!alreadySmall) {
                // 몸을 납작하게 해서 원래 몸의 윗부분 자리에 둔다
                player.setPose(Pose.SWIMMING);
                player.setPos(player.getX(), player.getY() + (STANDING_HEIGHT - SMALL_HEIGHT), player.getZ());
            }
            // 머리 위가 막혀 있을 때 틈 안의 블록에 끼지 않도록 살짝 올린다 (원작 그대로)
            boolean wasCollidedHorizontally = player.horizontalCollision;
            player.move(MoverType.SELF, new Vec3(0, 0.05, 0));
            player.horizontalCollision = wasCollidedHorizontally;
        } else if (!state.climbCrawling && wasClimbCrawling) {
            state.climbIntoCount = 0;
            if (state.crawling || state.crawlClimbing) return;

            if (mustCrawl(player) || crawlInput) {
                double minY = player.getBoundingBox().minY;
                double gapUnderneath = minY - Solids.maxBetween(player, minY - 1D, minY, 0);
                if (gapUnderneath >= 0D && gapUnderneath < 1D) {
                    // 틈 바닥에 내려앉아 기어간다
                    state.crawling = true;
                    moveVertical(player, -gapUnderneath);
                } else {
                    standUp(player);
                }
            } else {
                standUp(player);
            }
        }
    }

    /** 원작 mustCrawl: 작은 상자 기준으로 일어설 높이(1.8)가 안 되는가 */
    private static boolean mustCrawl(Player player) {
        double minY = player.getBoundingBox().minY;
        double bottom = Solids.maxBetween(player, minY - 1D, minY, 0);
        double top = minY + SMALL_HEIGHT;
        double ceiling = Solids.minBetween(player, top, top + 1.1D, 0);
        return ceiling - bottom < STANDING_HEIGHT;
    }

    /** 원작 resetHeightOffset(): 잘라냈던 아래쪽 1칸을 되돌린다 (발이 상자 아래 1칸으로) */
    private static void standUp(Player player) {
        player.setPose(Pose.STANDING);
        player.setPos(player.getX(), player.getY() - (STANDING_HEIGHT - SMALL_HEIGHT), player.getZ());
    }

    private static void moveVertical(Player player, double dy) {
        if (dy == 0) return;
        player.move(MoverType.SELF, new Vec3(0, dy, 0));
    }
}
