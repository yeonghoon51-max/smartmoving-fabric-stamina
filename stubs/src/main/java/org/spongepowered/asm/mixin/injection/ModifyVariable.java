package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
public @interface ModifyVariable {
    String method();
    At at();
    boolean argsOnly() default false;
    int ordinal() default -1;
    int index() default -1;
}
