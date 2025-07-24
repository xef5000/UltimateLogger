package ca.xef5000.ultimateLogger.frontend;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import ca.xef5000.ultimateLogger.api.LogEntry;
import ca.xef5000.ultimateLogger.managers.LogManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogsViewGui extends Gui {

    private final UltimateLogger plugin;
    private final LogManager logManager;
    private final GuiManager guiManager;
    private int currentPage;
    private final int pageSize = 45; // 5 rows of 9 for logs
    private final String currentFilter;
    private final List<FilterCondition> advancedFilters;

    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogsViewGui(UltimateLogger plugin, int page, String filter, List<FilterCondition> advancedFilters) {
        super(54, buildTitle(filter));
        this.plugin = plugin;
        this.logManager = plugin.getLogManager();
        this.guiManager = plugin.getGuiManager();
        this.currentPage = page;
        this.currentFilter = filter;
        this.advancedFilters = (advancedFilters == null) ? new ArrayList<>() : advancedFilters;
        decorate();
    }

    private static String buildTitle(String filter) {
        if (filter != null) {
            return "Logs (Filter: " + filter +")";
        }
        return "Logs";
    }

    @Override
    protected void decorate() {
        // Set placeholder items while logs are loading
        ItemStack loadingItem = createItem(Material.GLASS_PANE, ChatColor.GRAY + "Loading...", null);
        for (int i = 0; i < pageSize; i++) {
            inventory.setItem(i, loadingItem);
        }

        // Add navigation buttons
        if (currentPage > 1) {
            String name = ChatColor.YELLOW + "« Previous Page";
            List<String> lore = Collections.singletonList(ChatColor.GRAY + "Click to go to page " + ChatColor.GREEN + (currentPage - 1));
            ItemStack prevPage = createItem(Material.ARROW, name, lore);
            inventory.setItem(45, prevPage);
            setAction(45, event -> changePage(currentPage - 1));
        }

        if (currentFilter != null) { // Only show if a log_type is being filtered
            ItemStack advFilterItem = createItem(Material.COMPARATOR, ChatColor.RED + "Advanced Filter", null);
            inventory.setItem(48, advFilterItem); // Set it in an empty control slot
            setAction(48, event -> {
                Player p = (Player) event.getWhoClicked();
                p.closeInventory();
                // Launch our new GUI, passing the current state
                guiManager.openGui(p, new AdvancedFilterGui(plugin, currentFilter, advancedFilters, 1));
            });
        }

        ItemStack filterItem = createItem(Material.HOPPER, ChatColor.AQUA + "Filter Logs", Collections.singletonList(ChatColor.GRAY + "Filter the logs by type."));
        inventory.setItem(47, filterItem);
        setAction(47, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            guiManager.openGui(p, new FilterSelectionGui(plugin));
        });

        ItemStack refreshItem = createItem(Material.SUNFLOWER, ChatColor.YELLOW + "Refresh", Collections.singletonList(ChatColor.GRAY + "Reloads the current view."));
        inventory.setItem(49, refreshItem);
        setAction(49, event -> {
            logManager.invalidateCache(); // Clear the entire cache
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            // Re-open the same view, forcing a database pull
            guiManager.openGui(p, new LogsViewGui(plugin, currentPage, currentFilter, advancedFilters));
        });

        String nextName = ChatColor.YELLOW + "Next Page »";
        List<String> nextLore = Collections.singletonList(ChatColor.GRAY + "Click to go to page " + ChatColor.GREEN + (currentPage + 1));
        ItemStack nextPage = createItem(Material.ARROW, nextName, nextLore);
        inventory.setItem(53, nextPage);
        setAction(53, event -> changePage(currentPage + 1));

        // Asynchronously fetch and display the logs
        logManager.getLogsPage(currentPage, pageSize, currentFilter, advancedFilters).thenAccept(logs ->
            plugin.getServer().getScheduler().runTask(plugin, () -> populateLogs(logs))
        );
    }

    /**
     * Changes the current page of the GUI and redraws its contents
     * without closing and reopening the inventory.
     * @param newPage The page number to display.
     */
    public void changePage(int newPage) {
        if (newPage < 1) return; // Don't go to page 0 or less

        // Update the state
        this.currentPage = newPage;

        // Clear the inventory of old items and actions
        this.inventory.clear();
        this.getActions().clear(); // Assuming you add a getter in the parent Gui class

        // Redraw the GUI with the new page's content
        decorate();
    }

    private void populateLogs(List<LogEntry> logs) {
        for (int i = 0; i < pageSize; i++) {
            if (i < logs.size()) {
                LogEntry entry = logs.get(i);
                ItemStack logItem = createLogItem(entry);
                inventory.setItem(i, logItem);

                setAction(i, event -> {
                    Player p = (Player) event.getWhoClicked();
                    guiManager.openGui(p, new SingleLogViewGui(plugin, entry.getId(), this));
                });
            } else {
                // Clear any remaining loading panes
                inventory.setItem(i, null);
            }
        }
    }

    public static @NotNull ItemStack createLogItem(LogEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.AQUA + "Timestamp: " + ChatColor.WHITE + format.format(entry.getTimestamp().atZone(java.time.ZoneId.systemDefault())));
        lore.add(ChatColor.AQUA + "Type: " + ChatColor.WHITE + entry.getLogType());
        lore.add(ChatColor.AQUA + "Data: ");
        // A simple way to display map data
        LinkedHashMap<String, Object> dataMap = entry.getData().getData();
        if (dataMap != null && !dataMap.isEmpty()) {
            for (Map.Entry<String, Object> dataEntry : dataMap.entrySet()) {
                String formattedKey = formatKey(dataEntry.getKey());

                lore.add(ChatColor.GOLD + formattedKey + ": " + ChatColor.WHITE + dataEntry.getValue());
            }
        }

        ItemStack logItem = createItem(Material.PAPER, ChatColor.GOLD + "Log #" + entry.getId(), lore);
        return logItem;
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Formats a raw data key (like "player_name") into a human-readable
     * title-case string (like "Player Name").
     *
     * @param key The raw key string.
     * @return A formatted, human-readable string.
     */
    private static String formatKey(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String[] words = key.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }
        return formatted.toString().trim();
    }

    public String getCurrentFilter() {
        return currentFilter;
    }
}

