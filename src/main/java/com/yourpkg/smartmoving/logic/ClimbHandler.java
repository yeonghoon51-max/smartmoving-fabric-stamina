package com.yourpkg.smartmoving.logic;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class ClimbHandler {

    // 모드 추가 등반 가능 블록 태그 (datapacks로 확장 가능)
    private static final TagKey<Block> EXTRA_CLIMBABLE = TagKey.of(
            RegistryKeys.BLOCK, Identifier.of("smartmoving", "climbable_extra"));

    public static Vec3d apply(PlayerEntity player, PlayerContext ctx, Vec3d movementInput) {
        if (!ctx.grabbing || ctx.grabEnergy <= 0f) return movementInput;

        World world = player.getWorld();
        boolean nearClimbable = player.isClimbing()
                || isSolidWallAhead(world, player)
                || isClimbableBlockAhead(world, player);

        if (!nearClimbable) return movementInput;

        // 수평 이동 약간 강화
        float hBoost = SmartMovingMod.CONFIG.climbHorizontalBoost;
        movementInput = new Vec3d(
                movementInput.x * hBoost,
                movementInput.y,
                movementInput.z * hBoost);

        // 위로 이동 보조 (최대 0.26 m/t 이상 올라가지 않도록 제한)
        double newVy = player.getVelocity().y + SmartMovingMod.CONFIG.climbVerticalAssist;
        player.setVelocity(player.getVelocity().x, Math.min(newVy, 0.26), player.getVelocity().z);

        return movementInput;
    }

    /** 플레이어가 향하는 방향 정면에 단단한 벽이 있는지 확인 */
    public static boolean isSolidWallAhead(World world, PlayerEntity player) {
        Direction facing = player.getHorizontalFacing();
        BlockPos frontPos = player.getBlockPos().offset(facing);
        return world.getBlockState(frontPos).isSolid();
    }

    private static boolean isClimbableBlockAhead(World world, PlayerEntity player) {
        Direction facing = player.getHorizontalFacing();
        BlockPos frontPos = player.getBlockPos().offset(facing);
        var state = world.getBlockState(frontPos);
        return state.isIn(BlockTags.CLIMBABLE) || state.isIn(EXTRA_CLIMBABLE);
    }
}
