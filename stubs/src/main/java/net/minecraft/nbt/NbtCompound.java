package net.minecraft.nbt;
public class NbtCompound implements NbtElement {
    public void put(String key, NbtElement element) {}
    public NbtElement get(String key) { return null; }
    public void putFloat(String key, float value) {}
    public void putBoolean(String key, boolean value) {}
    public float getFloat(String key) { return 0f; }
    public boolean getBoolean(String key) { return false; }
}
