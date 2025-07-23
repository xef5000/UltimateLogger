package ca.xef5000.ultimateLogger.frontend;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Gui implements InventoryHolder {

    protected final Inventory inventory;
    // Maps a slot number to a Button object
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public Gui(int size, String title) {
        // The 'this' argument makes this class the InventoryHolder
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    // This is where subclasses will set up their items
    protected void decorate() {
        // To be implemented by subclasses
    }

    protected void setAction(int slot, Consumer<InventoryClickEvent> action) {
        actions.put(slot, action);
    }

    protected Map<Integer, Consumer<InventoryClickEvent>> getActions() {
        return actions;
    }

    public void handleClick(InventoryClickEvent event) {
        // Delegate the click to the correct button's action, if it exists
        if (actions.containsKey(event.getRawSlot())) {
            actions.get(event.getRawSlot()).accept(event);
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
