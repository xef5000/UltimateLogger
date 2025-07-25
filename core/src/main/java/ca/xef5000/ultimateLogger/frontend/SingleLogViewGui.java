package ca.xef5000.ultimateLogger.frontend;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.LogEntry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.List;

public class SingleLogViewGui extends Gui {
    private final UltimateLogger plugin;
    private final long logId;
    private final Gui parent;

    public SingleLogViewGui(UltimateLogger plugin, long logId, @Nullable Gui parent) {
        super(27, "View Log #" + logId);
        this.plugin = plugin;
        this.logId = logId;
        this.parent = parent;
        decorate();
    }

    @Override
    protected void decorate() {
        inventory.setItem(22, createItem(Material.GLASS_PANE, "Loading...", null)); // Placeholder
        inventory.setItem(0, createItem(Material.BARRIER, ChatColor.RED + "View all logs", null)); // Placeholder
        setAction(0, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            plugin.getGuiManager().openGui(p, new LogsViewGui(plugin, 1, null, null));
        });

        plugin.getLogManager().getLogById(logId).thenAccept(optEntry -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (optEntry.isEmpty()) {
                    inventory.setItem(13, createItem(Material.BARRIER, "Error", List.of("Log not found.")));
                    return;
                }
                LogEntry entry = optEntry.get();

                inventory.setItem(13, LogsViewGui.createLogItem(entry));

                // The Archive/Unarchive button
                drawArchiveButton(entry);
                
                // The Delete button
                drawDeleteButton();
            });
        });
    }

    private void drawArchiveButton(LogEntry entry) {
        if (entry.isArchived()) {
            ItemStack btn = createItem(Material.ENDER_CHEST, "§cUnarchive Log", List.of("Make this log expire normally again."));
            inventory.setItem(22, btn);
            setAction(22, event -> toggleArchiveStatus((Player) event.getWhoClicked(), false));
        } else {
            ItemStack btn = createItem(Material.CHEST, "§aArchive Log", List.of("Protect this log from auto-deletion."));
            inventory.setItem(22, btn);
            setAction(22, event -> toggleArchiveStatus((Player) event.getWhoClicked(), true));
        }
    }

    private void toggleArchiveStatus(Player player, boolean archive) {
        if (archive && !player.hasPermission("ultimatelogger.archive")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to archive logs.");
            return;
        } else if (!archive && !player.hasPermission("ultimatelogger.unarchive")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to unarchive logs.");
            return;
        }

        plugin.getLogManager().setLogArchivedStatus(logId, archive).thenRun(() -> {
            // Re-open the GUI to show the change instantly
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getGuiManager().openGui(player, new SingleLogViewGui(plugin, logId, parent))
            );
        });
    }
    
    private void drawDeleteButton() {
        ItemStack btn = createItem(Material.LAVA_BUCKET, "§4Delete Log", List.of("Permanently delete this log from the database."));
        inventory.setItem(16, btn);
        setAction(16, event -> deleteLog((Player) event.getWhoClicked()));
    }
    
    private void deleteLog(Player player) {
        if (!player.hasPermission("ultimatelogger.delete")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to delete logs.");
            return;
        }
        
        // Close the GUI first
        player.closeInventory();
        
        // Delete the log
        plugin.getLogManager().deleteLogById(logId).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Log #" + logId + " has been deleted successfully.");
                    // Return to parent GUI if available
                    if (parent != null) {
                        plugin.getGuiManager().openGui(player, parent);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to delete log #" + logId + ".");
                    // Reopen this GUI
                    plugin.getGuiManager().openGui(player, new SingleLogViewGui(plugin, logId, parent));
                }
            });
        });
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
