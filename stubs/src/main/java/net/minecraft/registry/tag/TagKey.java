package net.minecraft.registry.tag;
import net.minecraft.util.Identifier;
public final class TagKey<T> {
    private TagKey() {}
    @SuppressWarnings("unchecked")
    public static <T> TagKey<T> of(Object registryRef, Identifier id) { return new TagKey<>(); }
}
