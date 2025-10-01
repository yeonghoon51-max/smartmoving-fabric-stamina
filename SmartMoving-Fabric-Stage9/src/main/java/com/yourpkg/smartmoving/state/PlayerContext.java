package com.yourpkg.smartmoving.state;

import net.minecraft.nbt.*;

public class PlayerContext {
    public float  jumpCharge = 0f;    // 0..100
    public float  grabEnergy = 100f;  // 0..100
    public boolean grabbing  = false;
    public boolean crawling  = false;

    public NbtCompound toNbt() {
        NbtCompound n = new NbtCompound();
        n.putFloat("jumpCharge", jumpCharge);
        n.putFloat("grabEnergy", grabEnergy);
        n.putBoolean("grabbing", grabbing);
        n.putBoolean("crawling", crawling);
        return n;
    }

    public void fromNbt(NbtCompound n) {
        NbtElement e;
        e = n.get("jumpCharge"); if (e instanceof NbtFloat f) this.jumpCharge = f.floatValue();
        e = n.get("grabEnergy"); if (e instanceof NbtFloat f) this.grabEnergy = f.floatValue();
        e = n.get("grabbing");   if (e instanceof NbtByte  b) this.grabbing  = b.byteValue()!=0;
        e = n.get("crawling");   if (e instanceof NbtByte  b) this.crawling  = b.byteValue()!=0;
    }

    public interface Holder {
        PlayerContext smartmoving$getContext();
    }
}
