package ca.xef5000.ultimateLogger.frontend;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {

    // Maps a player's UUID to the GUI they currently have open
    private final Map<UUID, Gui> openGuis = new HashMap<>();

    public GuiManager(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGui(Player player, Gui gui) {
        player.openInventory(gui.getInventory());
        openGuis.put(player.getUniqueId(), gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Gui gui = openGuis.get(player.getUniqueId());

        if (gui != null) {
            // We have a handler for this player, so cancel the event
            event.setCancelled(true);
            // Delegate the click to the GUI handler
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // When a player closes a GUI, remove them from the map
        openGuis.remove(event.getPlayer().getUniqueId());
    }
}
