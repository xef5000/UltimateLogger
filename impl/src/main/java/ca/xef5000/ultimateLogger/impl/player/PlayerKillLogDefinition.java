package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class PlayerKillLogDefinition extends LogDefinition<PlayerDeathEvent> {
    @Override
    public String getId() {
        return "player_kill";
    }

    @Override
    public Class<PlayerDeathEvent> getEventClass() {
        return PlayerDeathEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        return (killer != null);
    }

    @Override
    public LogData captureData(PlayerDeathEvent event) {
        return new LogData()
                .put("killer_uuid", event.getEntity().getKiller().getUniqueId().toString())
                .put("killer_name", event.getEntity().getKiller().getName())
                .put("victim_uuid", event.getEntity().getUniqueId().toString())
                .put("victim_name", event.getEntity().getName())
                .put("location_world", event.getEntity().getLocation().getWorld().getName())
                .put("location_x", event.getEntity().getLocation().getBlockX())
                .put("location_y", event.getEntity().getLocation().getBlockY())
                .put("location_z", event.getEntity().getLocation().getBlockZ())
                ;
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("killer_name", "Killer Name", ParameterType.STRING),
                new ParameterDefinition("killer_uuid", "Killer UUID", ParameterType.UUID),
                new ParameterDefinition("victim_name", "Victim Name", ParameterType.STRING),
                new ParameterDefinition("victim_uuid", "Victim UUID", ParameterType.UUID),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING),
                new ParameterDefinition("location_x", "Location X", ParameterType.INTEGER),
                new ParameterDefinition("location_y", "Location Y", ParameterType.INTEGER),
                new ParameterDefinition("location_z", "Location Z", ParameterType.INTEGER)
        );
    }
}
