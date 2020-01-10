package gg.amy.mc.cardboard.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * @author amy
 * @since 1/10/20.
 */
public final class MessageUtil {
    private MessageUtil() {
    }
    
    public static void sendMessage(final CommandSender target, final String... messages) {
        for(final String message : messages) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
