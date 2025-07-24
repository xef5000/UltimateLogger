package ca.xef5000.ultimateLogger.impl;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.enchantment.EnchantItemEvent;

import java.util.List;

public class EnchantingLogDefinition extends LogDefinition<EnchantItemEvent> {
    @Override
    public String getId() {
        return "enchanting";
    }

    @Override
    public Class<EnchantItemEvent> getEventClass() {
        return EnchantItemEvent.class;
    }

    @Override
    public boolean shouldLog(EnchantItemEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(EnchantItemEvent event) {
        return new LogData()
                .put("player_uuid", event.getEnchanter().getUniqueId().toString())
                .put("player_name", event.getEnchanter().getName())
                .put("item_type", event.getItem().getType().toString())
                .put("enchantments", event.getEnchantsToAdd().toString())
                .put("experience_cost", event.getExpLevelCost())
                .put("which_button", event.whichButton());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("item_type", "Item Type", ParameterType.STRING),
                new ParameterDefinition("experience_cost", "Experience Cost", ParameterType.INTEGER),
                new ParameterDefinition("which_button", "Button Clicked", ParameterType.INTEGER)
        );
    }
}