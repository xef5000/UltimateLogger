package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.List;

public class ItemDropLogDefinition extends LogDefinition<PlayerDropItemEvent> {
    @Override
    public String getId() {
        return "item_drop";
    }

    @Override
    public Class<PlayerDropItemEvent> getEventClass() {
        return PlayerDropItemEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerDropItemEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(PlayerDropItemEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("item_type", event.getItemDrop().getItemStack().getType().toString())
                .put("item_amount", event.getItemDrop().getItemStack().getAmount())
                .put("location_world", event.getItemDrop().getLocation().getWorld().getName())
                .put("location_x", event.getItemDrop().getLocation().getBlockX())
                .put("location_y", event.getItemDrop().getLocation().getBlockY())
                .put("location_z", event.getItemDrop().getLocation().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("item_type", "Item Type", ParameterType.STRING),
                new ParameterDefinition("item_amount", "Item Amount", ParameterType.INTEGER),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING)
        );
    }
}