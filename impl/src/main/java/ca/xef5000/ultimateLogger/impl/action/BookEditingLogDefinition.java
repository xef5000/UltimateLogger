package ca.xef5000.ultimateLogger.impl.action;

import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerEditBookEvent;

import java.util.List;

public class BookEditingLogDefinition extends LogDefinition<PlayerEditBookEvent> {
    @Override
    public String getId() {
        return "book_editing";
    }

    @Override
    public Class<PlayerEditBookEvent> getEventClass() {
        return PlayerEditBookEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerEditBookEvent event) {
        return !event.isCancelled();
    }

    @Override
    public LogData captureData(PlayerEditBookEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("is_signing", event.isSigning())
                .put("title", event.getNewBookMeta().getTitle())
                .put("author", event.getNewBookMeta().getAuthor())
                .put("page_count", event.getNewBookMeta().getPageCount());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("title", "Book Title", ParameterType.STRING),
                new ParameterDefinition("author", "Book Author", ParameterType.STRING),
                new ParameterDefinition("page_count", "Page Count", ParameterType.INTEGER)
        );
    }
}