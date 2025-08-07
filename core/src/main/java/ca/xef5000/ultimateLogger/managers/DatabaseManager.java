package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final UltimateLogger plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public DatabaseManager(UltimateLogger plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupDataSource();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        if (configManager.getDatabaseType().equals("MYSQL")) {
            plugin.getLogger().info("Database type set to MySQL. Connecting...");
            config.setJdbcUrl("jdbc:mysql://" + configManager.getMySQLHost() + ":" + configManager.getMySQLPort() + "/" + configManager.getMySQLDatabase());
            config.setUsername(configManager.getMySQLUsername());
            config.setPassword(configManager.getMySQLPassword());
        } else { // Default to SQLite
            plugin.getLogger().info("Database type set to SQLite. Creating file...");
            // Ensure the file exists
            File dbFile = new File(plugin.getDataFolder(), configManager.getSQLiteFileName());
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create SQLite database file!");
                    e.printStackTrace();
                    return;
                }
            }
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }

        config.setMaximumPoolSize(configManager.getDatabasePoolMaxSize()); // Pool size from config
        config.setConnectionTestQuery("SELECT 1");

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean isOperational() {
        return dataSource != null && !dataSource.isClosed();
    }
}
