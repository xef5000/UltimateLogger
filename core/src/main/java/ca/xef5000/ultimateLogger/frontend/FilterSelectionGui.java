package ca.xef5000.ultimateLogger.frontend;

import ca.xef5000.ultimateLogger.UltimateLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

public class FilterSelectionGui extends Gui {

    private final UltimateLogger plugin;
    private final GuiManager guiManager;

    public FilterSelectionGui(UltimateLogger plugin) {
        super(54, "Select a Log Type to Filter");
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
        decorate();
    }

    @Override
    protected void decorate() {
        // Add a button to clear any existing filter
        ItemStack clearFilterItem = createItem(Material.BARRIER, ChatColor.RED + "Clear Filter", null);
        inventory.setItem(0, clearFilterItem);
        setAction(0, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            // Open the main logs view with a null filter
            guiManager.openGui(p, new LogsViewGui(plugin, 1, null, null));
        });

        // Fetch the distinct log types and populate the GUI
        plugin.getLogManager().getDistinctLogTypes().thenAccept(logTypes -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Start populating from slot 1
                for (int i = 0; i < logTypes.size(); i++) {
                    if (i + 1 >= inventory.getSize()) break; // Stop if GUI is full
                    String logType = logTypes.get(i);
                    ItemStack typeItem = createItem(Material.BOOK, ChatColor.GREEN + logType, Collections.singletonList(ChatColor.GRAY + "Click to filter by this type."));
                    inventory.setItem(i + 1, typeItem);
                    setAction(i + 1, event -> {
                        Player p = (Player) event.getWhoClicked();
                        p.closeInventory();
                        // Open the main logs view, passing the selected log type as the filter
                        guiManager.openGui(p, new LogsViewGui(plugin, 1, logType, null));
                    });
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
