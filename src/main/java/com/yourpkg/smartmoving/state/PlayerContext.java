package com.yourpkg.smartmoving.state;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;

public class PlayerContext {
    public float   jumpCharge = 0f;     // 0~100: 충전 점프 게이지
    public float   grabEnergy = 100f;   // 0~100: 잡기/클라이밍 스태미나
    public boolean grabbing   = false;  // G 키 누름 상태
    public boolean crawling   = false;  // 기어가기 상태 (미래 구현)

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
        e = n.get("jumpCharge"); if (e instanceof NbtFloat  f) jumpCharge = f.floatValue();
        e = n.get("grabEnergy"); if (e instanceof NbtFloat  f) grabEnergy = f.floatValue();
        e = n.get("grabbing");   if (e instanceof NbtByte   b) grabbing   = b.byteValue() != 0;
        e = n.get("crawling");   if (e instanceof NbtByte   b) crawling   = b.byteValue() != 0;
    }

    public interface Holder {
        PlayerContext smartmoving$getContext();
    }
}
