package ca.xef5000.ultimateLogger.frontend;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import ca.xef5000.ultimateLogger.managers.ChatInputManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedFilterGui extends Gui {
    private final UltimateLogger plugin;
    private final GuiManager guiManager;
    private final ChatInputManager chatInputManager;

    private final String baseLogType;
    private List<FilterCondition> currentConditions;
    private final int page;
    private final int paramsPerPage = 5; // 5 rows for parameters

    public AdvancedFilterGui(UltimateLogger plugin, String baseLogType, List<FilterCondition> conditions, int page) {
        super(54, "Advanced Filter: " + baseLogType);
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
        this.chatInputManager = plugin.getChatInputManager();
        this.baseLogType = baseLogType;
        this.currentConditions = new ArrayList<>(conditions);
        this.page = page;

        decorate();
    }

    @Override
    protected void decorate() {
        LogDefinition<?> definition = plugin.getLogManager().getLogDefinition(baseLogType);
        if (definition == null) return;

        List<ParameterDefinition> params = definition.getFilterableParameters();

        // Populate parameter rows
        for (int i = 0; i < paramsPerPage; i++) {
            int paramIndex = ((page - 1) * paramsPerPage) + i;
            if (paramIndex >= params.size()) break; // No more parameters to display

            ParameterDefinition param = params.get(paramIndex);
            FilterCondition existingCondition = findCondition(param.key());

            // Each row is 3 items wide
            int rowStartSlot = i * 9;
            drawParameterRow(rowStartSlot, param, existingCondition);
        }

        // --- CONTROL BUTTONS ---
        // Back to main GUI (Apply Filter)
        ItemStack applyItem = createItem(Material.BARRIER, ChatColor.GREEN + "Apply Filter & View Logs", Collections.singletonList(ChatColor.GRAY + "Go back to the log view with these filters."));
        inventory.setItem(48, applyItem);
        setAction(48, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            guiManager.openGui(p, new LogsViewGui(plugin, 1, baseLogType, currentConditions));
        });

        // Remove All Filters button
        ItemStack clearItem = createItem(Material.TNT, ChatColor.RED + "Remove All Filters", null);
        inventory.setItem(49, clearItem);
        setAction(49, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            guiManager.openGui(p, new AdvancedFilterGui(plugin, baseLogType, new ArrayList<>(), 1));
        });

        // Pagination Arrows
        if (page > 1) {
            ItemStack prevPage = createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null);
            inventory.setItem(45, prevPage);
            setAction(45, e -> guiManager.openGui((Player) e.getWhoClicked(), new AdvancedFilterGui(plugin, baseLogType, currentConditions, page - 1)));
        }
        if (params.size() > page * paramsPerPage) {
            ItemStack nextPage = createItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null);
            inventory.setItem(53, nextPage);
            setAction(53, e -> guiManager.openGui((Player) e.getWhoClicked(), new AdvancedFilterGui(plugin, baseLogType, currentConditions, page + 1)));
        }
    }

    private void drawParameterRow(int startSlot, ParameterDefinition param, FilterCondition condition) {
        // Column 1: Paper (Parameter Name)
        List<String> paramLore = new ArrayList<>();
        paramLore.add(ChatColor.GRAY + "Type: " + param.type().name());
        paramLore.add(" ");
        if (condition != null) {
            paramLore.add(ChatColor.GREEN + "Currently filtering this parameter.");
        } else {
            paramLore.add(ChatColor.YELLOW + "Click the comparator to add a filter.");
        }
        ItemStack paramItem = createItem(Material.PAPER, ChatColor.AQUA + param.displayName(), paramLore);
        inventory.setItem(startSlot, paramItem);

        // Column 2: Comparator
        String comparator = (condition != null) ? condition.comparator() : "N/A";
        ItemStack comparatorItem = createItem(Material.COMPARATOR, ChatColor.GOLD + "Comparator: " + comparator, Collections.singletonList(ChatColor.GRAY + "Click to change comparison method."));
        inventory.setItem(startSlot + 1, comparatorItem);
        setAction(startSlot + 1, event -> handleComparatorClick((Player) event.getWhoClicked(), param, condition));

        // Column 3: String (Value)
        String value = (condition != null) ? condition.value().toString() : "Not Set";
        ItemStack valueItem = createItem(Material.STRING, ChatColor.GOLD + "Value: " + value, Collections.singletonList(ChatColor.GRAY + "Click to set the value to compare against."));
        inventory.setItem(startSlot + 2, valueItem);
        setAction(startSlot + 2, event -> handleValueClick((Player) event.getWhoClicked(), param, condition));
    }

    private void handleComparatorClick(Player player, ParameterDefinition param, FilterCondition condition) {
        // For simplicity, we'll cycle through them. A separate GUI is also a great option.
        String currentComparator = (condition != null) ? condition.comparator() : "=";
        List<String> comparators = getValidComparators(param.type());
        int nextIndex = (comparators.indexOf(currentComparator) + 1) % comparators.size();
        String newComparator = comparators.get(nextIndex);

        updateCondition(param.key(), newComparator, (condition != null) ? condition.value() : "SET_VALUE");
        // Re-open the GUI to reflect the change
        guiManager.openGui(player, new AdvancedFilterGui(plugin, baseLogType, currentConditions, page));
    }

    private void handleValueClick(Player player, ParameterDefinition param, FilterCondition condition) {
        chatInputManager.getPlayerInput(player, "Enter value for " + param.displayName() + ":", input -> {
            String comparator = (condition != null) ? condition.comparator() : "=";
            updateCondition(param.key(), comparator, input);
            // Re-open the GUI to show the new value
            guiManager.openGui(player, new AdvancedFilterGui(plugin, baseLogType, currentConditions, page));
        });
    }

    private void updateCondition(String key, String comparator, Object value) {
        currentConditions.removeIf(c -> c.key().equals(key)); // Remove old condition
        currentConditions.add(new FilterCondition(key, comparator, value));
    }

    private FilterCondition findCondition(String key) {
        return currentConditions.stream().filter(c -> c.key().equals(key)).findFirst().orElse(null);
    }

    private List<String> getValidComparators(ParameterType type) {
        if (type == ParameterType.INTEGER || type == ParameterType.DOUBLE) {
            return List.of("=", "!=", ">", "<", ">=", "<=");
        }
        return List.of("=", "!="); // For STRING, UUID, BOOLEAN
    }

    // You already have a createItem method, ensure it's here or accessible
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
