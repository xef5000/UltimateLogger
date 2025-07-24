package ca.xef5000.ultimateLogger.impl.entity;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class EntityDeathLogDefinition extends LogDefinition<EntityDeathEvent> {
    @Override
    public String getId() {
        return "entity_death";
    }

    @Override
    public Class<EntityDeathEvent> getEventClass() {
        return EntityDeathEvent.class;
    }

    @Override
    public boolean shouldLog(EntityDeathEvent event) {
        return !(event.getEntity() instanceof Player);
    }

    @Override
    public LogData captureData(EntityDeathEvent event) {
        LogData data = new LogData()
                .put("entity_type", event.getEntity().getType().toString())
                .put("location_world", event.getEntity().getLocation().getWorld().getName())
                .put("location_x", event.getEntity().getLocation().getBlockX())
                .put("location_y", event.getEntity().getLocation().getBlockY())
                .put("location_z", event.getEntity().getLocation().getBlockZ())
                .put("dropped_exp", event.getDroppedExp());

        if (event.getEntity().getKiller() != null) {
            data.put("killer_uuid", event.getEntity().getKiller().getUniqueId().toString())
                .put("killer_name", event.getEntity().getKiller().getName());
        }

        return data;
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("entity_type", "Entity Type", ParameterType.STRING),
                new ParameterDefinition("killer_name", "Killer Name", ParameterType.STRING),
                new ParameterDefinition("killer_uuid", "Killer UUID", ParameterType.UUID),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING),
                new ParameterDefinition("dropped_exp", "Dropped Experience", ParameterType.INTEGER)
        );
    }
}