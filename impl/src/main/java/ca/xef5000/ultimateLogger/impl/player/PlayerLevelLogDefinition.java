package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.util.List;

public class PlayerLevelLogDefinition extends LogDefinition<PlayerLevelChangeEvent> {
    @Override
    public String getId() {
        return "player_level";
    }

    @Override
    public Class<PlayerLevelChangeEvent> getEventClass() {
        return PlayerLevelChangeEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerLevelChangeEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerLevelChangeEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("old_level", event.getOldLevel())
                .put("new_level", event.getNewLevel());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("old_level", "Old Level", ParameterType.INTEGER),
                new ParameterDefinition("new_level", "New Level", ParameterType.INTEGER)
        );
    }
}