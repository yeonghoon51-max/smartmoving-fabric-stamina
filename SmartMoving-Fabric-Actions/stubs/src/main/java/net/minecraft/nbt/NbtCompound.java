package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

public class NbtCompound extends NbtElement {
    private final Map<String, NbtElement> data = new HashMap<>();
    public void put(String key, NbtElement element) { data.put(key, element); }
    public void putFloat(String key, float value) { data.put(key, new NbtFloat(value)); }
    public void putBoolean(String key, boolean value) { data.put(key, new NbtByte((byte)(value?1:0))); }
    public NbtElement get(String key) { return data.get(key); }
}
