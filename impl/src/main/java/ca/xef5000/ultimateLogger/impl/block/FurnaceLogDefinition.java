package ca.xef5000.ultimateLogger.impl.block;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.inventory.FurnaceExtractEvent;

import java.util.List;

public class FurnaceLogDefinition extends LogDefinition<FurnaceExtractEvent> {
    @Override
    public String getId() {
        return "furnace";
    }

    @Override
    public Class<FurnaceExtractEvent> getEventClass() {
        return FurnaceExtractEvent.class;
    }

    @Override
    public boolean shouldLog(FurnaceExtractEvent event) {
        return true;
    }

    @Override
    public LogData captureData(FurnaceExtractEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("item_type", event.getItemType().toString())
                .put("item_amount", event.getItemAmount())
                .put("experience", event.getExpToDrop())
                .put("location_world", event.getBlock().getLocation().getWorld().getName())
                .put("location_x", event.getBlock().getLocation().getBlockX())
                .put("location_y", event.getBlock().getLocation().getBlockY())
                .put("location_z", event.getBlock().getLocation().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("item_type", "Item Type", ParameterType.STRING),
                new ParameterDefinition("item_amount", "Item Amount", ParameterType.INTEGER),
                new ParameterDefinition("experience", "Experience", ParameterType.INTEGER),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING)
        );
    }
}