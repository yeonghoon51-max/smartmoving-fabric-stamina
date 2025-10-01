package com.yourpkg.smartmoving.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * PlayerContext - 각 플레이어의 스태미너, 점프 차징, 등반/수영 상태를 서버 권위로 관리
 */
public class PlayerContext {

    private float stamina = 100f;       // 전기 아이콘: 기본 스태미너
    private float maxStamina = 100f;
    private float jumpCharge = 0f;      // 파란 화살표: 웅크리기 슈퍼점프 차징
    private boolean climbing = false;
    private boolean swimming = false;

    public void tick(ServerPlayerEntity player) {
        // 기본 스태미너 회복 로직
        if (!player.isSprinting() && !climbing && !swimming) {
            stamina = Math.min(maxStamina, stamina + 0.1f);
        }

        // 점프 차징 (웅크리기)
        if (player.isSneaking()) {
            jumpCharge = Math.min(1f, jumpCharge + 0.02f);
        } else {
            jumpCharge = 0f;
        }

        // 행동 시 스태미너 소모
        if (player.isSprinting()) {
            stamina = Math.max(0, stamina - 0.2f);
        }
        if (climbing) {
            stamina = Math.max(0, stamina - 0.3f);
        }
        if (swimming) {
            stamina = Math.max(0, stamina - 0.25f);
        }
    }

    // === Getter / Setter ===
    public float getStamina() { return stamina; }
    public float getJumpCharge() { return jumpCharge; }
    public boolean isClimbing() { return climbing; }
    public void setClimbing(boolean climbing) { this.climbing = climbing; }
    public boolean isSwimming() { return swimming; }
    public void setSwimming(boolean swimming) { this.swimming = swimming; }

    // === NBT 저장/로드 (서버 → 클라 동기화용) ===
    public void writeNbt(NbtCompound nbt) {
        nbt.putFloat("Stamina", stamina);
        nbt.putFloat("JumpCharge", jumpCharge);
        nbt.putBoolean("Climbing", climbing);
        nbt.putBoolean("Swimming", swimming);
    }

    public void readNbt(NbtCompound nbt) {
        stamina = nbt.getFloat("Stamina");
        jumpCharge = nbt.getFloat("JumpCharge");
        climbing = nbt.getBoolean("Climbing");
        swimming = nbt.getBoolean("Swimming");
    }

    // === Holder 인터페이스 (mixin에서 사용) ===
    public interface Holder {
        PlayerContext smartmoving$getContext();
    }
}
