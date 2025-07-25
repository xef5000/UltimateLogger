package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerQuitLogDefinition extends LogDefinition<PlayerQuitEvent> {
    @Override
    public String getId() {
        return "player_quit";
    }

    @Override
    public Class<PlayerQuitEvent> getEventClass() {
        return PlayerQuitEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerQuitEvent event) {
        return true;
    }

    @Override
    public LogData captureData(PlayerQuitEvent event) {
        assert event.quitMessage() != null;
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("quit_message", ((TranslatableComponent)event.quitMessage()) == null ? "null" : ((TranslatableComponent)event.quitMessage()).key());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("quit_message", "Quit Message", ParameterType.STRING)
        );
    }
}

