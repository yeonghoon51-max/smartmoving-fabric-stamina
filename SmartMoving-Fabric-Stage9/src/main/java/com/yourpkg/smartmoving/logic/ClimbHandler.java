package com.yourpkg.smartmoving.logic;

import com.yourpkg.smartmoving.SmartMovingMod;
import com.yourpkg.smartmoving.state.PlayerContext;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ClimbHandler {
    private static final TagKey<Block> EXTRA = TagKey.of(net.minecraft.registry.RegistryKeys.BLOCK,
            net.minecraft.util.Identifier.of("smartmoving","climbable_extra"));

    public static Vec3d apply(PlayerEntity player, PlayerContext ctx, Vec3d in) {
        World w = player.getWorld();
        boolean touchingWall = isSolidFront(w, player);
        var frontState = w.getBlockState(player.getBlockPos().offset(player.getHorizontalFacing()));
        boolean climbable = player.isClimbing() || touchingWall || frontState.isIn(BlockTags.CLIMBABLE) || frontState.isIn(EXTRA);

        if (climbable && ctx.grabbing && ctx.grabEnergy > 0f) {
            in = new Vec3d(in.x * SmartMovingMod.CONFIG.climbHorizontalBoost, in.y, in.z * SmartMovingMod.CONFIG.climbHorizontalBoost);
            double vy = player.getVelocity().y + SmartMovingMod.CONFIG.climbVerticalAssist;
            player.setVelocity(player.getVelocity().x, Math.min(vy, 0.26), player.getVelocity().z);
        }
        return in;
    }

    private static boolean isSolidFront(World w, PlayerEntity p) {
        Direction f = p.getHorizontalFacing();
        BlockPos fp = p.getBlockPos().offset(f);
        return w.getBlockState(fp).isSolidBlock(w, fp);
    }
}
