package org.little100.avaritia_Plugin.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.avaritia_Plugin.manager.CustomCraftingManager;
import org.little100.avaritia_Plugin.manager.LanguageManager;
import org.little100.avaritia_Plugin.util.CustomModelDataUtil;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合成表管理器
 * 负责加载和注册所有的自定义合成表
 */
public class RecipeManager {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private CustomCraftingManager customCraftingManager;
    private String currentRecipeKey;
    private final Map<String, ItemStack> itemCache = new HashMap<>();

    public RecipeManager(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public void setCustomCraftingManager(CustomCraftingManager manager) {
        this.customCraftingManager = manager;
    }

    /**
     * 加载所有合成表
     */
    public void loadRecipes() {
        File recipesFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipe.yml", false);
        }

        FileConfiguration recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection recipesSection = recipesConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            plugin.getLogger().warning(languageManager.getMessage("zh_cn", "recipe.not_found"));
            return;
        }

        int loadedCount = 0;

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
            if (recipeSection == null)
                continue;

            currentRecipeKey = key;

            ItemStack result = createResultItem(recipeSection);
            if (result == null)
                continue;

            itemCache.put(key, result.clone());

            String recipeType = recipeSection.getString("type", "shaped");
            Recipe recipe;

            if ("shapeless".equals(recipeType)) {
                recipe = createShapelessRecipe(key, result, recipeSection);
            } else {
                recipe = createShapedRecipe(key, result, recipeSection);
            }

            if (recipe == null)
                continue;

            registerRecipe(recipe, key);

            if (customCraftingManager != null && customCraftingManager.needsCustomHandling(recipeSection)) {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("合成表 " + key + " 需要自定义验证（已注册到Bukkit供查看）");
                }
            }
            loadedCount++;
        }

        currentRecipeKey = null;

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info(languageManager.getMessage("zh_cn", "recipe.load_success", loadedCount));
        }
    }

    /**
     * 创建合成表结果物品
     */
    private ItemStack createResultItem(ConfigurationSection recipeSection) {
        ConfigurationSection resultSection = recipeSection.getConfigurationSection("result");
        if (resultSection == null)
            return null;

        Material resultMaterial = Material.getMaterial(resultSection.getString("material", ""));
        if (resultMaterial == null)
            return null;

        String customModelDataString = resultSection.getString("custom_model_data");
        String displayName = resultSection.getString("display_name");
        List<String> lore = resultSection.getStringList("lore");
        int amount = resultSection.getInt("amount", 1);

        ItemStack result = new ItemStack(resultMaterial, amount);

        if (customModelDataString != null) {
            result = CustomModelDataUtil.setCustomModelData(result, customModelDataString);
        }

        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            if (displayName != null) {

                if (displayName.startsWith("items.")) {
                    meta.setDisplayName(languageManager.getMessage("zh_cn", displayName));
                } else {
                    meta.setDisplayName(displayName);
                }
            }
            if (lore != null && !lore.isEmpty()) {

                List<String> translatedLore = new java.util.ArrayList<>();
                for (String loreLine : lore) {
                    if (loreLine.startsWith("items.")) {
                        translatedLore.add(languageManager.getMessage("zh_cn", loreLine));
                    } else {
                        translatedLore.add(loreLine);
                    }
                }
                meta.setLore(translatedLore);
            }
            result.setItemMeta(meta);
        }

        String recipeKey = getCurrentRecipeKey();
        if (recipeKey != null) {
            result = PDCUtil.addPluginData(result, recipeKey);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("已为物品添加PDC标识: " + recipeKey);
            }
        }

        return result;
    }

    private ShapedRecipe createShapedRecipe(String key, ItemStack result, ConfigurationSection recipeSection) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        List<String> shape = recipeSection.getStringList("shape");
        if (shape.isEmpty())
            return null;

        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null)
            return null;

        boolean needsCustomHandling = customCraftingManager != null &&
                customCraftingManager.needsCustomHandling(recipeSection);

        for (String ingredientKey : ingredientsSection.getKeys(false)) {
            String ingredientValue = ingredientsSection.getString(ingredientKey, "");

            String resolvedItemId = resolveItemReference(ingredientValue);

            if (itemCache.containsKey(resolvedItemId)) {
                ItemStack pluginItem = itemCache.get(resolvedItemId);

                if (needsCustomHandling) {

                    RecipeChoice choice = new RecipeChoice.ExactChoice(pluginItem);
                    recipe.setIngredient(ingredientKey.charAt(0), choice);
                } else {

                    recipe.setIngredient(ingredientKey.charAt(0), pluginItem.getType());
                }
            } else {

                Material ingredientMaterial = Material.getMaterial(ingredientValue);
                if (ingredientMaterial != null) {
                    recipe.setIngredient(ingredientKey.charAt(0), ingredientMaterial);
                }
            }
        }

        return recipe;
    }

    private String getCurrentRecipeKey() {
        return currentRecipeKey;
    }

    private String resolveItemReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return reference;
        }

        if (reference.startsWith("avaritia:")) {
            String itemId = reference.substring("avaritia:".length());
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("解析插件物品引用: " + reference + " -> " + itemId);
            }
            return itemId;
        }

        if (reference.contains(":") && !reference.startsWith("avaritia:")) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("检测到外部命名空间物品: " + reference);
            }
            return reference;
        }

        return reference;
    }

    private ShapelessRecipe createShapelessRecipe(String key, ItemStack result, ConfigurationSection recipeSection) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, key);
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, result);

        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null)
            return null;

        boolean needsCustomHandling = customCraftingManager != null &&
                customCraftingManager.needsCustomHandling(recipeSection);

        for (String ingredientKey : ingredientsSection.getKeys(false)) {
            Object ingredientValue = ingredientsSection.get(ingredientKey);

            if (ingredientValue instanceof String) {
                String ingredientName = (String) ingredientValue;

                String resolvedItemId = resolveItemReference(ingredientName);

                if (itemCache.containsKey(resolvedItemId)) {
                    ItemStack pluginItem = itemCache.get(resolvedItemId);

                    if (needsCustomHandling) {
                        RecipeChoice choice = new RecipeChoice.ExactChoice(pluginItem);
                        recipe.addIngredient(choice);
                    } else {
                        recipe.addIngredient(pluginItem.getType());
                    }
                } else {

                    Material ingredientMaterial = Material.getMaterial(ingredientName);
                    if (ingredientMaterial != null) {
                        recipe.addIngredient(ingredientMaterial);
                    }
                }
            } else if (ingredientValue instanceof Integer) {

                int count = (Integer) ingredientValue;

                String resolvedItemId = resolveItemReference(ingredientKey);

                if (itemCache.containsKey(resolvedItemId)) {
                    ItemStack pluginItem = itemCache.get(resolvedItemId);

                    if (needsCustomHandling) {
                        RecipeChoice choice = new RecipeChoice.ExactChoice(pluginItem);
                        for (int i = 0; i < count; i++) {
                            recipe.addIngredient(choice);
                        }
                    } else {
                        for (int i = 0; i < count; i++) {
                            recipe.addIngredient(pluginItem.getType());
                        }
                    }
                } else {

                    Material ingredientMaterial = Material.getMaterial(ingredientKey);
                    if (ingredientMaterial != null) {
                        for (int i = 0; i < count; i++) {
                            recipe.addIngredient(ingredientMaterial);
                        }
                    }
                }
            }
        }

        return recipe;
    }

    /**
     * 注册合成表到服务器
     */
    private void registerRecipe(Recipe recipe, String recipeName) {

        try {
            Bukkit.addRecipe(recipe);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info(languageManager.getMessage("zh_cn", "recipe.loaded", recipeName));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("注册合成表失败 [" + recipeName + "]: " + e.getMessage());
        }
    }
}
