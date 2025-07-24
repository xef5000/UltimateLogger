package ca.xef5000.ultimateLogger;

import ca.xef5000.ultimateLogger.commands.LoggerCommands;
import ca.xef5000.ultimateLogger.frontend.GuiManager;
import ca.xef5000.ultimateLogger.impl.action.*;
import ca.xef5000.ultimateLogger.impl.block.AnvilLogDefinition;
import ca.xef5000.ultimateLogger.impl.block.ChestInteractionLogDefinition;
import ca.xef5000.ultimateLogger.impl.block.EnchantingLogDefinition;
import ca.xef5000.ultimateLogger.impl.block.FurnaceLogDefinition;
import ca.xef5000.ultimateLogger.impl.entity.EntityDeathLogDefinition;
import ca.xef5000.ultimateLogger.impl.entity.PrimedTNTLogDefinition;
import ca.xef5000.ultimateLogger.impl.player.*;
import ca.xef5000.ultimateLogger.managers.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class UltimateLogger extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private LogManager logManager;
    private GuiManager guiManager;
    private ChatInputManager chatInputManager;
    private WebhookManager webhookManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.configManager = new ConfigManager(this);
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        this.webhookManager = new WebhookManager(this);
        init();

        getLogger().info("UltimateLogger has been enabled!");
        new Metrics(this, 26639);
    }

    private void init() {
        this.databaseManager = new DatabaseManager(this, configManager);
        this.logManager = new LogManager(this, webhookManager, databaseManager);
        this.logManager.initialize();
        this.guiManager = new GuiManager(this);
        this.chatInputManager = new ChatInputManager(this);


        registerLogDefinitions();
        registerCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        getLogger().info("UltimateLogger has been disabled.");
    }

    public void fullPluginReload() {
        // Unregister all event listeners this plugin has registered.
        // This is crucial for a clean reload.
        HandlerList.unregisterAll(this);

        // Reload the config.yml file from disk
        reloadConfig();
        this.configManager = new ConfigManager(this);

        // Re-initialize managers that depend on the config
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        init();
    }

    private void registerLogDefinitions() {
        logManager.registerLogDefinition(new BlockBreakLogDefinition());
        logManager.registerLogDefinition(new BlockPlaceLogDefinition());
        logManager.registerLogDefinition(new PlayerChatLogDefinition());
        logManager.registerLogDefinition(new PlayerCommandLogDefinition());
        logManager.registerLogDefinition(new PlayerDeathLogDefinition());
        logManager.registerLogDefinition(new PlayerJoinLogDefinition());
        logManager.registerLogDefinition(new PlayerQuitLogDefinition());
        logManager.registerLogDefinition(new PlayerKickLogDefinition());
        logManager.registerLogDefinition(new PlayerSignLogDefinition());
        logManager.registerLogDefinition(new PlayerTeleportLogDefinition());
        logManager.registerLogDefinition(new PlayerLevelLogDefinition());
        logManager.registerLogDefinition(new BucketFillLogDefinition());
        logManager.registerLogDefinition(new BucketEmptyLogDefinition());
        logManager.registerLogDefinition(new AnvilLogDefinition());
        logManager.registerLogDefinition(new ItemPickupLogDefinition());
        logManager.registerLogDefinition(new ItemDropLogDefinition());
        logManager.registerLogDefinition(new EnchantingLogDefinition());
        logManager.registerLogDefinition(new BookEditingLogDefinition());
        logManager.registerLogDefinition(new FurnaceLogDefinition());
        logManager.registerLogDefinition(new GameModeLogDefinition());
        logManager.registerLogDefinition(new PlayerCraftLogDefinition());
        logManager.registerLogDefinition(new EntityDeathLogDefinition());
        logManager.registerLogDefinition(new PrimedTNTLogDefinition());
        logManager.registerLogDefinition(new ChestInteractionLogDefinition());
    }

    private void registerCommands() {
        LoggerCommands commands = new LoggerCommands(this);
        Objects.requireNonNull(getCommand("logger")).setExecutor(commands);
        Objects.requireNonNull(getCommand("logger")).setTabCompleter(commands);
    }

    public void reloadPluginConfig() {
        // The Bukkit method to reload the config from disk
        reloadConfig();
        // Re-initialize our managers to apply the new settings
        this.configManager = new ConfigManager(this);
        // We could also re-init the DB manager if its settings change, but for now this is fine.
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
}
