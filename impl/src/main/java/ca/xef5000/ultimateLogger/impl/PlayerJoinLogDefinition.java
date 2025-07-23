package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinLogDefinition extends LogDefinition<PlayerJoinEvent> {
    @Override
    public String getId() {
        return "player_join";
    }

    @Override
    public Class<PlayerJoinEvent> getEventClass() {
        return PlayerJoinEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerJoinEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerJoinEvent event) {
        assert event.joinMessage() != null;
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("join_message", ((TextComponent)event.joinMessage()).content());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("join_message", "Join Message", ParameterType.STRING)
        );
    }
}

