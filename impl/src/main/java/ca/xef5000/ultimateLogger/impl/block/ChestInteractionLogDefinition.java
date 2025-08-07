package ca.xef5000.ultimateLogger.impl.block;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import ca.xef5000.ultimateLogger.utils.ComponentUtils;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;

public class ChestInteractionLogDefinition extends LogDefinition<InventoryOpenEvent> {
    @Override
    public String getId() {
        return "chest_interaction";
    }

    @Override
    public Class<InventoryOpenEvent> getEventClass() {
        return InventoryOpenEvent.class;
    }

    @Override
    public boolean shouldLog(InventoryOpenEvent event) {
        return !event.isCancelled() && 
               event.getPlayer() instanceof org.bukkit.entity.Player &&
               (event.getInventory().getType().toString().contains("CHEST") ||
                event.getInventory().getType().toString().contains("BARREL") ||
                event.getInventory().getType().toString().contains("SHULKER"));
    }

    @Override
    public LogData captureData(InventoryOpenEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getPlayer();
        return new LogData()
                .put("player_uuid", player.getUniqueId().toString())
                .put("player_name", player.getName())
                .put("inventory_type", event.getInventory().getType().toString())
                .put("container_name", ComponentUtils.extractText(event.getView().title()))
                .put("location_world", event.getInventory().getLocation() != null ? 
                     event.getInventory().getLocation().getWorld().getName() : "UNKNOWN")
                .put("location_x", event.getInventory().getLocation() != null ? 
                     event.getInventory().getLocation().getBlockX() : 0)
                .put("location_y", event.getInventory().getLocation() != null ? 
                     event.getInventory().getLocation().getBlockY() : 0)
                .put("location_z", event.getInventory().getLocation() != null ? 
                     event.getInventory().getLocation().getBlockZ() : 0);
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("inventory_type", "Inventory Type", ParameterType.STRING),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING)
        );
    }
}