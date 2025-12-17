package org.spongepowered.asm.mixin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Repeatable(Mixins.class)
public @interface Mixin {
    Class<?>[] value() default {};
    String[] targets() default {};
    int priority() default 1000;
    boolean remap() default true;
    String[] constraints() default {};
}
