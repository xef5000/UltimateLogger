package ca.xef5000.ultimateLogger.api;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class LogEntry {

    private final long id;
    private final String logType;
    private final Instant timestamp;
    private final LogData data;

    public LogEntry(long id, @NotNull String logType, @NotNull Instant timestamp, @NotNull LogData data) {
        this.id = id;
        this.logType = logType;
        this.timestamp = timestamp;
        this.data = data;
    }

    public long getId() { return id; }
    public String getLogType() { return logType; }
    public Instant getTimestamp() { return timestamp; }
    public LogData getData() { return data; }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", logType='" + logType + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data.toJson() +
                '}';
    }
}
