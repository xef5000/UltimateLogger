package ca.xef5000.ultimateLogger.managers;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private final Map<UUID, Consumer<String>> inputSessions = new HashMap<>();

    public ChatInputManager(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Puts a player into an input session.
     * @param player The player to get input from.
     * @param prompt The message to send the player.
     * @param onInput The action to perform with the player's input.
     */
    public void getPlayerInput(Player player, String prompt, Consumer<String> onInput) {
        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "[UltimateLogger] " + ChatColor.YELLOW + prompt);
        player.sendMessage(ChatColor.GRAY + "Type 'cancel' to abort. Prefix with '\\' to escape special inputs");
        inputSessions.put(player.getUniqueId(), onInput);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if the player is in an input session
        if (inputSessions.containsKey(playerId)) {
            // This player is providing input, so cancel the chat message from going public
            event.setCancelled(true);

            var ref = new Object() {
                String message = getStringInput(event.message());
            };

            // Handle escape characters
            // If the message starts with a backslash, remove the backslash.
            // This allows inputs like "\/logger" to become "/logger", bypassing command execution.
            // It also allows typing "\cancel" to literally input the word "cancel".
            if (ref.message.startsWith("\\")) {
                ref.message = ref.message.substring(1);
            }

            Consumer<String> onInput = inputSessions.remove(playerId);

            if (ref.message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Input cancelled.");
                return;
            }

            // IMPORTANT: The consumer might open a GUI, which must be done on the main thread.
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("UltimateLogger"), () -> {
                onInput.accept(ref.message);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Make sure to clean up if a player logs off mid-input
        inputSessions.remove(event.getPlayer().getUniqueId());
    }

    @Nonnull
    private String getStringInput(Component component) {
        if (component == null) {
            return "";
        }

        Component translatedComponent = GlobalTranslator.render(component, Locale.ENGLISH);

        return PlainTextComponentSerializer.plainText().serialize(translatedComponent);
    }
}