package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import org.bukkit.configuration.file.FileConfiguration;

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
}
