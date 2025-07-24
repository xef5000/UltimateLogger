package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class PlayerTeleportLogDefinition extends LogDefinition<PlayerTeleportEvent> {
    @Override
    public String getId() {
        return "player_teleport";
    }

    @Override
    public Class<PlayerTeleportEvent> getEventClass() {
        return PlayerTeleportEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerTeleportEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(PlayerTeleportEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("teleport_cause", event.getCause().toString())
                .put("from_world", event.getFrom().getWorld().getName())
                .put("from_x", event.getFrom().getBlockX())
                .put("from_y", event.getFrom().getBlockY())
                .put("from_z", event.getFrom().getBlockZ())
                .put("to_world", event.getTo().getWorld().getName())
                .put("to_x", event.getTo().getBlockX())
                .put("to_y", event.getTo().getBlockY())
                .put("to_z", event.getTo().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("teleport_cause", "Teleport Cause", ParameterType.STRING),
                new ParameterDefinition("from_world", "From World", ParameterType.STRING),
                new ParameterDefinition("to_world", "To World", ParameterType.STRING)
        );
    }
}