package ca.xef5000.ultimateLogger.managers;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.LogEntry;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Type;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LogManager {

    private static final Gson GSON = new Gson();

    private final UltimateLogger plugin;
    private final DatabaseManager dbManager;
    private final WebhookManager webhookManager;

    // A thread-safe queue for logs waiting to be saved.
    private final Queue<LogDataTuple> saveQueue = new ConcurrentLinkedQueue<>();

    private final Cache<CacheKey, List<LogEntry>> logCache;

    private final Map<String, LogDefinition<?>> logDefinitionMap = new ConcurrentHashMap<>();

    private final Set<String> disabledLogTypes;

    private final Map<String, List<ConfigManager.WebhookConfig>> webhookConfigs;

    private BukkitTask saveTask;

    public LogManager(UltimateLogger plugin, WebhookManager webhookManager, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.webhookManager = webhookManager;

        this.disabledLogTypes = plugin.getConfigManager().getDisabledLogTypes();
        this.webhookConfigs = plugin.getConfigManager().getWebhookConfigs();

        // Build a cache with size and expiry time from config
        this.logCache = CacheBuilder.newBuilder()
                .maximumSize(plugin.getConfigManager().getLogCacheMaxSize())
                .expireAfterAccess(plugin.getConfigManager().getLogCacheExpiryMinutes(), TimeUnit.MINUTES)
                .build();
    }

    public void initialize() {
        createTableIfNotExists();
        startSaveTask();
        startCleanupTask();
    }

    public void shutdown() {
        // Cancel the existing scheduled task if it's running
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }

        // Process any remaining logs in the queue
        if (!saveQueue.isEmpty()) {
            List<LogDataTuple> batch = new ArrayList<>();
            saveQueue.addAll(batch);
            if (!batch.isEmpty()) {
                try {
                    saveBatch(batch);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save final batch of logs during shutdown: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Dynamically registers a listener for a given LogDefinition.
     */
    public <T extends Event> void registerLogDefinition(LogDefinition<T> definition) {
        if (disabledLogTypes.contains(definition.getId())) {
            return; // Abort registration
        }

        plugin.getServer().getPluginManager().registerEvent(
                definition.getEventClass(),
                new Listener() {}, // Empty listener object
                EventPriority.MONITOR, // Use MONITOR to read event data after other plugins are done
                (listener, event) -> {
                    // Make sure the event is of the correct type and should be logged
                    if (definition.getEventClass().isInstance(event) && definition.shouldLog((T) event)) {
                        LogData data = definition.captureData((T) event);
                        queueLog(definition.getId(), data);
                    }
                },
                plugin
        );
        logDefinitionMap.put(definition.getId(), definition);
        plugin.getLogger().info("Registered listener for LogDefinition: " + definition.getId());
    }

    private void queueLog(String logType, LogData data) {
        if (dbManager != null && dbManager.isOperational()) {
            saveQueue.add(new LogDataTuple(logType, data, logID -> {
                List<ConfigManager.WebhookConfig> configs = webhookConfigs.get(logType);
                if (configs != null) {
                    for (ConfigManager.WebhookConfig config : configs) {
                        data.setId(logID);
                        webhookManager.sendWebhook(
                                config.url(),
                                logType,
                                data,
                                config.conditions()
                        );
                    }
                }
            }));
        }
    }

    private void startSaveTask() {
        this.saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Process the queue in batches to be efficient
                List<LogDataTuple> batch = new ArrayList<>();
                int batchSize = plugin.getConfigManager().getLogBatchSize();
                while (!saveQueue.isEmpty() && batch.size() < batchSize) { // Process up to configured batch size
                    batch.add(saveQueue.poll());
                }

                if (!batch.isEmpty()) {
                    saveBatch(batch);
                }
            }
        }.runTaskTimerAsynchronously(plugin, plugin.getConfigManager().getLogBatchInterval(), plugin.getConfigManager().getLogBatchInterval()); // Run at configured interval
    }

    private void startCleanupTask() {
        int retentionDays = plugin.getConfigManager().getRetentionPeriodDays();
        if (retentionDays <= 0) {
            plugin.getLogger().info("Log retention is disabled. Cleanup task will not run.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanDatabase().thenAccept(
                        deletedCount -> plugin.getLogger().info("Cleaned up and deleted " + deletedCount + " expired logs.")
                );
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, plugin.getConfigManager().getCleanupIntervalMinutes());
    }

    /**
     * Asynchronously deletes all logs that are not archived and have expired.
     * @return A CompletableFuture that completes when the deletion is done.
     */
    public CompletableFuture<Integer> cleanDatabase() {
        plugin.getLogger().info("Running log cleanup task...");
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM ultimate_logs WHERE is_archived = ? AND expires_at < ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setBoolean(1, false);
                pstmt.setLong(2, Instant.now().toEpochMilli());
                int deletedRows = pstmt.executeUpdate();

                if (deletedRows > 0) {
                    plugin.getLogger().info("Cleaned up and deleted " + deletedRows + " expired logs.");
                    logCache.invalidateAll();
                }

                return deletedRows;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    /**
     * Clears logs matching the specified filter and conditions.
     * @param filter The log type filter
     * @param conditions Additional filter conditions
     * @return A CompletableFuture with the number of logs deleted
     */
    public CompletableFuture<Integer> clearLogs(String filter, List<FilterCondition> conditions) {
        return CompletableFuture.supplyAsync(() -> {
            List<Object> params = new ArrayList<>();
            StringBuilder sqlBuilder = new StringBuilder("DELETE FROM ultimate_logs WHERE ");

            if (filter != null && !filter.isEmpty()) {
                sqlBuilder.append("log_type = ? ");
                params.add(filter);
            } else {
                sqlBuilder.append("1=1 ");
            }

            if (conditions != null && !conditions.isEmpty()) {
                for (FilterCondition condition : conditions) {
                    String key = condition.key();
                    String comparator = condition.comparator();
                    Object value = condition.value();

                    switch (comparator) {
                        case "=" -> sqlBuilder.append("AND json_extract(data, '$.").append(key).append("') = ? ");
                        case "!=" -> sqlBuilder.append("AND json_extract(data, '$.").append(key).append("') != ? ");
                        case ">" -> sqlBuilder.append("AND CAST(json_extract(data, '$.").append(key).append("') AS NUMERIC) > ? ");
                        case "<" -> sqlBuilder.append("AND CAST(json_extract(data, '$.").append(key).append("') AS NUMERIC) < ? ");
                        case ">=" -> sqlBuilder.append("AND CAST(json_extract(data, '$.").append(key).append("') AS NUMERIC) >= ? ");
                        case "<=" -> sqlBuilder.append("AND CAST(json_extract(data, '$.").append(key).append("') AS NUMERIC) <= ? ");
                        case "startswith" -> {
                            sqlBuilder.append("AND json_extract(data, '$.").append(key).append("') LIKE ? ");
                            value = value.toString() + "%";
                        }
                        case "endswith" -> {
                            sqlBuilder.append("AND json_extract(data, '$.").append(key).append("') LIKE ? ");
                            value = "%" + value.toString();
                        }
                        case "contains" -> {
                            sqlBuilder.append("AND json_extract(data, '$.").append(key).append("') LIKE ? ");
                            value = "%" + value.toString() + "%";
                        }
                        default -> {
                            continue;
                        }
                    }
                    params.add(value);
                }
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

                // Set parameters
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }

                int deletedCount = pstmt.executeUpdate();

                // Invalidate cache since data has changed
                logCache.invalidateAll();

                plugin.getLogger().info("Cleared " + deletedCount + " logs with filter: " +
                        filter + " and " + conditions.size() + " conditions.");

                return deletedCount;
            } catch (SQLException e) {
                plugin.getLogger().severe("Error clearing logs: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to clear logs", e);
            }
        });
    }

    private void saveBatch(List<LogDataTuple> batch) {
        final String sql = "INSERT INTO ultimate_logs (log_type, timestamp, is_archived, expires_at, data) VALUES (?, ?, ?, ?, ?)";

        long now = Instant.now().toEpochMilli();
        long retentionMillis = TimeUnit.DAYS.toMillis(plugin.getConfigManager().getRetentionPeriodDays());

        try (Connection conn = dbManager.getConnection()) {
            int successCount = 0;

            for (LogDataTuple tuple : batch) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, tuple.logType());
                    pstmt.setLong(2, now);
                    pstmt.setBoolean(3, false);

                    if (retentionMillis > 0) {
                        pstmt.setLong(4, now + retentionMillis);
                    } else {
                        pstmt.setNull(4, Types.BIGINT);
                    }

                    pstmt.setString(5, tuple.data().toJson());
                    pstmt.executeUpdate(); // Execute single update

                    // Get the generated ID
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        Consumer<Long> callback = tuple.onSaveComplete();
                        if (callback != null) {
                            callback.accept(id);
                            successCount++;
                        }
                    }
                }
            }

            logCache.invalidateAll();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save log batch to the database!");
            e.printStackTrace();
        }
    }

    /**
     * Asynchronously fetches a page of logs. Checks cache first.
     */
    public CompletableFuture<List<LogEntry>> getLogsPage(int page, int pageSize, String filter, List<FilterCondition> advancedFilters) {
        int internalPage = Math.max(0, page - 1);
        CacheKey key = new CacheKey(internalPage, filter, advancedFilters);

        List<LogEntry> cachedPage = logCache.getIfPresent(key);
        if (cachedPage != null) {
            return CompletableFuture.completedFuture(cachedPage);
        }

        final CompletableFuture<List<LogEntry>> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                List<LogEntry> logs = new ArrayList<>();
                List<Object> params = new ArrayList<>();

                // **FIX 1: Update the SELECT statement to include the new 'is_archived' column**
                StringBuilder sqlBuilder = new StringBuilder("SELECT id, log_type, timestamp, is_archived, data FROM ultimate_logs ");

                if (filter != null && !filter.trim().isEmpty()) {
                    sqlBuilder.append("WHERE log_type = ? ");
                    params.add(filter);
                } else {
                    sqlBuilder.append("WHERE 1=1 "); // Start a WHERE clause if there's no type filter
                }

                if (advancedFilters != null) {
                    for (FilterCondition condition : advancedFilters) {
                        String comparator = condition.comparator();
                        if (List.of("=", "!=", ">", "<", ">=", "<=").contains(comparator)) {
                            sqlBuilder.append(String.format("AND json_extract(data, '$.%s') %s ? ", condition.key(), comparator));
                            params.add(condition.value());
                        } else if ("startswith".equals(comparator)) {
                            sqlBuilder.append(String.format("AND json_extract(data, '$.%s') LIKE ? ", condition.key()));
                            params.add(condition.value() + "%");
                        } else if ("endswith".equals(comparator)) {
                            sqlBuilder.append(String.format("AND json_extract(data, '$.%s') LIKE ? ", condition.key()));
                            params.add("%" + condition.value());
                        }
                    }
                }

                // This part of the query building is fine
                sqlBuilder.append("ORDER BY id DESC LIMIT ? OFFSET ?");
                int offset = (page - 1) * pageSize;
                String finalSql = sqlBuilder.toString().replace("LIMIT ? OFFSET ?", "LIMIT " + pageSize + " OFFSET " + offset);
                // We revert to string formatting for LIMIT/OFFSET due to JDBC limitations.

                try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(finalSql)) {
                    // Set only the parameters for the WHERE clauses
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }

                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        String jsonString = rs.getString("data");
                        LogData data = new LogData();
                        try {
                            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                            Map<String, Object> parsedMap = GSON.fromJson(jsonString, mapType);
                            if (parsedMap != null) parsedMap.forEach(data::put);
                        } catch (Exception e) {
                            data.put("parsing_failed", "true");
                        }

                        // **FIX 2: Call the new LogEntry constructor with all 5 parameters**
                        logs.add(new LogEntry(
                                rs.getLong("id"),
                                rs.getString("log_type"),
                                Instant.ofEpochMilli(rs.getLong("timestamp")),
                                rs.getBoolean("is_archived"), // Provide the new value
                                data
                        ));
                    }

                    logCache.put(key, logs);
                    future.complete(logs);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to load logs from database:");
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);

        return future;
    }

    private void createTableIfNotExists() {
        boolean isMySql = dbManager.getDataSource().getJdbcUrl().contains("mysql");
        String sql;

        if (isMySql) {
            sql = "CREATE TABLE IF NOT EXISTS ultimate_logs (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "log_type VARCHAR(255) NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "is_archived BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "expires_at BIGINT, " +
                    "data JSON NOT NULL);";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS ultimate_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "log_type TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "is_archived INTEGER NOT NULL DEFAULT 0, " +
                    "expires_at INTEGER, " +
                    "data TEXT NOT NULL);";
        }

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create 'ultimate_logs' table!");
            e.printStackTrace();
        }
    }

    /**
     * Asynchronously fetches a single log by its unique ID.
     * @param logId The ID of the log to fetch.
     * @return A CompletableFuture containing an Optional of the LogEntry, or empty if not found.
     */
    public CompletableFuture<Optional<LogEntry>> getLogById(long logId) {
        return CompletableFuture.supplyAsync(() -> {
            // Select all columns for the specific log ID
            String sql = "SELECT * FROM ultimate_logs WHERE id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, logId);
                ResultSet rs = pstmt.executeQuery();

                // Use if, as we only expect one result
                if (rs.next()) {
                    // This is the complete parsing logic
                    String jsonString = rs.getString("data");
                    LogData data = new LogData();
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                        Map<String, Object> parsedMap = GSON.fromJson(jsonString, mapType);
                        if (parsedMap != null) parsedMap.forEach(data::put);
                    } catch (Exception e) {
                        data.put("parsing_failed", "true");
                    }

                    // Construct the LogEntry with all 5 required parameters
                    LogEntry entry = new LogEntry(
                            rs.getLong("id"),
                            rs.getString("log_type"),
                            Instant.ofEpochMilli(rs.getLong("timestamp")),
                            rs.getBoolean("is_archived"), // Get the new boolean value
                            data
                    );
                    return Optional.of(entry);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load log by ID: " + logId);
                e.printStackTrace();
            }
            // Return empty if not found or if an error occurred
            return Optional.empty();
        });
    }

    public CompletableFuture<Void> setLogArchivedStatus(long logId, boolean archived) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE ultimate_logs SET is_archived = ?, expires_at = ? WHERE id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, archived);
                if (archived) {
                    pstmt.setNull(2, Types.BIGINT); // Archived logs never expire
                } else {
                    // Reset the expiration timer from NOW
                    long retentionMillis = TimeUnit.DAYS.toMillis(plugin.getConfigManager().getRetentionPeriodDays());
                    pstmt.setLong(2, Instant.now().toEpochMilli() + retentionMillis);
                }
                pstmt.setLong(3, logId);
                pstmt.executeUpdate();
                logCache.invalidateAll(); // Invalidate cache since data changed
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    /**
     * Asynchronously deletes a log by its unique ID.
     * @param logId The ID of the log to delete.
     * @return A CompletableFuture that completes when the deletion is done.
     */
    public CompletableFuture<Boolean> deleteLogById(long logId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM ultimate_logs WHERE id = ?";
            try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, logId);
                int rowsAffected = pstmt.executeUpdate();
                logCache.invalidateAll(); // Invalidate cache since data changed
                return rowsAffected > 0; // Return true if at least one row was deleted
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete log with ID: " + logId);
                e.printStackTrace();
                return false;
            }
        });
    }

    public List<String> getDistinctLogTypes() {
        return logDefinitionMap.values().stream()
                .map(LogDefinition::getId)
                .sorted()
                .collect(Collectors.toList());
    }

    public int getSaveQueueSize() {
        return saveQueue.size();
    }

    public long getCacheSize() {
        return logCache.size();
    }

    public LogDefinition<?> getLogDefinition(String id) {
        return logDefinitionMap.get(id);
    }

    public void invalidateCache() {
        logCache.invalidateAll();
    }

    private record LogDataTuple(String logType, LogData data, Consumer<Long> onSaveComplete) {}

    private record CacheKey(int page, String filter, List<FilterCondition> advancedFilters) {}
}
