package ca.xef5000.ultimateLogger.impl.entity;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class PrimedTNTLogDefinition extends LogDefinition<EntityExplodeEvent> {
    @Override
    public String getId() {
        return "primed_tnt";
    }

    @Override
    public Class<EntityExplodeEvent> getEventClass() {
        return EntityExplodeEvent.class;
    }

    @Override
    public boolean shouldLog(EntityExplodeEvent event) {
        return event.getEntity() instanceof TNTPrimed;
    }

    @Override
    public LogData captureData(EntityExplodeEvent event) {
        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        LogData data = new LogData()
                .put("location_world", event.getLocation().getWorld().getName())
                .put("location_x", event.getLocation().getBlockX())
                .put("location_y", event.getLocation().getBlockY())
                .put("location_z", event.getLocation().getBlockZ())
                .put("blocks_destroyed", event.blockList().size())
                .put("yield", event.getYield());

        if (tnt.getSource() instanceof Player) {
            Player player = (Player) tnt.getSource();
            data.put("player_uuid", player.getUniqueId().toString())
                .put("player_name", player.getName());
        }

        return data;
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING),
                new ParameterDefinition("blocks_destroyed", "Blocks Destroyed", ParameterType.INTEGER),
                new ParameterDefinition("yield", "Explosion Yield", ParameterType.STRING)
        );
    }
}