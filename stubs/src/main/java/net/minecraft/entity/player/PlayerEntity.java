package net.minecraft.entity.player;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
public abstract class PlayerEntity extends LivingEntity {
    public Direction getHorizontalFacing() { return null; }
    public BlockPos getBlockPos() { return null; }
}
