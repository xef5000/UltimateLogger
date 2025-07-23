package ca.xef5000.ultimateLogger.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
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
        player.sendMessage(ChatColor.GRAY + "Type 'cancel' to abort.");
        inputSessions.put(player.getUniqueId(), onInput);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if the player is in an input session
        if (inputSessions.containsKey(playerId)) {
            // This player is providing input, so cancel the chat message from going public
            event.setCancelled(true);

            String message = event.getMessage();
            Consumer<String> onInput = inputSessions.remove(playerId);

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Input cancelled.");
                return;
            }

            // IMPORTANT: The consumer might open a GUI, which must be done on the main thread.
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("UltimateLogger"), () -> {
                onInput.accept(message);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Make sure to clean up if a player logs off mid-input
        inputSessions.remove(event.getPlayer().getUniqueId());
    }
}
