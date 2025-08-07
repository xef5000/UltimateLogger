package ca.xef5000.ultimateLogger.commands;

import ca.xef5000.ultimateLogger.UltimateLogger;
import ca.xef5000.ultimateLogger.api.LogEntry;
import ca.xef5000.ultimateLogger.frontend.GuiManager;
import ca.xef5000.ultimateLogger.frontend.LogsViewGui;
import ca.xef5000.ultimateLogger.frontend.SingleLogViewGui;
import ca.xef5000.ultimateLogger.managers.LogManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoggerCommands implements CommandExecutor, TabCompleter {

    private final UltimateLogger plugin;
    private final LogManager logManager;
    private final GuiManager guiManager;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoggerCommands(UltimateLogger plugin) {
        this.plugin = plugin;
        this.logManager = plugin.getLogManager();
        this.guiManager = plugin.getGuiManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("ultimatelogger.view")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            guiManager.openGui(player, new LogsViewGui(plugin, 1, null, null));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "view":
                handleViewCommand(sender, args);
                break;
            case "stats":
                handleStatsCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "log":
                handlelog(sender, args);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleViewCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatelogger.view")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        int page = 1;
        int pageSize = 10;

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    sender.sendMessage(ChatColor.RED + "Page number must be 1 or greater.");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number. Please use a number.");
                return;
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Loading logs for page " + page + "...");

        // Use the async future from LogManager
        int finalPage = page;
        logManager.getLogsPage(page, pageSize, null, null).thenAccept(logs -> {
            // We are in an async thread here, so we must schedule the message sending back to the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (logs.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No logs found for this page.");
                    return;
                }

                sender.sendMessage(ChatColor.GOLD + "--- " + ChatColor.WHITE + "Logs (Page " + finalPage + ")" + ChatColor.GOLD + " ---");
                for (LogEntry entry : logs) {
                    String timestamp = DATE_FORMAT.format(entry.getTimestamp().atZone(java.time.ZoneId.systemDefault()));
                    sender.sendMessage(ChatColor.GRAY + "[" + entry.getId() + "] " +
                            ChatColor.AQUA + timestamp + " " +
                            ChatColor.GREEN + entry.getLogType() + ": " +
                            ChatColor.WHITE + entry.getData().toJson());
                }
            });
        }).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "An error occurred while fetching logs. Check the console.");
            });
            ex.printStackTrace();
            return null;
        });
    }

    private void handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("ultimatelogger.stats")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "--- " + ChatColor.WHITE + "UltimateLogger Stats" + ChatColor.GOLD + " ---");
        sender.sendMessage(ChatColor.AQUA + "Logs in save queue: " + ChatColor.WHITE + logManager.getSaveQueueSize());
        sender.sendMessage(ChatColor.AQUA + "Pages in cache: " + ChatColor.WHITE + logManager.getCacheSize());
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("ultimatelogger.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        plugin.fullPluginReload();
        sender.sendMessage(ChatColor.GREEN + "UltimateLogger configuration has been reloaded!");
    }

    private void handlelog(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatelogger.view")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /logger log <id>");
            return;
        }
        String id = args[1];
        if (sender instanceof Player player) {
            guiManager.openGui(player, new SingleLogViewGui(plugin, Long.parseLong(id), null));
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- " + ChatColor.WHITE + "UltimateLogger Help" + ChatColor.GOLD + " ---");
        sender.sendMessage(ChatColor.AQUA + "/logger view <page>" + ChatColor.GRAY + " - View logs.");
        sender.sendMessage(ChatColor.AQUA + "/logger stats" + ChatColor.GRAY + " - View plugin statistics.");
        sender.sendMessage(ChatColor.AQUA + "/logger reload" + ChatColor.GRAY + " - Reload the config.");
        sender.sendMessage(ChatColor.AQUA + "/logger help" + ChatColor.GRAY + " - Shows this message.");
        sender.sendMessage(ChatColor.AQUA + "/logger log <id>" + ChatColor.GRAY + " - View a specific log by ID.");
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("view", "stats", "reload", "help", "log").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        // No suggestions for other arguments
        return Collections.emptyList();
    }
}
