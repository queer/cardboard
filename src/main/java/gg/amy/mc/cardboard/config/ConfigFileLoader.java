package gg.amy.mc.cardboard.config;

import gg.amy.mc.cardboard.Cardboard;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 1/12/20.
 */
public class ConfigFileLoader {
    private final Cardboard cardboard;
    private final Map<String, YamlConfiguration> configs = new HashMap<>();
    
    public ConfigFileLoader(final Cardboard cardboard) {
        this.cardboard = cardboard;
    }
    
    public ConfigurationSection loadFile(final String path) {
        return configs.computeIfAbsent(path, __ -> {
            final File file = new File(cardboard.getDataFolder(), path);
            return YamlConfiguration.loadConfiguration(file);
        });
    }
}
