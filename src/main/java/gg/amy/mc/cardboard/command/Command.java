package gg.amy.mc.cardboard.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author amy
 * @since 1/9/20.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * @return The name of the command. Required.
     */
    String name();
    
    /**
     * @return The aliases of the command. Optional.
     */
    String[] aliases() default {};
    
    /**
     * @return The description of the command. Optional.
     */
    String desc() default "A really cool command.";
    
    /**
     * @return The usage of the command. Optional.
     */
    String usage() default "No usage specified.";
    
    /**
     * @return The label of the command. Optional.
     */
    String label() default "cardboard";
    
    /**
     * @return The permission node of the command. Required.
     */
    String permissionNode();
    
    /**
     * @return The no-permission message of the command. Optional.
     */
    String permissionMessage() default "You don't have permission to do that!";
}