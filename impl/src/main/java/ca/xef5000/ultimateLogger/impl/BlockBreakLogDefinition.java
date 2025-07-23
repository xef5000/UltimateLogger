package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

public class BlockBreakLogDefinition extends LogDefinition<BlockBreakEvent> {

    @Override
    public String getId() {
        return "block_break";
    }

    @Override
    public Class<BlockBreakEvent> getEventClass() {
        return BlockBreakEvent.class;
    }

    @Override
    public boolean shouldLog(BlockBreakEvent event) {
        // We can add logic here. For example, don't log if the event was cancelled.
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(BlockBreakEvent event) {
        // Create the LogData payload with relevant information
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("block_type", event.getBlock().getType().toString())
                .put("location_world", event.getBlock().getLocation().getWorld().getName())
                .put("location_x", event.getBlock().getLocation().getBlockX())
                .put("location_y", event.getBlock().getLocation().getBlockY())
                .put("location_z", event.getBlock().getLocation().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("block_type", "Block Type", ParameterType.STRING),
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("location_x", "Location X", ParameterType.INTEGER),
                new ParameterDefinition("location_y", "Location Y", ParameterType.INTEGER),
                new ParameterDefinition("location_z", "Location Z", ParameterType.INTEGER),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING)
        );
    }
}
