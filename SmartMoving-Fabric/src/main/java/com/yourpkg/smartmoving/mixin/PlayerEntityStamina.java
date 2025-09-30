package com.yourpkg.smartmoving.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityStamina {
    // 이후 @Inject(method = "tick", ...) 등으로 스태미너 로직을 추가할 수 있습니다.
}
