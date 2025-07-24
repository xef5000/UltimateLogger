package ca.xef5000.ultimateLogger.impl.player;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;

import java.util.List;

public class PlayerChatLogDefinition extends LogDefinition<AsyncChatEvent> {
    @Override
    public String getId() {
        return "player_chat";
    }

    @Override
    public Class<AsyncChatEvent> getEventClass() {
        return AsyncChatEvent.class;
    }

    @Override
    public boolean shouldLog(AsyncChatEvent event) {
        return true;
    }

    @Override
    public LogData captureData(AsyncChatEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("message", ((TextComponent) event.message()).content());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("message", "Message", ParameterType.STRING)
        );
    }
}

