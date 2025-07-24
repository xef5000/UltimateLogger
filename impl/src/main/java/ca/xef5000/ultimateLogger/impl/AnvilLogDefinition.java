package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import java.util.List;

public class AnvilLogDefinition extends LogDefinition<PrepareAnvilEvent> {
    @Override
    public String getId() {
        return "anvil";
    }

    @Override
    public Class<PrepareAnvilEvent> getEventClass() {
        return PrepareAnvilEvent.class;
    }

    @Override
    public boolean shouldLog(PrepareAnvilEvent event) {
        return event.getResult() != null && event.getViewers().size() > 0;
    }

    @Override
    public LogData captureData(PrepareAnvilEvent event) {
        String playerName = event.getViewers().get(0).getName();
        String playerUuid = event.getViewers().get(0).getUniqueId().toString();
        
        return new LogData()
                .put("player_uuid", playerUuid)
                .put("player_name", playerName)
                .put("input_item", event.getInventory().getItem(0) != null ? 
                     event.getInventory().getItem(0).getType().toString() : "NONE")
                .put("result_item", event.getResult().getType().toString())
                .put("repair_cost", event.getInventory().getRepairCost());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("input_item", "Input Item", ParameterType.STRING),
                new ParameterDefinition("result_item", "Result Item", ParameterType.STRING),
                new ParameterDefinition("repair_cost", "Repair Cost", ParameterType.INTEGER)
        );
    }
}