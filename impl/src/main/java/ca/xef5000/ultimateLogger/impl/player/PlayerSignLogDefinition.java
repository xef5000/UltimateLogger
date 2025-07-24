package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.block.SignChangeEvent;

import java.util.List;

public class PlayerSignLogDefinition extends LogDefinition<SignChangeEvent> {
    @Override
    public String getId() {
        return "player_sign";
    }

    @Override
    public Class<SignChangeEvent> getEventClass() {
        return SignChangeEvent.class;
    }

    @Override
    public boolean shouldLog(SignChangeEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(SignChangeEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("sign_text", String.join("|", event.getLines()))
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
                new ParameterDefinition("sign_text", "Sign Text", ParameterType.STRING),
                new ParameterDefinition("location_world", "Location World", ParameterType.STRING),
                new ParameterDefinition("location_x", "Location X", ParameterType.INTEGER),
                new ParameterDefinition("location_y", "Location Y", ParameterType.INTEGER),
                new ParameterDefinition("location_z", "Location Z", ParameterType.INTEGER)
        );
    }
}