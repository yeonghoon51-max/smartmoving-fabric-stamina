package com.yourpkg.smartmoving.state;

import net.minecraft.nbt.NbtCompound;

/** Server-authoritative player state for SmartMoving. */
public class PlayerContext {
    // 공개 필드(다른 mixin에서 바로 접근)
    public float  jumpCharge = 0f;    // 0..100 (파란 화살표)
    public float  grabEnergy = 100f;  // 0..100 (번개)
    public boolean grabbing  = false;
    public boolean crawling  = false;

    public NbtCompound toNbt() {
        var n = new NbtCompound();
        n.putFloat("jumpCharge", jumpCharge);
        n.putFloat("grabEnergy", grabEnergy);
        n.putBoolean("grabbing", grabbing);
        n.putBoolean("crawling", crawling);
        return n;
    }

    public void fromNbt(NbtCompound n) {
        if (n.contains("jumpCharge")) this.jumpCharge = n.getFloat("jumpCharge");
        if (n.contains("grabEnergy")) this.grabEnergy = n.getFloat("grabEnergy");
        if (n.contains("grabbing"))   this.grabbing   = n.getBoolean("grabbing");
        if (n.contains("crawling"))   this.crawling   = n.getBoolean("crawling");
    }

    /** Implement on the server player via mixin to expose the context. */
    public interface Holder {
        PlayerContext smartmoving$getContext();
    }
}
