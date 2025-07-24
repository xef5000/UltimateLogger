package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.List;

public class PlayerCraftLogDefinition extends LogDefinition<CraftItemEvent> {
    @Override
    public String getId() {
        return "player_craft";
    }

    @Override
    public Class<CraftItemEvent> getEventClass() {
        return CraftItemEvent.class;
    }

    @Override
    public boolean shouldLog(CraftItemEvent event) {
        return !event.isCancelled() && event.getWhoClicked() instanceof org.bukkit.entity.Player;
    }

    @Override
    public LogData captureData(CraftItemEvent event) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        return new LogData()
                .put("player_uuid", player.getUniqueId().toString())
                .put("player_name", player.getName())
                .put("crafted_item", event.getCurrentItem().getType().toString())
                .put("crafted_amount", event.getCurrentItem().getAmount())
                .put("recipe_type", event.getRecipe().getClass().getSimpleName());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("crafted_item", "Crafted Item", ParameterType.STRING),
                new ParameterDefinition("crafted_amount", "Crafted Amount", ParameterType.INTEGER),
                new ParameterDefinition("recipe_type", "Recipe Type", ParameterType.STRING)
        );
    }
}