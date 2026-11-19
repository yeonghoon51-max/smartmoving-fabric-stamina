// This file is part of Smart Moving (Fabric).
// Ported from Smart Moving 16.3 by Divisor (net.smart.moving.render.SmartMovingModel), GPL-3.0-or-later.
package com.smartmoving.client;

import com.smartmoving.state.SmartMovingPlayer;
import com.smartmoving.state.SmartMovingState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

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
            if (client.world == null || client.isPaused()) return;
            for (PlayerEntity player : client.world.getPlayers()) {
                tickStatistics(player, SmartMovingPlayer.of(player));
            }
        });
    }

    private static void tickStatistics(PlayerEntity player, SmartMovingState s) {
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

    /** BipedEntityModel.setAngles() 끝에서 부른다. 스마트 무빙 동작 중이 아니면 아무것도 하지 않는다. */
    public static void apply(BipedEntityModel<?> model, PlayerEntity player, float age, float headYaw, float headPitch) {
        SmartMovingState s = SmartMovingPlayer.of(player);
        float totalTime = age;
        float viewVerticalAngle = headPitch * RADIANS_PER_DEGREE;
        float viewHorizontalAngle = headYaw * RADIANS_PER_DEGREE;

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

        head.yaw = 0F;
        head.pitch = viewVerticalAngle;

        // 원작 기본값: 손 UpGrab, 발 DownStep
        float handsDistanceUpFactor = 2F;
        float handsDistanceUpOffset = -2.5F;
        float handsDistanceSideFactor = 1.0F;
        float feetDistanceUpFactor = verticalSpeed > 0 ? 0.3F / verticalSpeed : 0F;
        float feetDistanceUpOffset = -0.3F;
        float feetDistanceSideFactor = 0.5F;

        rightArm.pitch = MathHelper.cos(totalVerticalDistance * FREQUENCE_FACTOR + HALF) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        leftArm.pitch = MathHelper.cos(totalVerticalDistance * FREQUENCE_FACTOR) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
        rightArm.yaw = MathHelper.cos(totalHorizontalDistance * FREQUENCE_FACTOR + QUARTER) * horizontalSpeed * handsDistanceSideFactor;
        leftArm.yaw = MathHelper.cos(totalHorizontalDistance * FREQUENCE_FACTOR) * horizontalSpeed * handsDistanceSideFactor;
        rightArm.roll = 0F;
        leftArm.roll = 0F;

        rightLeg.pitch = MathHelper.cos(totalVerticalDistance * FREQUENCE_FACTOR) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
        leftLeg.pitch = MathHelper.cos(totalVerticalDistance * FREQUENCE_FACTOR + HALF) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
        rightLeg.roll = -(MathHelper.cos(totalHorizontalDistance * FREQUENCE_FACTOR) - 1.0F) * horizontalSpeed * feetDistanceSideFactor;
        leftLeg.roll = -(MathHelper.cos(totalHorizontalDistance * FREQUENCE_FACTOR + QUARTER) + 1.0F) * horizontalSpeed * feetDistanceSideFactor;
        rightLeg.yaw = 0F;
        leftLeg.yaw = 0F;
    }

    /** 원작 isCeilingClimb */
    private static void ceiling(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                                ModelPart rightLeg, ModelPart leftLeg) {
        float currentHorizontalSpeed = s.animHorizontal.currentSpeed();
        float distance = s.animHorizontal.total * 0.7F;
        float walkFactor = factor(currentHorizontalSpeed, 0F, 0.12951545F);
        float standFactor = factor(currentHorizontalSpeed, 0.12951545F, 0F);

        // Half = 팔을 위로 (천장을 잡는다)
        leftArm.pitch = (MathHelper.cos(distance) * 0.52F + HALF) * walkFactor + HALF * standFactor;
        rightArm.pitch = (MathHelper.cos(distance + HALF) * 0.52F - HALF) * walkFactor - HALF * standFactor;

        leftLeg.pitch = -MathHelper.cos(distance) * 0.12F * walkFactor;
        rightLeg.pitch = -MathHelper.cos(distance + HALF) * 0.32F * walkFactor;

        float rotateY = MathHelper.cos(distance) * 0.44F * walkFactor;
        rightArm.yaw = leftArm.yaw = -rotateY;
        rightLeg.yaw = leftLeg.yaw = -rotateY;
        rightArm.roll = leftArm.roll = 0F;
        head.yaw = -rotateY;
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

        head.yaw = MathHelper.cos(distance / 2.0F - QUARTER) * walkFactor;
        head.pitch = -EIGHTH * standSneakFactor;

        rightArm.roll = QUARTER + EIGHTH + MathHelper.cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
        leftArm.roll = -QUARTER - EIGHTH - MathHelper.cos(totalTime * 0.1F) * standSneakFactor * 0.8F;
        rightArm.pitch = ((distance * 0.5F) % WHOLE - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        leftArm.pitch = ((distance * 0.5F + HALF) % WHOLE - HALF) * walkFactor + SIXTEENTH * standSneakFactor;
        rightArm.yaw = leftArm.yaw = 0F;

        rightLeg.pitch = MathHelper.cos(distance) * 0.52264464F * walkFactor;
        leftLeg.pitch = MathHelper.cos(distance + HALF) * 0.52264464F * walkFactor;
        float rotateFeetAngleZ = SIXTEENTH * standSneakFactor + MathHelper.cos(totalTime * 0.1F) * 0.4F * (standFactor - sneakFactor);
        rightLeg.roll = rotateFeetAngleZ;
        leftLeg.roll = -rotateFeetAngleZ;
    }

    /** 원작 isDive */
    private static void dive(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                             ModelPart rightLeg, ModelPart leftLeg) {
        float currentSpeed = s.animAll.currentSpeed();
        float distance = s.animAll.total * 0.7F;
        float walkFactor = factor(currentSpeed, 0F, 0.15679921F);
        float standFactor = factor(currentSpeed, 0.15679921F, 0F);

        head.pitch = -EIGHTH;

        rightLeg.roll = (MathHelper.cos(distance) + 1F) * 0.52264464F * walkFactor + SIXTEENTH * standFactor;
        leftLeg.roll = (MathHelper.cos(distance + HALF) - 1F) * 0.52264464F * walkFactor - SIXTEENTH * standFactor;
        rightLeg.pitch = leftLeg.pitch = 0F;

        rightArm.roll = (MathHelper.cos(distance + HALF) * 0.52264464F * 2.5F + QUARTER) * walkFactor + (QUARTER + EIGHTH) * standFactor;
        leftArm.roll = (MathHelper.cos(distance) * 0.52264464F * 2.5F - QUARTER) * walkFactor - (QUARTER + EIGHTH) * standFactor;
        rightArm.pitch = leftArm.pitch = 0F;
        rightArm.yaw = leftArm.yaw = 0F;
    }

    /** 원작 isCrawl */
    private static void crawl(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                              ModelPart rightLeg, ModelPart leftLeg, float viewHorizontalAngle) {
        float currentHorizontalSpeed = s.animHorizontal.currentSpeed();
        float distance = s.animHorizontal.total * 1.3F;
        float walkFactor = factor(currentHorizontalSpeed, 0F, 0.12951545F);
        float standFactor = factor(currentHorizontalSpeed, 0.12951545F, 0F);

        head.roll = -viewHorizontalAngle;
        head.pitch = -EIGHTH;

        rightLeg.pitch = (MathHelper.cos(distance - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.pitch = (MathHelper.cos(distance - HALF - QUARTER) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + THIRTYTWOTH * standFactor;
        rightLeg.roll = (MathHelper.cos(distance - QUARTER) + 1F) * 0.25F * walkFactor + THIRTYTWOTH * standFactor;
        leftLeg.roll = (MathHelper.cos(distance - QUARTER) - 1F) * 0.25F * walkFactor - THIRTYTWOTH * standFactor;

        rightArm.pitch = HALF + EIGHTH;
        leftArm.pitch = HALF + EIGHTH;
        rightArm.roll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + THIRTYTWOTH) * walkFactor + SIXTEENTH * standFactor;
        leftArm.roll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH - THIRTYTWOTH) * walkFactor - SIXTEENTH * standFactor;
        rightArm.yaw = -QUARTER;
        leftArm.yaw = QUARTER;
    }

    /** 원작 isSlide */
    private static void slide(SmartMovingState s, ModelPart head, ModelPart rightArm, ModelPart leftArm,
                              ModelPart rightLeg, ModelPart leftLeg, float viewHorizontalAngle) {
        float distance = s.animHorizontal.total * 0.7F;
        float walkFactor = factor(s.animHorizontal.currentSpeed(), 0F, 1F) * 0.8F;

        head.roll = -viewHorizontalAngle;
        head.pitch = -EIGHTH - SIXTEENTH;

        rightLeg.pitch = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        leftLeg.pitch = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + SIXTYFOURTH;
        rightLeg.roll = THIRTYTWOTH;
        leftLeg.roll = -THIRTYTWOTH;

        rightArm.pitch = MathHelper.cos(distance + QUARTER) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        leftArm.pitch = MathHelper.cos(distance - HALF) * SIXTYFOURTH * walkFactor + HALF - SIXTYFOURTH;
        rightArm.roll = SIXTEENTH;
        leftArm.roll = -SIXTEENTH;
        rightArm.yaw = -QUARTER;
        leftArm.yaw = QUARTER;
    }

    /** 원작 isFlying */
    private static void fly(SmartMovingState s, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                            ModelPart leftLeg, float totalTime) {
        float currentSpeed = s.animAll.currentSpeed();
        float distance = s.animAll.total * 0.08F;
        float walkFactor = factor(currentSpeed, 0F, 1);
        float standFactor = factor(currentSpeed, 1F, 0F);
        float time = totalTime * 0.15F;

        rightArm.yaw = (MathHelper.cos(time) * SIXTEENTH) * standFactor;
        leftArm.yaw = (MathHelper.cos(time) * SIXTEENTH) * standFactor;
        rightArm.roll = (MathHelper.cos(distance + HALF) * SIXTYFOURTH + (HALF - SIXTEENTH)) * walkFactor + QUARTER * standFactor;
        leftArm.roll = (MathHelper.cos(distance) * SIXTYFOURTH - (HALF - SIXTEENTH)) * walkFactor - QUARTER * standFactor;
        rightArm.pitch = leftArm.pitch = 0F;

        rightLeg.pitch = MathHelper.cos(distance) * SIXTYFOURTH * walkFactor + MathHelper.cos(time + HALF) * SIXTYFOURTH * standFactor;
        leftLeg.pitch = MathHelper.cos(distance + HALF) * SIXTYFOURTH * walkFactor + MathHelper.cos(time) * SIXTYFOURTH * standFactor;
        rightLeg.roll = SIXTYFOURTH;
        leftLeg.roll = -SIXTYFOURTH;
    }

    /** 원작 isHeadJump: 팔을 앞으로 뻗고 다이빙하듯 */
    private static void headJump(SmartMovingState s, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                                 ModelPart leftLeg) {
        float currentVerticalAngle = s.animVerticalAngle;
        float bendFactor = Math.min(factor(currentVerticalAngle, QUARTER, 0), factor(currentVerticalAngle, -QUARTER, 0));
        rightArm.pitch = bendFactor * -EIGHTH;
        leftArm.pitch = bendFactor * -EIGHTH;
        rightLeg.pitch = bendFactor * -EIGHTH;
        leftLeg.pitch = bendFactor * -EIGHTH;

        float armFactorZ = factor(currentVerticalAngle, QUARTER, -QUARTER);
        rightArm.roll = HALF - SIXTEENTH + armFactorZ * EIGHTH;
        leftArm.roll = SIXTEENTH - HALF - armFactorZ * EIGHTH;
        rightArm.yaw = leftArm.yaw = 0F;

        float legFactorZ = factor(currentVerticalAngle, -QUARTER, QUARTER);
        rightLeg.roll = SIXTYFOURTH * legFactorZ;
        leftLeg.roll = -SIXTYFOURTH * legFactorZ;
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
