package gg.amy.mc.cardboard.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows injecting a Bukkit plugin by name. If such a plugin is not loaded,
 * the injector will throw.
 *
 * @author amy
 * @since 1/12/20.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BukkitPlugin {
    String value();
}
