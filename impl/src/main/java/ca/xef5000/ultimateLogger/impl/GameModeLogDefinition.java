package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.List;

public class GameModeLogDefinition extends LogDefinition<PlayerGameModeChangeEvent> {
    @Override
    public String getId() {
        return "game_mode";
    }

    @Override
    public Class<PlayerGameModeChangeEvent> getEventClass() {
        return PlayerGameModeChangeEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerGameModeChangeEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(PlayerGameModeChangeEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("old_gamemode", event.getPlayer().getGameMode().toString())
                .put("new_gamemode", event.getNewGameMode().toString());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("old_gamemode", "Old Game Mode", ParameterType.STRING),
                new ParameterDefinition("new_gamemode", "New Game Mode", ParameterType.STRING)
        );
    }
}