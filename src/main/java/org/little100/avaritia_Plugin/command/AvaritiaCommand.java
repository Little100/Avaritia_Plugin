package org.little100.avaritia_Plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.NBTDebugCommand;
import org.little100.avaritia_Plugin.manager.GUIManager;
import org.little100.avaritia_Plugin.manager.LanguageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AvaritiaCommand implements CommandExecutor, TabCompleter {
    
    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;
    private final GUIManager guiManager;
    
    // 子命令处理器
    private final NBTDebugCommand nbtDebugCommand;
    private final LanguageCommand languageCommand;
    private final GUICommand guiCommand;
    private final GiveCommand giveCommand;
    
    public AvaritiaCommand(Avaritia_Plugin plugin, LanguageManager languageManager, GUIManager guiManager, org.little100.avaritia_Plugin.manager.ItemManager itemManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.guiManager = guiManager;
        this.nbtDebugCommand = new NBTDebugCommand(plugin, languageManager);
        this.languageCommand = new LanguageCommand(plugin, languageManager, itemManager);
        this.guiCommand = new GUICommand(plugin, languageManager, guiManager);
        this.giveCommand = new GiveCommand(plugin, languageManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 如果没有参数，显示帮助
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (subCommand) {
            case "nbtdebug":
            case "nbt":
            case "debug":
                return nbtDebugCommand.onCommand(sender, command, "nbtdebug", subArgs);
                
            case "language":
            case "lang":
            case "lng":
                return languageCommand.onCommand(sender, command, "language", subArgs);
                
            case "gui":
            case "menu":
            case "inv":
                return guiCommand.onCommand(sender, command, "gui", subArgs);
                
            case "give":
            case "item":
                return giveCommand.execute(sender, subArgs);
                
            case "help":
            case "?":
                sendHelp(sender);
                return true;
                
            case "version":
            case "ver":
                sendVersion(sender);
                return true;
                
            case "reload":
                return handleReload(sender);
                
            default:
                sender.sendMessage("§c未知的子命令: " + subCommand);
                sender.sendMessage("§7使用 §e/avaritia help §7查看可用命令");
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList(
                "nbtdebug", "language", "gui", "give", "help", "version", "reload"
            );
            
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }
            
            // 别名
            if ("nbt".startsWith(input)) completions.add("nbt");
            if ("debug".startsWith(input)) completions.add("debug");
            if ("lang".startsWith(input)) completions.add("lang");
            if ("lng".startsWith(input)) completions.add("lng");
            if ("menu".startsWith(input)) completions.add("menu");
            if ("inv".startsWith(input)) completions.add("inv");
            if ("item".startsWith(input)) completions.add("item");
            if ("ver".startsWith(input)) completions.add("ver");
            
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            
            switch (subCommand) {
                case "nbtdebug":
                case "nbt":
                case "debug":
                    return new ArrayList<>();
                    
                case "language":
                case "lang":
                case "lng":
                    return languageCommand.onTabComplete(sender, command, "language", subArgs);
                    
                case "gui":
                case "menu":
                case "inv":
                    return guiCommand.onTabComplete(sender, command, "gui", subArgs);
                    
                case "give":
                case "item":
                    return giveCommand.onTabComplete(sender, subArgs);
            }
        }
        
        return completions;
    }
    
    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            languageManager.sendMessage(player, "command.help.header");
            languageManager.sendMessage(player, "command.help.nbtdebug");
            languageManager.sendMessage(player, "command.help.language");
            languageManager.sendMessage(player, "command.help.gui");
            languageManager.sendMessage(player, "command.help.version");
            languageManager.sendMessage(player, "command.help.reload");
            languageManager.sendMessage(player, "command.help.footer");
        } else {
            sender.sendMessage("§6=== Avaritia Plugin Commands ===");
            sender.sendMessage("§e/avaritia nbtdebug §7- Show item NBT debug info");
            sender.sendMessage("§e/avaritia language <lang> §7- Switch display language");
            sender.sendMessage("§e/avaritia gui <edit|look|new> <size> §7- Open GUI editor");
            sender.sendMessage("§e/avaritia give <player> <item> [amount] §7- Give plugin items");
            sender.sendMessage("§e/avaritia version §7- Show plugin version");
            sender.sendMessage("§e/avaritia reload §7- Reload plugin config");
            sender.sendMessage("§7Use aliases: §enbt, lang, menu, item");
        }
    }
    
    private void sendVersion(CommandSender sender) {
        sender.sendMessage("§6无尽贪婪插件 §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7作者: §f" + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage("§7服务器类型: §f" + (plugin.isFolia() ? "Folia" : "Paper/Spigot"));
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("avaritia.admin.reload")) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "general.no_permission");
            } else {
                sender.sendMessage("§c你没有权限执行此命令!");
            }
            return true;
        }
        
        try {
            plugin.reloadConfig();
            
            languageManager.reload();
            
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "command.reload.success");
            } else {
                sender.sendMessage("§a插件配置重载成功!");
            }
            
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("配置文件已被 " + sender.getName() + " 重载");
            }
            
        } catch (Exception e) {
            sender.sendMessage("§c重载配置时发生错误: " + e.getMessage());
            plugin.getLogger().warning("配置重载失败: " + e.getMessage());
        }
        
        return true;
    }
}
