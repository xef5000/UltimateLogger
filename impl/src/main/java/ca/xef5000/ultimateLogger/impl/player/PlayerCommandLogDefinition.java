package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class PlayerCommandLogDefinition extends LogDefinition<PlayerCommandPreprocessEvent> {
    @Override
    public String getId() {
        return "player_command";
    }

    @Override
    public Class<PlayerCommandPreprocessEvent> getEventClass() {
        return PlayerCommandPreprocessEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerCommandPreprocessEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerCommandPreprocessEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("command", event.getMessage());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("command", "Command", ParameterType.STRING)
        );
    }
}

