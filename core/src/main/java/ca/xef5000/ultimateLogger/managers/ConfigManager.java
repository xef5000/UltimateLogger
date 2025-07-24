package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final FileConfiguration config;

    public ConfigManager(UltimateLogger plugin) {
        plugin.saveDefaultConfig(); // Copies config.yml if it doesn't exist
        this.config = plugin.getConfig();
    }

    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE").toUpperCase();
    }

    // SQLite settings
    public String getSQLiteFileName() {
        return config.getString("database.sqlite.filename", "logs.db");
    }

    // MySQL settings
    public String getMySQLHost() {
        return config.getString("database.mysql.host");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port");
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password");
    }

    // Database connection pool settings
    public int getDatabasePoolMaxSize() {
        return config.getInt("database.pool.max-size", 10);
    }

    // Log Manager cache settings
    public int getLogCacheMaxSize() {
        return config.getInt("log-manager.cache.max-size", 100);
    }

    public int getLogCacheExpiryMinutes() {
        return config.getInt("log-manager.cache.expiry-minutes", 5);
    }

    // Log Manager batch processing settings
    public int getLogBatchSize() {
        return config.getInt("log-manager.batch.size", 100);
    }

    public int getLogBatchInterval() {
        return config.getInt("log-manager.batch.interval", 100);
    }

    public Set<String> getDisabledLogTypes() {
        // getStringList returns an empty list if the path doesn't exist.
        List<String> disabledList = config.getStringList("log-manager.disabled-log-types");
        // Return it as a HashSet for efficient .contains() checks.
        return new HashSet<>(disabledList);
    }

    public Map<String, String> getWebhookUrls() {
        Map<String, String> webhookMap = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("log-manager.webhooks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String url = section.getString(key);
                if (url != null && !url.isEmpty()) {
                    webhookMap.put(key, url);
                }
            }
        }
        return webhookMap;
    }
}
