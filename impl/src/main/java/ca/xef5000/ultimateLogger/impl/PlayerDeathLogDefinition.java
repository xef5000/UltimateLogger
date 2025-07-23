package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class PlayerDeathLogDefinition extends LogDefinition<PlayerDeathEvent> {
    @Override
    public String getId() {
        return "player_death";
    }

    @Override
    public Class<PlayerDeathEvent> getEventClass() {
        return PlayerDeathEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerDeathEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerDeathEvent event) {
        assert event.deathMessage() != null;
        return new LogData()
                .put("player_uuid", event.getEntity().getUniqueId().toString())
                .put("player_name", event.getEntity().getName())
                .put("death_message", ((TextComponent)event.deathMessage()).content())
                .put("location_world", event.getEntity().getLocation().getWorld().getName())
                .put("location_x", event.getEntity().getLocation().getBlockX())
                .put("location_y", event.getEntity().getLocation().getBlockY())
                .put("location_z", event.getEntity().getLocation().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("location_x", "Location X", ParameterType.INTEGER),
                new ParameterDefinition("location_y", "Location Y", ParameterType.INTEGER),
                new ParameterDefinition("location_z", "Location Z", ParameterType.INTEGER),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING),
                new ParameterDefinition("death_message", "Death Message", ParameterType.STRING)
        );
    }
}

