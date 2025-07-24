package ca.xef5000.ultimateLogger.impl.action;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.List;

public class ItemPickupLogDefinition extends LogDefinition<EntityPickupItemEvent> {
    @Override
    public String getId() {
        return "item_pickup";
    }

    @Override
    public Class<EntityPickupItemEvent> getEventClass() {
        return EntityPickupItemEvent.class;
    }

    @Override
    public boolean shouldLog(EntityPickupItemEvent event) {
        return !event.isCancelled() && event.getEntity() instanceof org.bukkit.entity.Player;
    }

    @Override
    public LogData captureData(EntityPickupItemEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getEntity();
        return new LogData()
                .put("player_uuid", player.getUniqueId().toString())
                .put("player_name", player.getName())
                .put("item_type", event.getItem().getItemStack().getType().toString())
                .put("item_amount", event.getItem().getItemStack().getAmount())
                .put("location_world", event.getItem().getLocation().getWorld().getName())
                .put("location_x", event.getItem().getLocation().getBlockX())
                .put("location_y", event.getItem().getLocation().getBlockY())
                .put("location_z", event.getItem().getLocation().getBlockZ());
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