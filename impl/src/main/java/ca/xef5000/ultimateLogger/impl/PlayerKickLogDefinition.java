package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.List;

public class PlayerKickLogDefinition extends LogDefinition<PlayerKickEvent> {
    @Override
    public String getId() {
        return "player_kick";
    }

    @Override
    public Class<PlayerKickEvent> getEventClass() {
        return PlayerKickEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerKickEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerKickEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("kick_reason", ((TextComponent) event.reason()).content());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("kick_reason", "Kick Reason", ParameterType.STRING)
        );
    }
}