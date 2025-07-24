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

import java.lang.reflect.Type;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LogManager {

    private static final Gson GSON = new Gson();

    private final UltimateLogger plugin;
    private final DatabaseManager dbManager;

    // A thread-safe queue for logs waiting to be saved.
    private final Queue<LogDataTuple> saveQueue = new ConcurrentLinkedQueue<>();

    private final Cache<CacheKey, List<LogEntry>> logCache;

    private final Map<String, LogDefinition<?>> logDefinitionMap = new ConcurrentHashMap<>();

    private final Set<String> disabledLogTypes;

    public LogManager(UltimateLogger plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;

        this.disabledLogTypes = plugin.getConfigManager().getDisabledLogTypes();

        // Build a cache with size and expiry time from config
        this.logCache = CacheBuilder.newBuilder()
                .maximumSize(plugin.getConfigManager().getLogCacheMaxSize())
                .expireAfterAccess(plugin.getConfigManager().getLogCacheExpiryMinutes(), TimeUnit.MINUTES)
                .build();
    }

    public void initialize() {
        createTableIfNotExists();
        startSaveTask();
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
        saveQueue.add(new LogDataTuple(logType, data));
    }

    private void startSaveTask() {
        new BukkitRunnable() {
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

    private void saveBatch(List<LogDataTuple> batch) {
        final String sql = "INSERT INTO ultimate_logs (log_type, timestamp, data) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (LogDataTuple tuple : batch) {
                pstmt.setString(1, tuple.logType());
                pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
                pstmt.setString(3, tuple.data().toJson()); // We'll store the data as JSON text
                pstmt.addBatch();
            }
            pstmt.executeBatch();
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
        // Page numbers are 1-based for users, but 0-based for calculations
        int internalPage = Math.max(0, page - 1);
        CacheKey key = new CacheKey(internalPage, filter, advancedFilters);

        // Check cache first
        List<LogEntry> cachedPage = logCache.getIfPresent(key);
        if (cachedPage != null) {
            return CompletableFuture.completedFuture(cachedPage);
        }

        final CompletableFuture<List<LogEntry>> future = new CompletableFuture<>();

        // Run the database query on an async thread using the Bukkit scheduler.
        new BukkitRunnable() {
            @Override
            public void run() {
                List<LogEntry> logs = new ArrayList<>();
                List<Object> params = new ArrayList<>();
                StringBuilder sqlBuilder = new StringBuilder("SELECT id, log_type, timestamp, data FROM ultimate_logs ");

                // Dynamically add a WHERE clause if a filter is provided
                if (filter != null && !filter.trim().isEmpty()) {
                    sqlBuilder.append("WHERE log_type = ? ");
                    params.add(filter);
                }

                if (advancedFilters != null) {
                    for (FilterCondition condition : advancedFilters) {
                        // IMPORTANT: We must validate the comparator to prevent SQL injection
                        if (!List.of("=", "!=", ">", "<", ">=", "<=").contains(condition.comparator())) {
                            continue; // Skip invalid comparators
                        }
                        // The '$.' prefix is how you query the root of a JSON object.
                        sqlBuilder.append(String.format("AND json_extract(data, '$.%s') %s ? ", condition.key(), condition.comparator()));
                        params.add(condition.value());
                    }
                }

                sqlBuilder.append("ORDER BY id DESC LIMIT ? OFFSET ?");
                params.add(pageSize);
                params.add((page - 1) * pageSize);
                try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }

                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        String jsonString = rs.getString("data");
                        LogData data = new LogData();
                        try {
                            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                            Map<String, Object> parsedMap = new Gson().fromJson(jsonString, mapType);
                            if (parsedMap != null) parsedMap.forEach(data::put);
                        } catch (Exception e) {
                            data.put("parsing_failed", "true");
                        }
                        logs.add(new LogEntry(rs.getLong("id"), rs.getString("log_type"), Instant.ofEpochMilli(rs.getLong("timestamp")), data));
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
            sql = "CREATE TABLE IF NOT EXISTS ultimate_logs (id BIGINT PRIMARY KEY AUTO_INCREMENT, log_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, data JSON NOT NULL);";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS ultimate_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, log_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, data TEXT NOT NULL);";
        }

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create 'ultimate_logs' table!");
            e.printStackTrace();
        }
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

    private record LogDataTuple(String logType, LogData data) {}

    private record CacheKey(int page, String filter, List<FilterCondition> advancedFilters) {}
}
