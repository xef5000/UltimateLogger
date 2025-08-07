package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import ca.xef5000.ultimateLogger.utils.FilterSerializer;
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
        return config.getInt("logs.cache.max-size", 100);
    }

    public int getLogCacheExpiryMinutes() {
        return config.getInt("logs.cache.expiry-minutes", 5);
    }

    // Log Manager batch processing settings
    public int getLogBatchSize() {
        return config.getInt("logs.batch.size", 100);
    }

    public int getLogBatchInterval() {
        return config.getInt("logs.batch.interval", 100);
    }

    public Set<String> getDisabledLogTypes() {
        // getStringList returns an empty list if the path doesn't exist.
        List<String> disabledList = config.getStringList("logs.disabled-log-types");
        // Return it as a HashSet for efficient .contains() checks.
        return new HashSet<>(disabledList);
    }

    public Map<String, WebhookConfig> getWebhookConfigs() {
        Map<String, WebhookConfig> configs = new HashMap<>();
        ConfigurationSection webhooksSection = config.getConfigurationSection("logs.webhooks");

        if (webhooksSection != null) {
            for (String logType : webhooksSection.getKeys(false)) {
                String url = config.getString("logs.webhooks." + logType + ".url");
                String conditionStr = config.getString("logs.webhooks." + logType + ".condition");

                List<FilterCondition> conditions = new ArrayList<>();
                if (conditionStr != null && !conditionStr.isEmpty()) {
                    Map.Entry<String, List<FilterCondition>> parsed = FilterSerializer.deserialize(";" + conditionStr);
                    conditions = parsed.getValue();
                }

                configs.put(logType, new WebhookConfig(url, conditions));
            }
        }

        return configs;
    }
    public int getRetentionPeriodDays() {
        return config.getInt("logs.retention-period-days", 30);
    }

    public long getCleanupIntervalMinutes() {
        // Return ticks for the Bukkit scheduler (minutes * 60 * 20)
        return config.getLong("logs.cleanup-interval-minutes", 60) * 60 * 20;
    }

    public record WebhookConfig(String url, List<FilterCondition> conditions) {}
}
