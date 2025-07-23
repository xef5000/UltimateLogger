package ca.xef5000.ultimateLogger.api;

import org.bukkit.event.Event;

import java.util.List;

public abstract class LogDefinition<T extends Event> {

    public abstract String getId();

    public abstract Class<T> getEventClass();

    public abstract boolean shouldLog(T event);

    public abstract LogData captureData(T event);

    public abstract List<ParameterDefinition> getFilterableParameters();
}
