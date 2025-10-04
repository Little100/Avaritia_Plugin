package org.little100.avaritia_Plugin.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;

    public ItemManager(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public void updatePlayerItems(Player player) {
        String language = languageManager.getPlayerLanguage(player);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && PDCUtil.isPluginItem(item)) {
                ItemStack updated = updateItemLanguage(item, language);
                if (updated != null) {
                    player.getInventory().setItem(i, updated);
                }
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && PDCUtil.isPluginItem(armor[i])) {
                ItemStack updated = updateItemLanguage(armor[i], language);
                if (updated != null) {
                    armor[i] = updated;
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && PDCUtil.isPluginItem(offHand)) {
            ItemStack updated = updateItemLanguage(offHand, language);
            if (updated != null) {
                player.getInventory().setItemInOffHand(updated);
            }
        }

        for (int i = 0; i < player.getEnderChest().getSize(); i++) {
            ItemStack item = player.getEnderChest().getItem(i);
            if (item != null && PDCUtil.isPluginItem(item)) {
                ItemStack updated = updateItemLanguage(item, language);
                if (updated != null) {
                    player.getEnderChest().setItem(i, updated);
                }
            }
        }

        player.updateInventory();
    }

    private ItemStack updateItemLanguage(ItemStack item, String language) {
        String itemId = PDCUtil.getItemId(item);
        if (itemId == null)
            return null;

        ConfigurationSection itemSection = getItemSectionFromItemsYml(itemId);
        if (itemSection != null) {
            return updateItemFromItemsYml(item, itemId, itemSection, language);
        }

        ConfigurationSection resultSection = getItemSectionFromRecipeYml(itemId);
        if (resultSection != null) {
            return updateItemFromRecipeYml(item, itemId, resultSection, language);
        }

        return null;
    }

    private ConfigurationSection getItemSectionFromItemsYml(String itemId) {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists())
            return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null)
            return null;

        return itemsSection.getConfigurationSection(itemId);
    }

    private ConfigurationSection getItemSectionFromRecipeYml(String itemId) {
        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipeFile.exists())
            return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null)
            return null;

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(itemId);
        if (recipeSection == null)
            return null;

        return recipeSection.getConfigurationSection("result");
    }

    private ItemStack updateItemFromItemsYml(ItemStack item, String itemId, ConfigurationSection section,
            String language) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        String displayNameKey = section.getString("display_name");
        if (displayNameKey != null && displayNameKey.startsWith("items.")) {
            String translatedName = languageManager.getMessage(language, displayNameKey);
            meta.setDisplayName(translatedName);
        }

        List<String> loreKeys = section.getStringList("lore");
        if (!loreKeys.isEmpty()) {
            List<String> translatedLore = new ArrayList<>();
            for (String loreKey : loreKeys) {
                if (loreKey.startsWith("items.")) {
                    translatedLore.add(languageManager.getMessage(language, loreKey));
                } else {
                    translatedLore.add(loreKey);
                }
            }
            meta.setLore(translatedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack updateItemFromRecipeYml(ItemStack item, String itemId, ConfigurationSection resultSection,
            String language) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        String langName = languageManager.getMessage(language, "items." + itemId + ".name");
        if (!langName.startsWith("Missing translation:")) {
            meta.setDisplayName(langName);
        } else if (resultSection.getString("display_name") != null) {
            String displayName = resultSection.getString("display_name");
            if (displayName.startsWith("items.")) {
                meta.setDisplayName(languageManager.getMessage(language, displayName));
            } else {
                meta.setDisplayName(displayName);
            }
        }

        List<String> langLore = new ArrayList<>();
        int loreIndex = 0;
        while (true) {
            String loreLine = languageManager.getMessage(language, "items." + itemId + ".lore." + loreIndex);
            if (loreLine.startsWith("Missing translation:")) {
                break;
            }
            langLore.add(loreLine);
            loreIndex++;
        }

        if (!langLore.isEmpty()) {
            meta.setLore(langLore);
        } else {
            List<String> configLore = resultSection.getStringList("lore");
            if (!configLore.isEmpty()) {
                List<String> translatedLore = new ArrayList<>();
                for (String loreLine : configLore) {
                    if (loreLine.startsWith("items.")) {
                        translatedLore.add(languageManager.getMessage(language, loreLine));
                    } else {
                        translatedLore.add(loreLine);
                    }
                }
                meta.setLore(translatedLore);
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
