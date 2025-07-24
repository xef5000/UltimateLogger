package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.List;

public class BucketFillLogDefinition extends LogDefinition<PlayerBucketFillEvent> {
    @Override
    public String getId() {
        return "bucket_fill";
    }

    @Override
    public Class<PlayerBucketFillEvent> getEventClass() {
        return PlayerBucketFillEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerBucketFillEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(PlayerBucketFillEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("bucket_type", event.getItemStack().getType().toString())
                .put("block_type", event.getBlockClicked().getType().toString())
                .put("location_world", event.getBlockClicked().getLocation().getWorld().getName())
                .put("location_x", event.getBlockClicked().getLocation().getBlockX())
                .put("location_y", event.getBlockClicked().getLocation().getBlockY())
                .put("location_z", event.getBlockClicked().getLocation().getBlockZ());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("bucket_type", "Bucket Type", ParameterType.STRING),
                new ParameterDefinition("block_type", "Block Type", ParameterType.STRING),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING)
        );
    }
}