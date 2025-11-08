package ca.xef5000.ultimateLogger.frontend;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.FilterCondition;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TimeFilterGui extends Gui {
    private final UltimateLogger plugin;
    private final GuiManager guiManager;
    private final String baseLogType; // can be null
    private final List<FilterCondition> currentConditions;
    private final int returnPage;

    private LocalDateTime selectedStart;
    private LocalDateTime selectedEnd;
    private String comparator; // "before", "after", or "between"

    public TimeFilterGui(UltimateLogger plugin, String baseLogType, List<FilterCondition> currentConditions, int returnPage) {
        super(54, "Time Filter");
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
        this.baseLogType = baseLogType;
        this.currentConditions = (currentConditions == null) ? new ArrayList<>() : currentConditions;
        this.returnPage = Math.max(1, returnPage);

        this.selectedStart = LocalDateTime.now();
        this.selectedEnd = this.selectedStart.plusHours(1);
        this.comparator = "before"; // default

        decorate();
    }

    @Override
    protected void decorate() {
        draw();
    }

    private void draw() {
        inventory.clear();
        getActions().clear();

        // Time selectors
        if ("between".equalsIgnoreCase(comparator)) {
            addTimeSelector(10, true);
            addTimeSelector(19, false); // second selector begins at slot 19 per requirement
        } else {
            addTimeSelector(10, true);
        }

        // Comparator toggle at slot 31
        ItemStack cmp = createItem(
                Material.COMPARATOR,
                ChatColor.GOLD + "Comparator: " + ChatColor.AQUA + comparator.toUpperCase(),
                List.of(ChatColor.GRAY + "Click to toggle: BEFORE → AFTER → BETWEEN"));
        inventory.setItem(31, cmp);
        setAction(31, e -> {
            if ("before".equalsIgnoreCase(comparator)) comparator = "after";
            else if ("after".equalsIgnoreCase(comparator)) comparator = "between";
            else comparator = "before";
            draw();
        });

        // Apply and Reset buttons
        ItemStack apply = createItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Apply", List.of(ChatColor.GRAY + "Apply the time filter."));
        inventory.setItem(48, apply);
        setAction(48, e -> {
            Player p = (Player) e.getWhoClicked();
            // Remove previous timestamp condition if present
            currentConditions.removeIf(c -> "timestamp".equalsIgnoreCase(c.key()));

            if ("between".equalsIgnoreCase(comparator)) {
                long startMs = selectedStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long endMs = selectedEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long min = Math.min(startMs, endMs);
                long max = Math.max(startMs, endMs);
                currentConditions.add(new FilterCondition("timestamp", "between", new long[]{min, max}));
            } else {
                long ms = selectedStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                currentConditions.add(new FilterCondition("timestamp", comparator, ms));
            }
            p.closeInventory();
            guiManager.openGui(p, new LogsViewGui(plugin, 1, baseLogType, currentConditions));
        });

        ItemStack reset = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Reset", List.of(ChatColor.GRAY + "Remove time filter and return."));
        inventory.setItem(50, reset);
        setAction(50, e -> {
            Player p = (Player) e.getWhoClicked();
            currentConditions.removeIf(c -> "timestamp".equalsIgnoreCase(c.key()));
            p.closeInventory();
            guiManager.openGui(p, new LogsViewGui(plugin, returnPage, baseLogType, currentConditions));
        });
    }

    private void addTimeSelector(int startSlot, boolean forStart) {
        LocalDateTime sel = forStart ? selectedStart : selectedEnd;
        placeAdjustable(startSlot, Material.WRITABLE_BOOK, (forStart ? "Start " : "End ") + "Year", sel.getYear(), 1970, 9999, (val) -> {
            if (forStart) selectedStart = selectedStart.withYear(val); else selectedEnd = selectedEnd.withYear(val);
        });
        placeAdjustable(startSlot + 1, Material.MAP, (forStart ? "Start " : "End ") + "Month", sel.getMonthValue(), 1, 12, (val) -> {
            if (forStart) {
                int day = selectedStart.getDayOfMonth();
                selectedStart = selectedStart.withMonth(val);
                int maxDay = YearMonth.of(selectedStart.getYear(), selectedStart.getMonthValue()).lengthOfMonth();
                if (day > maxDay) day = maxDay;
                selectedStart = selectedStart.withDayOfMonth(day);
            } else {
                int day = selectedEnd.getDayOfMonth();
                selectedEnd = selectedEnd.withMonth(val);
                int maxDay = YearMonth.of(selectedEnd.getYear(), selectedEnd.getMonthValue()).lengthOfMonth();
                if (day > maxDay) day = maxDay;
                selectedEnd = selectedEnd.withDayOfMonth(day);
            }
        });
        int maxDay = YearMonth.of(sel.getYear(), sel.getMonthValue()).lengthOfMonth();
        placeAdjustable(startSlot + 2, Material.CLOCK, (forStart ? "Start " : "End ") + "Day", sel.getDayOfMonth(), 1, maxDay, (val) -> {
            if (forStart) selectedStart = selectedStart.withDayOfMonth(val); else selectedEnd = selectedEnd.withDayOfMonth(val);
        });
        placeAdjustable(startSlot + 3, Material.REDSTONE, (forStart ? "Start " : "End ") + "Hour", sel.getHour(), 0, 23, (val) -> {
            if (forStart) selectedStart = selectedStart.withHour(val); else selectedEnd = selectedEnd.withHour(val);
        });
        placeAdjustable(startSlot + 4, Material.REDSTONE_TORCH, (forStart ? "Start " : "End ") + "Minute", sel.getMinute(), 0, 59, (val) -> {
            if (forStart) selectedStart = selectedStart.withMinute(val); else selectedEnd = selectedEnd.withMinute(val);
        });

        long epochMs = sel.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        ItemStack preview = createItem(Material.PAPER, ChatColor.GREEN + (forStart ? "Start Selected:" : "End Selected:"),
                List.of(
                        ChatColor.AQUA + sel.toString(),
                        ChatColor.GRAY + "Epoch ms: " + epochMs
                ));
        inventory.setItem(startSlot + 6, preview);
    }

    private interface IntApplier { void apply(int value); }

    private void placeAdjustable(int slot, Material mat, String label, int value, int min, int max, IntApplier applier) {
        // Main value item
        ItemStack item = createItem(mat, ChatColor.AQUA + label + ": " + ChatColor.WHITE + value,
                List.of(ChatColor.GRAY + "Left-click: +1", ChatColor.GRAY + "Right-click: -1", ChatColor.DARK_GRAY + "Shift adds/subtracts 10"));
        inventory.setItem(slot, item);
        setAction(slot, e -> {
            ClickType type = e.getClick();
            int delta = (type.isLeftClick() ? 1 : type.isRightClick() ? -1 : 0);
            if (delta == 0) return;
            int step = (type.isShiftClick()) ? 10 : 1;
            delta = delta > 0 ? step : -step;
            int newVal = value + delta;
            if (newVal < min) newVal = max;
            if (newVal > max) newVal = min;
            applier.apply(newVal);
            // After change, ensure day valid when month/year changed for both selectors
            int maxDayStart = YearMonth.of(selectedStart.getYear(), selectedStart.getMonthValue()).lengthOfMonth();
            if (selectedStart.getDayOfMonth() > maxDayStart) selectedStart = selectedStart.withDayOfMonth(maxDayStart);
            int maxDayEnd = YearMonth.of(selectedEnd.getYear(), selectedEnd.getMonthValue()).lengthOfMonth();
            if (selectedEnd.getDayOfMonth() > maxDayEnd) selectedEnd = selectedEnd.withDayOfMonth(maxDayEnd);
            draw();
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
