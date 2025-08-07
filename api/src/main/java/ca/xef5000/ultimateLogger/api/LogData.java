package ca.xef5000.ultimateLogger.api;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LogData {

    private static final Gson GSON = new Gson();

    private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    private long id;

    public LogData() {
    }

    /**
     * Puts a key-value pair into the data map.
     * @param key The data key (e.g., "player_name", "block_type").
     * @param value The data value.
     * @return The LogData instance for chaining.
     */
    public LogData put(@NotNull String key, @NotNull Object value) {
        data.put(key, value);
        return this;
    }

    public LinkedHashMap<String, Object> getData() {
        return data;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public String getString(@NotNull String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    @Nullable
    public UUID getUUID(@NotNull String key) {
        try {
            return UUID.fromString(getString(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    @Nullable
    public Integer getInteger(@NotNull String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    @Nullable
    public Double getDouble(@NotNull String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    /**
     * A very flexible way to store data is to serialize this object to JSON.
     * This method is a placeholder; you would use a library like Gson for the real implementation.
     * @return A JSON representation of the data map.
     */
    public String toJson() {
        // Use Gson to create a valid JSON string
        return GSON.toJson(this.data);
    }
}
