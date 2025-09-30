package com.yourpkg.smartmoving.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityStamina {
    // 여기에 나중에 스태미너 관련 필드/메소드, @Inject 같은 Mixin 코드가 들어갑니다.
}
