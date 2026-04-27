package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.METHOD}) @Retention(RetentionPolicy.RUNTIME)
public @interface Unique {}
