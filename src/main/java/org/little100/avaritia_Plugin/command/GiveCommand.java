package org.little100.avaritia_Plugin.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.manager.LanguageManager;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GiveCommand {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;

    public GiveCommand(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avaritia.give") && !sender.isOp()) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "command.no_permission");
            } else {
                sender.sendMessage("§c你没有权限执行此命令!");
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /avaritia give <玩家> <物品ID> [数量]");
            sender.sendMessage("§7示例: /avaritia give @p neutron_dust 64");
            return true;
        }

        String targetName = args[0];
        String itemId = args[1];
        int amount = 1;

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§c数量必须在1-64之间！");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的数量: " + args[2]);
                return true;
            }
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c玩家不在线: " + targetName);
            return true;
        }

        ItemStack item = createItem(itemId, target, amount);
        if (item == null) {
            sender.sendMessage("§c未找到物品: " + itemId);
            sender.sendMessage("§7使用 /avaritia give list 查看可用物品");
            return true;
        }

        target.getInventory().addItem(item);

        String itemName = item.getItemMeta() != null ? item.getItemMeta().getDisplayName() : itemId;
        sender.sendMessage("§a已给予 " + target.getName() + " §r" + itemName + " §ax" + amount);
        target.sendMessage("§a你获得了 §r" + itemName + " §ax" + amount);

        return true;
    }

    private ItemStack createItem(String itemId, Player player, int amount) {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemSection = itemsConfig.getConfigurationSection("items." + itemId);

        if (itemSection != null) {
            return createItemFromSection(itemSection, player, itemId, amount);
        }
        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (recipeFile.exists()) {
            FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
            ConfigurationSection recipeSection = recipeConfig.getConfigurationSection("recipes." + itemId + ".result");

            if (recipeSection != null) {
                return createItemFromRecipeResult(recipeSection, player, itemId, amount);
            }
        }

        return null;
    }

    private ItemStack createItemFromSection(ConfigurationSection section, Player player, String itemId, int amount) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, amount);

        if (section.contains("custom_model_data")) {
            String cmd;
            if (section.isInt("custom_model_data")) {
                cmd = String.valueOf(section.getInt("custom_model_data"));
            } else {
                cmd = section.getString("custom_model_data");
            }

            if (cmd != null && !cmd.isEmpty()) {
                item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[Give命令] 为物品 " + itemId + " 设置CMD: " + cmd);
                }
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayNameKey = section.getString("display_name");
            if (displayNameKey != null) {
                String displayName = languageManager.getMessage(player, displayNameKey);
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            }

            List<String> loreKeys = section.getStringList("lore");
            if (!loreKeys.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String loreKey : loreKeys) {
                    String loreLine = languageManager.getMessage(player, loreKey);
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', loreLine));
                }
                meta.setLore(lore);
            }

            if (section.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }

            if (section.contains("max_durability")) {
                int maxDurability = section.getInt("max_durability");
                if (maxDurability > 0 && meta.getLore() != null) {
                    List<String> currentLore = new ArrayList<>(meta.getLore());
                    currentLore.add("");
                    currentLore.add(org.bukkit.ChatColor.GRAY + "耐久: " + org.bukkit.ChatColor.GREEN + maxDurability
                            + "/" + maxDurability);
                    meta.setLore(currentLore);
                }
            }

            item.setItemMeta(meta);
        }

        item = PDCUtil.addPluginData(item, itemId);

        if (plugin.getConfig().getBoolean("debug", true)) {
            boolean hasPluginData = PDCUtil.isPluginItem(item);
            String storedItemId = PDCUtil.getItemId(item);
            plugin.getLogger().info("[Give命令] 物品PDC验证 - 是插件物品: " + hasPluginData + ", 物品ID: " + storedItemId);
        }

        return item;
    }

    private ItemStack createItemFromRecipeResult(ConfigurationSection section, Player player, String itemId,
            int amount) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, amount);

        if (section.contains("custom_model_data")) {
            String cmd;
            if (section.isInt("custom_model_data")) {
                cmd = String.valueOf(section.getInt("custom_model_data"));
            } else {
                cmd = section.getString("custom_model_data");
            }

            if (cmd != null && !cmd.isEmpty()) {
                item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[Give命令-Recipe] 为物品 " + itemId + " 设置CMD: " + cmd);
                }
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayNameKey = section.getString("display_name");
            if (displayNameKey != null) {
                String displayName = languageManager.getMessage(player, displayNameKey);
                meta.setDisplayName(displayName);
            }

            List<String> loreKeys = section.getStringList("lore");
            if (!loreKeys.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String loreKey : loreKeys) {
                    lore.add(languageManager.getMessage(player, loreKey));
                }
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        item = PDCUtil.addPluginData(item, itemId);
        if (plugin.getConfig().getBoolean("debug", true)) {
            boolean hasPluginData = PDCUtil.isPluginItem(item);
            String storedItemId = PDCUtil.getItemId(item);
            plugin.getLogger().info("[Give命令-Recipe] 物品PDC验证 - 是插件物品: " + hasPluginData + ", 物品ID: " + storedItemId);
        }

        return item;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            List<String> itemIds = getAllItemIds();
            for (String itemId : itemIds) {
                if (itemId.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(itemId);
                }
            }
        } else if (args.length == 3) {
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        return completions;
    }

    private List<String> getAllItemIds() {
        List<String> itemIds = new ArrayList<>();

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (itemsFile.exists()) {
            FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
            if (itemsSection != null) {
                itemIds.addAll(itemsSection.getKeys(false));
            }
        }

        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (recipeFile.exists()) {
            FileConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
            ConfigurationSection recipesSection = recipeConfig.getConfigurationSection("recipes");
            if (recipesSection != null) {
                for (String key : recipesSection.getKeys(false)) {
                    if (!key.contains("_to_") && !key.contains("_from_") && !itemIds.contains(key)) {
                        itemIds.add(key);
                    }
                }
            }
        }
        return itemIds;
    }
}
