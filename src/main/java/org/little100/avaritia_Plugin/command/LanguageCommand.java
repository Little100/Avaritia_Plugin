package org.little100.avaritia_Plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.manager.ItemManager;
import org.little100.avaritia_Plugin.manager.LanguageManager;

import java.util.ArrayList;
import java.util.List;

public class LanguageCommand implements CommandExecutor, TabCompleter {
    
    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;
    private final ItemManager itemManager;
    
    public LanguageCommand(Avaritia_Plugin plugin, LanguageManager languageManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.itemManager = itemManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // 显示当前语言和可用语言
            String currentLang = languageManager.getPlayerLanguage(player);
            String currentDisplay = languageManager.getLanguageDisplayName(currentLang);
            
            languageManager.sendMessage(player, "language.current", currentDisplay);
            languageManager.sendMessage(player, "language.supported_languages");
            
            for (String lang : languageManager.getSupportedLanguages()) {
                String displayName = languageManager.getLanguageDisplayName(lang);
                String current = lang.equals(currentLang) ? " §a(当前)" : "";
                player.sendMessage("  §7- §e" + lang + " §8(" + displayName + ")" + current);
            }
            
            languageManager.sendMessage(player, "language.usage");
            return true;
        }
        
        if (args.length == 1) {
            String targetLanguage = args[0].toLowerCase();
            
            // 直接切换语言
            if (languageManager.isLanguageSupported(targetLanguage)) {
                if (languageManager.setPlayerLanguage(player, targetLanguage)) {
                    // 更新玩家背包中的所有插件物品
                    itemManager.updatePlayerItems(player);
                    
                    // 使用设置的语言发送消息
                    String displayName = languageManager.getLanguageDisplayName(targetLanguage);
                    languageManager.sendMessage(player, "language.changed", displayName);
                } else {
                    languageManager.sendMessage(player, "language.change_failed");
                }
            } else {
                languageManager.sendMessage(player, "language.unsupported", targetLanguage);
                languageManager.sendMessage(player, "language.usage");
            }
            
            return true;
        }
        
        languageManager.sendMessage(player, "language.usage");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // 只显示支持的语言选项
            for (String lang : languageManager.getSupportedLanguages()) {
                if (lang.toLowerCase().startsWith(input)) {
                    completions.add(lang);
                }
            }
        }
        
        return completions;
    }
}
