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

    // In ConfigManager.java, update getWebhookConfigs()
    public Map<String, List<WebhookConfig>> getWebhookConfigs() {
        Map<String, List<WebhookConfig>> result = new HashMap<>();

        // Try the new format first (root level webhooks)
        ConfigurationSection webhooksSection = config.getConfigurationSection("webhooks");

        // Fall back to old format if new format isn't found
        if (webhooksSection == null) {
            webhooksSection = config.getConfigurationSection("logs.webhooks");
        }

        if (webhooksSection == null) return result;

        for (String webhookKey : webhooksSection.getKeys(false)) {
            ConfigurationSection webhook = webhooksSection.getConfigurationSection(webhookKey);
            String type = null;
            String url = null;
            String conditionStr = null;

            if (webhook != null) {
                // Try the new format with explicit "type" field
                type = webhook.getString("type");
                url = webhook.getString("url");
                conditionStr = webhook.getString("condition");
            } else {
                // Try the old format where the key is the type
                type = webhookKey;
                url = webhooksSection.getString(webhookKey + ".url");
                conditionStr = webhooksSection.getString(webhookKey + ".condition");
            }

            if (type == null || url == null) continue;

            List<FilterCondition> conditions = new ArrayList<>();
            if (conditionStr != null && !conditionStr.isEmpty()) {
                Map.Entry<String, List<FilterCondition>> parsed = FilterSerializer.deserialize(";" + conditionStr);
                conditions = parsed.getValue();
            }

            WebhookConfig config = new WebhookConfig(url, conditions, type);

            // Group configs by their log type
            result.computeIfAbsent(type, k -> new ArrayList<>()).add(config);
        }

        return result;
    }
    public int getRetentionPeriodDays() {
        return config.getInt("logs.retention-period-days", 30);
    }

    public long getCleanupIntervalMinutes() {
        // Return ticks for the Bukkit scheduler (minutes * 60 * 20)
        return config.getLong("logs.cleanup-interval-minutes", 60) * 60 * 20;
    }

    public record WebhookConfig(String url, List<FilterCondition> conditions, String type) {}
}
