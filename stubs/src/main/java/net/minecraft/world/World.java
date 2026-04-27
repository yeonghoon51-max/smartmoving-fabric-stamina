package net.minecraft.world;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
public class World {
    public boolean isClient() { return false; }
    public BlockState getBlockState(BlockPos pos) { return null; }
}
