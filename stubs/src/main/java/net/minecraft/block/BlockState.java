package net.minecraft.block;
import net.minecraft.registry.tag.TagKey;
public final class BlockState {
    public boolean isSolid() { return false; }
    public boolean isIn(TagKey<Block> tag) { return false; }
}
