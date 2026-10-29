// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (net.smart.moving.render.SmartMovingModel), GPL-3.0-or-later.
package com.smartmoving.client;

import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

/**
 * 원작 Smart Render 의 스마트 무빙 애니메이션 (SmartMovingModel.setRotationAngles) 중 팔·다리·머리 부분.
 *
 * <p>원작은 몸통을 여러 조각(몸 전체, 가슴, 골반 등)으로 나눠 따로 돌렸다. 바닐라 모델에는 그런 조각이 없으므로
 * 몸 전체의 기울기는 바닐라의 엎드린 자세에 맡기고, 팔·다리·머리 각도만 원작 공식대로 정한다.
 */
public final class SmartAnimations {
    private static final float WHOLE = (float) Math.PI * 2F;
    private static final float HALF = (float) Math.PI;
    private static final float QUARTER = HALF / 2F;
    private static final float EIGHTH = QUARTER / 2F;
    private static final float SIXTEENTH = EIGHTH / 2F;
    private static final float THIRTYTWOTH = SIXTEENTH / 2F;
    private static final float SIXTYFOURTH = THIRTYTWOTH / 2F;
    private static final float FREQUENCE_FACTOR = 0.6662F;
    private static final float RADIANS_PER_DEGREE = HALF / 180F;

    private SmartAnimations() {
    }

    /** 원작 SmartStatistics: 매 틱 모든 플레이어의 움직인 거리를 누적한다 */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.isPaused()) return;
            for (Player player : client.level.players()) {
                tickStatistics(player, SmartMovingPlayer.of(player));
            }
        });
    }

    private static void tickStatistics(Player player, SmartMovingState s) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (!s.animHasLast) {
            s.animLastX = x;
            s.animLastY = y;
            s.animLastZ = z;
            s.animHasLast = true;
        }
        double dx = x - s.animLastX;
        double dy = y - s.animLastY;
        double dz = z - s.animLastZ;
        s.animLastX = x;
        s.animLastY = y;
        s.animLastZ = z;
        // 순간이동 등은 무시
        if (dx * dx + dy * dy + dz * dz > 64) return;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        s.animHorizontal.add(horizontal);
        s.animVertical.add(Math.abs(dy));
        s.animAll.add(Math.sqrt(horizontal * horizontal + dy * dy));
        s.animHorizontalDistance = (float) horizontal;

        // 원작 SmartRenderRender: 움직이는 방향
        if (horizontal > 0.0001) {
            s.animVerticalAngle = (float) Math.atan(dy / horizontal);
            float angle = (float) -Math.atan(dx / dz);
            if (Float.isNaN(angle)) angle = s.animHorizontalAngle;
            else if (dz < 0) angle += HALF;
            s.animHorizontalAngle = angle;
        } else if (dy != 0) {
            s.animVerticalAngle = dy > 0 ? QUARTER : -QUARTER;
        } else {
            s.animVerticalAngle = 0;
        }
    }

    /** HumanoidModel.setAngles() 끝에서 부른다. 스마트 무빙 동작 중이 아니면 아무것도 하지 않는다. */
    public static void apply(HumanoidModel<?> model, HumanoidRenderState state, Player player) {
        SmartMovingState s = SmartMovingPlayer.of(player);
        float totalTime = state.ageInTicks;
        float viewVerticalAngle = state.xRot * RADIANS_PER_DEGREE;
        float viewHorizontalAngle = state.yRot * RADIANS_PER_DEGREE;

        ModelPart head = model.head;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        ModelPart rightLeg = model.rightLeg;
        ModelPart leftLeg = model.leftLeg;

        if (s.climbing || s.crawlClimbing) {
            climb(s, head, rightArm, leftArm, rightLeg, leftLeg, viewVerticalAngle);
        } else if (s.ceilingClimbing) {
            ceiling(s, head, rightArm, leftArm, rightLeg, leftLeg);
        } else if (s.swimming) {
            swim(s, head, rightArm, leftArm, rightLeg, leftLeg, totalTime);
        } else if (s.diving) {
            dive(s, head, rightArm, leftArm, rightLeg, leftLeg);
        } else if (s.crawling) {
            crawl(s, head, rightArm, leftArm, rightLeg, leftLeg, viewHorizontalAngle);
        } else if (s.sliding) {
            slide(s, head, rightArm, leftArm, rightLeg, leftLeg, viewHorizontalAngle);
        } else if (s.flying) {
            fly(s, rightArm, leftArm, rightLeg, leftLeg, totalTime);
        } else if (s.headJumping) {
            headJump(s, rightArm, leftArm, rightLeg, leftLeg);
        }
    }

    /** 원작 isClimb / isCrawlClimb */
    private static void climb(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                              ModelPart rightLeg, ModelPart leftLeg, float viewVerticalAngle) {
        float totalVerticalDistance = s.animVertical.total;
        float totalHorizontalDistance = s.animHorizontal.total;
        float verticalSpeed = Math.min(0.5F, s.animVertical.currentSpeed());
        float horizontalSpeed = Math.min(0.5F, s.animHorizontal.currentSpeed());

        head.yRot = 0F;
        head.xRot = viewVerticalAngle;

        // 원작 기본값: 손 UpGrab, 발 DownStep
        float handsDistanceUpFactor = 2F;
        float handsDistanceUpOffset = -2.5F;
        float handsDistanceSideFactor = 1.0F;
        float feetDistanceUpFactor = verticalSpeed > 0 ? 0.3F / verticalSpeed : 0F;
        float feetDistanceUpOffset = -0.3F;
        float feetDistanceSideFactor = 0.5F;

        rightArm.xRot = Mth.cos(totalVerticalDistance * FREQUENCE_FACTOR + HALF) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        leftArm.xRot = Mth.cos(totalVerticalDistance * FREQUENCE_FACTOR) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        rightArm.yRot = Mth.cos(totalHorizontalDistance * FREQUENCE_FACTOR + QUARTER) * horizontalSpeed * handsDistanceSideFactor;
        leftArm.yRot = Mth.cos(totalHorizontalDistance * FREQUENCE_FACTOR) * horizontalSpeed * handsDistanceSideFactor;
        rightArm.zRot = 0F;
        leftArm.zRot = 0F;

        rightLeg.xRot = Mth.cos(totalVerticalDistance * FREQUENCE_FACTOR) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
        leftLeg.xRot = Mth.cos(totalVerticalDistance * FREQUENCE_FACTOR + HALF) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
        rightLeg.zRot = -(Mth.cos(totalHorizontalDistance * FREQUENCE_FACTOR) - 1.0F) * horizontalSpeed * feetDistanceSideFactor;
        leftLeg.zRot = -(Mth.cos(totalHorizontalDistance * FREQUENCE_FACTOR + QUARTER) + 1.0F) * horizontalSpeed * feetDistanceSideFactor;
        rightLeg.yRot = 0F;
        leftLeg.yRot = 0F;
    }

    /** 원작 isCeilingClimb */
    private static void ceiling(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                                ModelPart rightLeg, ModelPart leftLeg) {
        float currentHorizontalSpeed = s.animHorizontal.currentSpeed();
        float distance = s.animHorizontal.total * 0.7F;
        float walkFactor = factor(currentHorizontalSpeed, 0F, 0.12951545F);
        float standFactor = factor(currentHorizontalSpeed, 0.12951545F, 0F);

        // Half = 팔을 위로 (천장을 잡는다)
        leftArm.xRot = (Mth.cos(distance) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        rightArm.xRot = (Mth.cos(distance + HALF) * 0.52F - HALF) * walkFactor - HALF * standFactor;

        leftLeg.xRot = -Mth.cos(distance) * 0.12F * walkFactor;
        rightLeg.xRot = -Mth.cos(distance + HALF) * 0.32F * walkFactor;

        float rotateY = Mth.cos(distance) * 0.44F * walkFactor;
        rightArm.yRot = leftArm.yRot = -rotateY;
        rightLeg.yRot = leftLeg.yRot = -rotateY;
        rightArm.zRot = leftArm.zRot = 0F;
        head.yRot = -rotateY;
    }

    /** 원작 isSwim: 평영 */
    private static void swim(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                             ModelPart rightLeg, ModelPart leftLeg, float totalTime) {
        float currentHorizontalSpeed = s.animHorizontal.currentSpeed();
        float distance = s.animHorizontal.total;
        float walkFactor = factor(currentHorizontalSpeed, 0.15679921F, 0.52264464F);
        float sneakFactor = Math.min(factor(currentHorizontalSpeed, 0, 0.15679921F),
                factor(currentHorizontalSpeed, 0.52264464F, 0.15679921F));
        float standFactor = factor(currentHorizontalSpeed, 0.15679921F, 0F);
        float standSneakFactor = standFactor + sneakFactor;

        head.yRot = Mth.cos(distance / 2.0F - QUARTER) * walkFactor;
        head.xRot = -EIGHTH * standSneakFactor;

        rightArm.zRot = QUARTER + EIGHTH + Mth.cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
        leftArm.zRot = -QUARTER - EIGHTH - Mth.cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
        rightArm.xRot = ((distance * 0.5F) % WHOLE - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        leftArm.xRot = ((distance * 0.5F + HALF) % WHOLE - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        rightArm.yRot = leftArm.yRot = 0F;

        rightLeg.xRot = Mth.cos(distance) * 0.52264464F * walkFactor;
        leftLeg.xRot = Mth.cos(distance + HALF) * 0.52264464F * walkFactor;
        float rotateFeetAngleZ = SIXTEENTH * standSneakFactor + Mth.cos(totalTime * 0.1F) * 0.4F * (standFactor - sneakFactor);
        rightLeg.zRot = rotateFeetAngleZ;
        leftLeg.zRot = -rotateFeetAngleZ;
    }

    /** 원작 isDive */
    private static void dive(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                             ModelPart rightLeg, ModelPart leftLeg) {
        float currentSpeed = s.animAll.currentSpeed();
        float distance = s.animAll.total * 0.7F;
        float walkFactor = factor(currentSpeed, 0F, 0.15679921F);
        float standFactor = factor(currentSpeed, 0.15679921F, 0F);

        head.xRot = -EIGHTH;

        rightLeg.zRot = (Mth.cos(distance) + 1F) * 0.52264464F * walkFactor + SIXTEENTH * standFactor;
        leftLeg.zRot = (Mth.cos(distance + HALF) - 1F) * 0.52264464F * walkFactor - SIXTEENTH * standFactor;
        rightLeg.xRot = leftLeg.xRot = 0F;

        rightArm.zRot = (Mth.cos(distance + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor + (QUARTER + EIGHTH) * standFactor;
        leftArm.zRot = (Mth.cos(distance) * 0.52264464F * 2.5F - QUARTER) * walkFactor - (QUARTER + EIGHTH) * standFactor;
        rightArm.xRot = leftArm.xRot = 0F;
        rightArm.yRot = leftArm.yRot = 0F;
    }

    /** 원작 isCrawl */
    private static void crawl(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                              ModelPart rightLeg, ModelPart leftLeg, float viewHorizontalAngle) {
        float currentHorizontalSpeed = s.animHorizontal.currentSpeed();
        float distance = s.animHorizontal.total * 1.3F;
        float walkFactor = factor(currentHorizontalSpeed, 0F, 0.12951545F);
        float standFactor = factor(currentHorizontalSpeed, 0.12951545F, 0F);

        head.zRot = -viewHorizontalAngle;
        head.xRot = -EIGHTH;

        rightLeg.xRot = (Mth.cos(distance - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.xRot = (Mth.cos(distance - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        rightLeg.zRot = (Mth.cos(distance - QUARTER) + 1F) * 0.25F * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.zRot = (Mth.cos(distance - QUARTER) - 1F) * 0.25F * walkFactor - THIRTYTWOTH * standFactor;

        rightArm.xRot = HALF + EIGHTH;
        leftArm.xRot = HALF + EIGHTH;
        rightArm.zRot = (Mth.cos(distance + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + SIXTEENTH * standFactor;
        leftArm.zRot = (Mth.cos(distance + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor - SIXTEENTH * standFactor;
        rightArm.yRot = -QUARTER;
        leftArm.yRot = QUARTER;
    }

    /** 원작 isSlide */
    private static void slide(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                              ModelPart rightLeg, ModelPart leftLeg, float viewHorizontalAngle) {
        float distance = s.animHorizontal.total * 0.7F;
        float walkFactor = factor(s.animHorizontal.currentSpeed(), 0F, 1F) * 0.8F;

        head.zRot = -viewHorizontalAngle;
        head.xRot = -EIGHTH - SIXTEENTH;

        rightLeg.xRot = Mth.cos(distance + HALF) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        leftLeg.xRot = Mth.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        rightLeg.zRot = THIRTYTWOTH;
        leftLeg.zRot = -THIRTYTWOTH;

        rightArm.xRot = Mth.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        leftArm.xRot = Mth.cos(distance - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        rightArm.zRot = SIXTEENTH;
        leftArm.zRot = -SIXTEENTH;
        rightArm.yRot = -QUARTER;
        leftArm.yRot = QUARTER;
    }

    /** 원작 isFlying */
    private static void fly(SmartMovingState s, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                            ModelPart leftLeg, float totalTime) {
        float currentSpeed = s.animAll.currentSpeed();
        float distance = s.animAll.total * 0.08F;
        float walkFactor = factor(currentSpeed, 0F, 1);
        float standFactor = factor(currentSpeed, 1F, 0F);
        float time = totalTime * 0.15F;

        rightArm.yRot = (Mth.cos(time) * SIXTEENTH) * standFactor;
        leftArm.yRot = (Mth.cos(time) * SIXTEENTH) * standFactor;
        rightArm.zRot = (Mth.cos(distance + HALF) * SIXTYFOURTH + (HALF - SIXTEENTH)) * walkFactor + QUARTER * standFactor;
        leftArm.zRot = (Mth.cos(distance) * SIXTYFOURTH - (HALF - SIXTEENTH)) * walkFactor - QUARTER * standFactor;
        rightArm.xRot = leftArm.xRot = 0F;

        rightLeg.xRot = Mth.cos(distance) * SIXTYFOURTH * walkFactor + Mth.cos(time + HALF) * SIXTYFOURTH * standFactor;
        leftLeg.xRot = Mth.cos(distance + HALF) * SIXTYFOURTH * walkFactor + Mth.cos(time) * SIXTYFOURTH * standFactor;
        rightLeg.zRot = SIXTYFOURTH;
        leftLeg.zRot = -SIXTYFOURTH;
    }

    /** 원작 isHeadJump: 팔을 앞으로 뻗고 다이빙하듯 */
    private static void headJump(SmartMovingState s, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                                 ModelPart leftLeg) {
        float currentVerticalAngle = s.animVerticalAngle;
        float bendFactor = Math.min(factor(currentVerticalAngle, QUARTER, 0), factor(currentVerticalAngle, -QUARTER, 0));
        rightArm.xRot = bendFactor * -EIGHTH;
        leftArm.xRot = bendFactor * -EIGHTH;
        rightLeg.xRot = bendFactor * -EIGHTH;
        leftLeg.xRot = bendFactor * -EIGHTH;

        float armFactorZ = factor(currentVerticalAngle, QUARTER, -QUARTER);
        rightArm.zRot = HALF - SIXTEENTH + armFactorZ * EIGHTH;
        leftArm.zRot = SIXTEENTH - HALF - armFactorZ * EIGHTH;
        rightArm.yRot = leftArm.yRot = 0F;

        float legFactorZ = factor(currentVerticalAngle, -QUARTER, QUARTER);
        rightLeg.zRot = SIXTYFOURTH * legFactorZ;
        leftLeg.zRot = -SIXTYFOURTH * legFactorZ;
    }

    /** 원작 Factor: x 가 x0 에서 x1 로 갈 때 0 에서 1 로 (범위 밖은 0 또는 1) */
    private static float factor(float x, float x0, float x1) {
        if (x0 > x1) {
            if (x <= x1) return 1F;
            if (x >= x0) return 0F;
            return (x0 - x) / (x0 - x1);
        }
        if (x >= x1) return 1F;
        if (x <= x0) return 0F;
        return (x - x0) / (x1 - x0);
    }
}
