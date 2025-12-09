package net.minecraft.nbt;
public class NbtByte extends NbtElement {
    private final byte value;
    public NbtByte(byte value){this.value=value;}
    public byte byteValue(){return value;}
}
