package org.little100.avaritia_Plugin.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomCraftingManager {

    private final JavaPlugin plugin;
    private final Map<String, CustomRecipe> customRecipes = new HashMap<>();
    private final Map<String, ItemStack> itemCache = new HashMap<>();
    private LanguageManager languageManager;

    public CustomCraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    public void setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;

        loadCustomRecipes();
    }

    private static class CustomRecipe {
        public final String id;
        public final ItemStack result;
        public final List<String> shape;
        public final Map<Character, String> ingredients;
        public final boolean isShapeless;

        public CustomRecipe(String id, ItemStack result, List<String> shape, Map<Character, String> ingredients,
                boolean isShapeless) {
            this.id = id;
            this.result = result;
            this.shape = shape;
            this.ingredients = ingredients;
            this.isShapeless = isShapeless;
        }
    }

    private void loadCustomRecipes() {
        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipeFile.exists()) {
            plugin.saveResource("recipe.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");

        if (recipesSection == null) {
            plugin.getLogger().warning("recipe.yml中没有找到recipes部分");
            return;
        }

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
            if (recipeSection == null)
                continue;

            ItemStack resultItem = createItemFromConfig(recipeSection.getConfigurationSection("result"), key);
            if (resultItem != null) {
                itemCache.put(key, resultItem);
            }
        }

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
            if (recipeSection == null)
                continue;

            if (needsCustomHandling(recipeSection)) {
                CustomRecipe customRecipe = createCustomRecipe(key, recipeSection);
                if (customRecipe != null) {
                    customRecipes.put(key, customRecipe);
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("加载自定义合成表: " + key);
                    }
                }
            }
        }

        plugin.getLogger().info("已加载 " + customRecipes.size() + " 个自定义合成表");
    }

    private ItemStack createItemFromConfig(ConfigurationSection resultSection, String recipeKey) {
        return createItemFromConfig(resultSection, recipeKey, "zh_cn");
    }

    private ItemStack createItemFromConfig(ConfigurationSection resultSection, String recipeKey, String language) {
        if (resultSection == null)
            return null;

        Material material = Material.getMaterial(resultSection.getString("material", ""));
        if (material == null)
            return null;

        int amount = resultSection.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);

        String cmd = resultSection.getString("custom_model_data");
        if (cmd != null) {
            item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
        }

        var meta = item.getItemMeta();
        if (meta != null) {

            String itemId = recipeKey;
            String configDisplayName = resultSection.getString("display_name");
            if (configDisplayName != null && configDisplayName.startsWith("items.")
                    && configDisplayName.endsWith(".name")) {

                itemId = configDisplayName.substring(6, configDisplayName.length() - 5);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[物品创建] 从display_name提取物品ID: " + itemId + " (配方ID: " + recipeKey + ")");
                }
            }

            if (languageManager != null) {
                String langName = languageManager.getMessage(language, "items." + itemId + ".name");
                if (!langName.startsWith("Missing translation:")) {
                    meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', langName));
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("[物品创建] " + recipeKey + " 名称: " + langName);
                    }
                } else if (configDisplayName != null) {
                    meta.setDisplayName(configDisplayName);
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().warning("[物品创建] " + recipeKey + " 使用配置名称(翻译缺失): " + configDisplayName);
                    }
                }

                List<String> langLore = new ArrayList<>();
                int loreIndex = 0;
                while (true) {
                    String loreLine = languageManager.getMessage(language, "items." + itemId + ".lore." + loreIndex);
                    if (loreLine.startsWith("Missing translation:")) {
                        break;
                    }
                    langLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', loreLine));
                    loreIndex++;
                }

                if (!langLore.isEmpty()) {
                    meta.setLore(langLore);
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("[物品创建] " + recipeKey + " 使用翻译lore, 行数: " + langLore.size());
                    }
                } else {
                    List<String> configLore = resultSection.getStringList("lore");
                    if (!configLore.isEmpty()) {
                        meta.setLore(configLore);
                        if (plugin.getConfig().getBoolean("debug", true)) {
                            plugin.getLogger().warning("[物品创建] " + recipeKey + " 使用配置lore(翻译缺失): " + configLore);
                        }
                    }
                }
            } else {

                if (resultSection.getString("display_name") != null) {
                    meta.setDisplayName(resultSection.getString("display_name"));
                }

                List<String> lore = resultSection.getStringList("lore");
                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }
            }

            if (resultSection.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[物品创建] " + recipeKey + " 设置为不可破坏");
                }
            }

            if (resultSection.contains("max_durability")) {
                int maxDurability = resultSection.getInt("max_durability");
                if (meta instanceof org.bukkit.inventory.meta.Damageable) {

                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("[物品创建] " + recipeKey + " 最大耐久: " + maxDurability);
                    }
                }
            }

            if (resultSection.contains("enchantments")) {
                ConfigurationSection enchantSection = resultSection.getConfigurationSection("enchantments");
                if (enchantSection != null) {
                    for (String enchantName : enchantSection.getKeys(false)) {
                        int level = enchantSection.getInt(enchantName);
                        try {
                            org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment
                                    .getByName(enchantName);
                            if (enchant != null) {
                                meta.addEnchant(enchant, level, true);
                                if (plugin.getConfig().getBoolean("debug", true)) {
                                    plugin.getLogger()
                                            .info("[物品创建] " + recipeKey + " 添加附魔: " + enchantName + " " + level);
                                }
                            } else {
                                plugin.getLogger().warning("[物品创建] " + recipeKey + " 未知附魔: " + enchantName);
                            }
                        } catch (Exception e) {
                            plugin.getLogger()
                                    .warning("[物品创建] " + recipeKey + " 附魔错误: " + enchantName + " - " + e.getMessage());
                        }
                    }
                }
            }

            if (resultSection.contains("attack_damage")) {
                double attackDamage = resultSection.getDouble("attack_damage");
                try {

                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "attack_damage");
                    org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                            key,
                            attackDamage,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            org.bukkit.inventory.EquipmentSlotGroup.HAND);

                    org.bukkit.attribute.Attribute attackAttribute = org.bukkit.Registry.ATTRIBUTE.get(
                            org.bukkit.NamespacedKey.minecraft("generic.attack_damage"));

                    if (attackAttribute != null) {
                        meta.addAttributeModifier(attackAttribute, modifier);
                        if (plugin.getConfig().getBoolean("debug", true)) {
                            plugin.getLogger().info("[物品创建] " + recipeKey + " 设置攻击力: " + attackDamage);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[物品创建] " + recipeKey + " 攻击力设置错误: " + e.getMessage());
                }
            }

            item.setItemMeta(meta);
        }

        String itemId = recipeKey;
        String configDisplayName = resultSection.getString("display_name");
        if (configDisplayName != null && configDisplayName.startsWith("items.")
                && configDisplayName.endsWith(".name")) {
            itemId = configDisplayName.substring(6, configDisplayName.length() - 5);
        }
        item = PDCUtil.addPluginData(item, itemId);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[物品创建] " + recipeKey + " PDC标识: " + itemId);
        }

        return item;
    }

    public boolean needsCustomHandling(ConfigurationSection recipeSection) {
        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null)
            return false;

        for (String key : ingredientsSection.getKeys(false)) {
            Object value = ingredientsSection.get(key);
            if (value instanceof String) {
                String ingredient = (String) value;

                if (ingredient.startsWith("avaritia:") || itemCache.containsKey(ingredient)) {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("合成表需要自定义处理，因为材料包含: " + ingredient);
                    }
                    return true;
                }
            } else if (value instanceof Integer) {

                if (key.startsWith("avaritia:") || itemCache.containsKey(key)) {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("无序合成表需要自定义处理，因为材料包含: " + key);
                    }
                    return true;
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("该合成表使用纯原版材料，由Bukkit处理");
        }

        return false;
    }

    private CustomRecipe createCustomRecipe(String key, ConfigurationSection recipeSection) {
        ItemStack result = itemCache.get(key);
        if (result == null)
            return null;

        String type = recipeSection.getString("type", "shaped");
        boolean isShapeless = "shapeless".equals(type);

        List<String> shape = null;
        Map<Character, String> ingredients = new HashMap<>();

        if (!isShapeless) {

            shape = recipeSection.getStringList("shape");
            if (shape.isEmpty())
                return null;
        }

        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                Object value = ingredientsSection.get(ingredientKey);

                if (value instanceof String) {
                    String ingredient = (String) value;
                    if (ingredient.startsWith("avaritia:")) {
                        ingredient = ingredient.substring("avaritia:".length());
                    }
                    ingredients.put(ingredientKey.charAt(0), ingredient);
                } else if (value instanceof Integer) {

                    String cleanKey = ingredientKey;
                    if (cleanKey.startsWith("avaritia:")) {
                        cleanKey = cleanKey.substring("avaritia:".length());
                    }
                    ingredients.put(ingredientKey.charAt(0), cleanKey + ":" + value);
                }
            }
        }

        return new CustomRecipe(key, result.clone(), shape, ingredients, isShapeless);
    }

    public ItemStack findMatchingRecipe(ItemStack[] matrix) {
        return findMatchingRecipe(matrix, "zh_cn");
    }

    public ItemStack findMatchingRecipe(ItemStack[] matrix, String language) {
        for (Map.Entry<String, CustomRecipe> entry : customRecipes.entrySet()) {
            CustomRecipe recipe = entry.getValue();
            if (recipe.isShapeless) {
                if (matchesShapelessRecipe(matrix, recipe)) {

                    return createLocalizedResult(entry.getKey(), language);
                }
            } else {
                if (matchesShapedRecipe(matrix, recipe)) {

                    return createLocalizedResult(entry.getKey(), language);
                }
            }
        }
        return null;
    }

    private ItemStack createLocalizedResult(String recipeKey, String language) {
        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipeFile.exists())
            return itemCache.get(recipeKey);

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null)
            return itemCache.get(recipeKey);

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeKey);
        if (recipeSection == null)
            return itemCache.get(recipeKey);

        return createItemFromConfig(recipeSection.getConfigurationSection("result"), recipeKey, language);
    }

    private boolean matchesShapedRecipe(ItemStack[] matrix, CustomRecipe recipe) {
        if (recipe.shape == null)
            return false;

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("开始验证有序合成: " + recipe.id);
        }

        String[] currentPattern = new String[3];
        boolean hasUnknownItem = false;

        for (int row = 0; row < 3; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                ItemStack item = matrix[row * 3 + col];
                char symbol = getSymbolForItem(item, recipe);

                if (symbol == '?') {
                    hasUnknownItem = true;
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        String itemDesc = item != null ? getItemIdentifier(item) : "null";
                        plugin.getLogger().info("位置 [" + row + "," + col + "] 有未知物品: " + itemDesc);
                    }
                }

                sb.append(symbol);
            }
            currentPattern[row] = sb.toString();
        }

        if (hasUnknownItem) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("合成失败：包含不匹配的材料");
            }
            return false;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("当前模式: " + String.join("|", currentPattern));
            plugin.getLogger().info("目标模式: " + String.join("|", recipe.shape));
        }

        boolean matches = matchesPattern(currentPattern, recipe.shape);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("模式匹配结果: " + matches);
        }

        return matches;
    }

    private boolean matchesShapelessRecipe(ItemStack[] matrix, CustomRecipe recipe) {
        Map<String, Integer> required = new HashMap<>();
        Map<String, Integer> provided = new HashMap<>();

        for (Map.Entry<Character, String> entry : recipe.ingredients.entrySet()) {
            String ingredient = entry.getValue();

            int lastColonIndex = ingredient.lastIndexOf(':');
            if (lastColonIndex > 0 && lastColonIndex < ingredient.length() - 1) {
                try {
                    String materialId = ingredient.substring(0, lastColonIndex);
                    int count = Integer.parseInt(ingredient.substring(lastColonIndex + 1));
                    required.put(materialId, required.getOrDefault(materialId, 0) + count);
                    continue;
                } catch (NumberFormatException e) {

                }
            }

            required.put(ingredient, required.getOrDefault(ingredient, 0) + 1);
        }

        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                String itemId = getItemIdentifier(item);
                if (itemId != null) {
                    provided.put(itemId, provided.getOrDefault(itemId, 0) + item.getAmount());
                }
            }
        }

        return required.equals(provided);
    }

    private char getSymbolForItem(ItemStack item, CustomRecipe recipe) {
        if (item == null || item.getType().isAir()) {
            return ' ';
        }

        String itemId = getItemIdentifier(item);
        for (Map.Entry<Character, String> entry : recipe.ingredients.entrySet()) {
            String requiredId = entry.getValue();

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("验证物品: " + itemId + " vs 需要: " + requiredId);
            }

            if (requiredId.equals(itemId)) {
                return entry.getKey();
            }
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("物品 " + itemId + " 不匹配任何合成材料");
        }

        return '?';
    }

    private String getItemIdentifier(ItemStack item) {
        if (item == null || item.getType().isAir())
            return null;

        if (PDCUtil.isPluginItem(item)) {
            return PDCUtil.getItemId(item);
        }

        return item.getType().name();
    }

    private boolean matchesPattern(String[] current, List<String> target) {

        String[] trimmedCurrent = trimPattern(current);
        String[] trimmedTarget = trimPattern(target.toArray(new String[0]));

        if (trimmedCurrent.length != trimmedTarget.length)
            return false;

        for (int i = 0; i < trimmedCurrent.length; i++) {
            if (!trimmedCurrent[i].equals(trimmedTarget[i])) {
                return false;
            }
        }

        return true;
    }

    private String[] trimPattern(String[] pattern) {
        if (pattern == null || pattern.length == 0)
            return new String[0];

        int firstRow = -1, lastRow = -1;
        for (int i = 0; i < pattern.length; i++) {
            if (!pattern[i].replace(" ", "").isEmpty()) {
                if (firstRow == -1)
                    firstRow = i;
                lastRow = i;
            }
        }

        if (firstRow == -1)
            return new String[] { " " };

        int firstCol = Integer.MAX_VALUE, lastCol = -1;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < pattern[row].length(); col++) {
                if (pattern[row].charAt(col) != ' ') {
                    firstCol = Math.min(firstCol, col);
                    lastCol = Math.max(lastCol, col);
                }
            }
        }

        if (lastCol == -1)
            return new String[] { " " };

        String[] trimmed = new String[lastRow - firstRow + 1];
        for (int i = 0; i < trimmed.length; i++) {
            String row = pattern[firstRow + i];
            if (lastCol + 1 <= row.length()) {
                trimmed[i] = row.substring(firstCol, lastCol + 1);
            } else {

                StringBuilder sb = new StringBuilder();
                for (int col = firstCol; col <= lastCol; col++) {
                    sb.append(col < row.length() ? row.charAt(col) : ' ');
                }
                trimmed[i] = sb.toString();
            }
        }

        return trimmed;
    }

    public boolean validateRecipe(ItemStack[] matrix, ItemStack result) {
        String resultId = PDCUtil.getItemId(result);
        if (resultId == null)
            return false;

        CustomRecipe recipe = customRecipes.get(resultId);
        if (recipe == null)
            return false;

        return recipe.isShapeless ? matchesShapelessRecipe(matrix, recipe) : matchesShapedRecipe(matrix, recipe);
    }

    public void consumeIngredients(ItemStack[] matrix, ItemStack result) {
        String resultId = PDCUtil.getItemId(result);
        if (resultId == null)
            return;

        CustomRecipe recipe = customRecipes.get(resultId);
        if (recipe == null)
            return;

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null && !matrix[i].getType().isAir()) {
                matrix[i].setAmount(matrix[i].getAmount() - 1);
                if (matrix[i].getAmount() <= 0) {
                    matrix[i] = null;
                }
            }
        }
    }

    public void reload() {
        customRecipes.clear();
        itemCache.clear();
        loadCustomRecipes();
    }

    private void reloadItems() {
        if (languageManager == null)
            return;

        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipeFile.exists())
            return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null)
            return;

        itemCache.clear();
        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
            if (recipeSection == null)
                continue;

            ItemStack resultItem = createItemFromConfig(recipeSection.getConfigurationSection("result"), key, "zh_cn");
            if (resultItem != null) {
                itemCache.put(key, resultItem);
            }
        }
    }

    public ItemStack getLocalizedItem(String itemId, String language) {
        if (languageManager == null || itemId == null)
            return itemCache.get(itemId);

        File recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        if (!recipeFile.exists())
            return itemCache.get(itemId);

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null)
            return itemCache.get(itemId);

        ConfigurationSection recipeSection = recipesSection.getConfigurationSection(itemId);
        if (recipeSection == null)
            return itemCache.get(itemId);

        return createItemFromConfig(recipeSection.getConfigurationSection("result"), itemId, language);
    }

    public ItemStack createLocalizedItem(ItemStack item, String language) {
        if (item == null || !PDCUtil.isPluginItem(item))
            return null;

        String itemId = PDCUtil.getItemId(item);
        if (itemId == null)
            return null;

        if (itemId.contains(":")) {
            itemId = itemId.substring(itemId.indexOf(":") + 1);
        }

        return getLocalizedItem(itemId, language);
    }
}
