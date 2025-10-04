package org.little100.avaritia_Plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.manager.GUIManager;
import org.little100.avaritia_Plugin.manager.LanguageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUICommand implements CommandExecutor, TabCompleter {
    
    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;
    private final GUIManager guiManager;
    
    public GUICommand(Avaritia_Plugin plugin, LanguageManager languageManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.guiManager = guiManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("avaritia.gui.use")) {
            languageManager.sendMessage(player, "general.no_permission");
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        if (action.equals("create")) {
            if (args.length < 3) {
                player.sendMessage("§c用法: /gui create <文件名> <大小>");
                player.sendMessage("§7示例: /gui create extreme_table 54");
                return true;
            }
            
            String fileName = args[1];
            String sizeArg = args[2];
            
            int size;
            try {
                size = Integer.parseInt(sizeArg);
            } catch (NumberFormatException e) {
                languageManager.sendMessage(player, "gui.invalid_size", sizeArg);
                return true;
            }
            
            if (!isValidSize(size)) {
                languageManager.sendMessage(player, "gui.invalid_size_range", size);
                return true;
            }
            
            guiManager.openCreateGUI(player, fileName, size);
            return true;
        }
        
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }
        
        String sizeArg = args[1];
        
        int size;
        try {
            size = Integer.parseInt(sizeArg);
        } catch (NumberFormatException e) {
            languageManager.sendMessage(player, "gui.invalid_size", sizeArg);
            return true;
        }
        
        if (!isValidSize(size)) {
            languageManager.sendMessage(player, "gui.invalid_size_range", size);
            return true;
        }
        
        switch (action) {
            case "edit":
                if (!player.hasPermission("avaritia.gui.edit")) {
                    languageManager.sendMessage(player, "general.no_permission");
                    return true;
                }
                guiManager.openEditGUI(player, size);
                break;
                
            case "look":
            case "view":
                guiManager.openViewGUI(player, size);
                break;
                
            case "new":
            case "create":
                if (!player.hasPermission("avaritia.gui.create")) {
                    languageManager.sendMessage(player, "general.no_permission");
                    return true;
                }
                guiManager.openNewGUI(player, size);
                break;
                
            default:
                sendUsage(player);
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String action : Arrays.asList("edit", "look", "view", "new", "create")) {
                if (action.startsWith(input)) {
                    completions.add(action);
                }
            }
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
        
            if (action.equals("create")) {
                completions.add("<文件名>");
                return completions;
            }
            
            String input = args[1];
            
            int[] commonSizes = {1, 5, 9, 18, 27, 36, 45, 54};
            for (int size : commonSizes) {
                String sizeStr = String.valueOf(size);
                if (sizeStr.startsWith(input)) {
                    completions.add(sizeStr);
                }
            }
            
            try {
                int inputNum = Integer.parseInt(input);
                for (int i = Math.max(1, inputNum); i <= Math.min(54, inputNum + 10); i++) {
                    String sizeStr = String.valueOf(i);
                    if (sizeStr.startsWith(input) && !completions.contains(sizeStr)) {
                        completions.add(sizeStr);
                    }
                }
            } catch (NumberFormatException ignored) {
                // 忽略
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            String input = args[2];
            int[] commonSizes = {1, 5, 9, 18, 27, 36, 45, 54};
            for (int size : commonSizes) {
                String sizeStr = String.valueOf(size);
                if (sizeStr.startsWith(input)) {
                    completions.add(sizeStr);
                }
            }
        }
        
        return completions;
    }
    
    private boolean isValidSize(int size) {
        return size >= 1 && size <= 54;
    }
    
    private void sendUsage(Player player) {
        languageManager.sendMessage(player, "gui.usage_header");
        languageManager.sendMessage(player, "gui.usage_edit");
        languageManager.sendMessage(player, "gui.usage_look");
        languageManager.sendMessage(player, "gui.usage_new");
        languageManager.sendMessage(player, "gui.size_note");
        languageManager.sendMessage(player, "gui.size_examples");
    }
}



