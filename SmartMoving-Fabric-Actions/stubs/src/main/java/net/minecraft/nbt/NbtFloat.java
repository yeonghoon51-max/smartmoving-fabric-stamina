package net.minecraft.nbt;
public class NbtFloat extends NbtElement {
    private final float value;
    public NbtFloat(float value){this.value=value;}
    public float floatValue(){return value;}
}
