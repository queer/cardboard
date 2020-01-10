package gg.amy.mc.cardboard.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

/**
 * @author amy
 * @since 1/9/20.
 */
public final class BukkitCommandInjector {
    private final CommandMap commandMap;
    
    public BukkitCommandInjector() {
        try {
            final Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch(final IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void register(final org.bukkit.command.Command command) {
        commandMap.register(command.getName(), command.getLabel(), command);
    }
}
