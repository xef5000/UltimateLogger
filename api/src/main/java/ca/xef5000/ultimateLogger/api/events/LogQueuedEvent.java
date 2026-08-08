package ca.xef5000.ultimateLogger.api.events;

import ca.xef5000.ultimateLogger.api.LogData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LogQueuedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    private final String logType;
    private final LogData data;

    public LogQueuedEvent(String logType, LogData data) {
        this.logType = logType;
        this.data = data;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public String getLogType() {
        return logType;
    }

    public LogData getData() {
        return data;
    }
}
