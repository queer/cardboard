package gg.amy.mc.cardboard.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author amy
 * @since 1/9/20.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subcommand {
    /**
     * @return The name(s) of the subcommand. Required.
     */
    String[] value();
    
    /**
     * @return The description of the command. Optional.
     */
    String desc() default "A really cool command.";
    
    /**
     * @return The usage of the command. Optional.
     */
    String usage() default "No usage specified.";
    
    /**
     * @return The permission node of the command. Optional.
     */
    String permissionNode();
}