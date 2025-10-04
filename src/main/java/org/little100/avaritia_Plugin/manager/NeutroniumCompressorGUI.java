package org.little100.avaritia_Plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.FoliaUtil;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中子态素压缩机GUI管理器
 */
public class NeutroniumCompressorGUI implements Listener {
    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;
    private final Map<Location, CompressorData> compressors = new ConcurrentHashMap<>();
    private final Map<UUID, Location> viewingCompressors = new ConcurrentHashMap<>();
    private final Map<String, SingularityRecipe> singularityRecipes = new HashMap<>();
    private FileConfiguration config;

    private static class CompressorData {
        String singularityType = null;
        int currentPoints = 0;
    }

    private static class SingularityRecipe {
        String singularityType;
        Map<Material, Integer> materials;
        int requiredPoints;
        String outputCustomModelData;

        SingularityRecipe(String type, Map<Material, Integer> mats, int points, String cmd) {
            this.singularityType = type;
            this.materials = mats;
            this.requiredPoints = points;
            this.outputCustomModelData = cmd;
        }

        boolean acceptsMaterial(Material material) {
            return materials.containsKey(material);
        }

        int getPoints(Material material) {
            return materials.getOrDefault(material, 0);
        }
    }

    public NeutroniumCompressorGUI(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        loadConfig();
        loadRecipes();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "neutronium_compressor.yml");
        if (!configFile.exists()) {
            plugin.saveResource("neutronium_compressor.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadRecipes() {
        ConfigurationSection singularitiesSection = config.getConfigurationSection("singularities");
        if (singularitiesSection == null) {
            plugin.getLogger().warning("压缩机配置文件中没有找到 singularities 配置！");
            return;
        }

        for (String key : singularitiesSection.getKeys(false)) {
            ConfigurationSection recipeSection = singularitiesSection.getConfigurationSection(key);
            if (recipeSection != null) {

                ConfigurationSection materialsSection = recipeSection.getConfigurationSection("materials");
                if (materialsSection == null) {
                    plugin.getLogger().warning("奇点配方 " + key + " 没有materials配置！");
                    continue;
                }

                Map<Material, Integer> materials = new HashMap<>();
                for (String matName : materialsSection.getKeys(false)) {
                    Material mat = Material.getMaterial(matName);
                    int points = materialsSection.getInt(matName);
                    if (mat != null) {
                        materials.put(mat, points);
                        plugin.getLogger().info("  - " + matName + " = " + points + "点");
                    }
                }

                int requiredPoints = recipeSection.getInt("required_points");
                String cmd = recipeSection.getString("output_cmd");

                if (!materials.isEmpty()) {
                    singularityRecipes.put(key, new SingularityRecipe(key, materials, requiredPoints, cmd));
                    plugin.getLogger().info("加载奇点配方: " + key + " (需要" + requiredPoints + "点)");
                }
            }
        }
    }

    /**
     * 注册压缩机
     */
    public void registerCompressor(Location location) {
        compressors.put(location, new CompressorData());
        plugin.getLogger().info("注册中子态素压缩机: " + location);
    }

    /**
     * 注销压缩机
     */
    public void unregisterCompressor(Location location) {
        compressors.remove(location);
        plugin.getLogger().info("注销中子态素压缩机: " + location);
    }

    /**
     * 获取压缩机破坏时应该掉落的物品（转换为低阶材料）
     * 
     * @param location 压缩机位置
     * @return 要掉落的物品列表
     */
    public List<ItemStack> getCompressorDrops(Location location) {
        List<ItemStack> drops = new ArrayList<>();

        CompressorData data = compressors.get(location);
        if (data == null || data.singularityType == null || data.currentPoints <= 0) {

            return drops;
        }

        SingularityRecipe recipe = singularityRecipes.get(data.singularityType);
        if (recipe == null) {
            return drops;
        }

        Material lowestMaterial = null;
        int lowestPoints = Integer.MAX_VALUE;

        for (Map.Entry<Material, Integer> entry : recipe.materials.entrySet()) {
            if (entry.getValue() < lowestPoints) {
                lowestPoints = entry.getValue();
                lowestMaterial = entry.getKey();
            }
        }

        if (lowestMaterial == null || lowestPoints <= 0) {
            return drops;
        }

        int itemCount = data.currentPoints / lowestPoints;

        if (itemCount > 0) {

            while (itemCount > 0) {
                int stackSize = Math.min(itemCount, 64);
                ItemStack drop = new ItemStack(lowestMaterial, stackSize);
                drops.add(drop);
                itemCount -= stackSize;
            }

            plugin.getLogger().info("压缩机 " + location + " 将掉落 " +
                    (data.currentPoints / lowestPoints) + " 个 " + lowestMaterial +
                    " (总点数: " + data.currentPoints + ")");
        }

        return drops;
    }

    /**
     * 打开压缩机GUI
     */
    public void openCompressorGUI(Player player, Location location) {
        try {
            plugin.getLogger().info("尝试为玩家 " + player.getName() + " 打开压缩机GUI，位置: " + location);

            CompressorData data = compressors.get(location);
            if (data == null) {
                data = new CompressorData();
                compressors.put(location, data);
                plugin.getLogger().info("创建新的压缩机数据");
            }

            String title = languageManager.getMessage(player, "neutronium_compressor_gui.title");
            plugin.getLogger().info("GUI标题: " + title);

            Inventory gui = Bukkit.createInventory(null, 9, title);
            plugin.getLogger().info("GUI创建成功");

            updateGUI(gui, data, player);
            plugin.getLogger().info("GUI更新完成");

            viewingCompressors.put(player.getUniqueId(), location);

            player.openInventory(gui);
            plugin.getLogger().info("GUI已打开给玩家");
        } catch (Exception e) {
            plugin.getLogger().severe("打开压缩机GUI时发生异常: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§c打开GUI时发生错误，请查看控制台");
        }
    }

    /**
     * 更新GUI显示
     * 布局：[0=输入] [1-3=进度条] [4=箭头进度] [5-7=进度条] [8=输出]
     */
    private void updateGUI(Inventory gui, CompressorData data, Player player) {

        int percentage = 0;
        if (data.singularityType != null) {
            SingularityRecipe recipe = singularityRecipes.get(data.singularityType);
            if (recipe != null) {
                percentage = (int) ((data.currentPoints * 100.0) / recipe.requiredPoints);
            }
        }

        int greenSlots = (int) Math.floor(percentage / 100.0 * 6);

        int[] progressSlots = { 1, 2, 3, 5, 6, 7 };
        for (int i = 0; i < progressSlots.length; i++) {
            boolean isGreen = i < greenSlots;
            Material material = isGreen ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;

            ItemStack pane = new ItemStack(material);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                pane.setItemMeta(meta);
            }

            gui.setItem(progressSlots[i], pane);
        }

        gui.setItem(4, createArrowItem(data, player));

        gui.setItem(8, createOutputItem(data, player));
    }

    /**
     * 创建箭头进度显示物品
     */
    private ItemStack createArrowItem(CompressorData data, Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (data.singularityType == null) {

            meta.setDisplayName(languageManager.getMessage(player, "neutronium_compressor_gui.no_item"));
            meta.setLore(Arrays.asList(
                    languageManager.getMessage(player, "neutronium_compressor_gui.put_material")));
        } else {

            SingularityRecipe recipe = singularityRecipes.get(data.singularityType);
            if (recipe != null) {

                String singularityName = languageManager.getMessage(player,
                        "items." + data.singularityType + "_singularity.name");
                int percentage = (int) ((data.currentPoints * 100.0) / recipe.requiredPoints);

                String currentItemMsg = MessageFormat.format(
                        languageManager.getMessage(player, "neutronium_compressor_gui.current_item"),
                        singularityName);
                String progressMsg = MessageFormat.format(
                        languageManager.getMessage(player, "neutronium_compressor_gui.progress"),
                        data.currentPoints, recipe.requiredPoints, percentage);

                meta.setDisplayName("§e§l进度: " + percentage + "%");
                meta.setLore(Arrays.asList(
                        "",
                        currentItemMsg,
                        progressMsg,
                        "",
                        languageManager.getMessage(player, "neutronium_compressor_gui.compressing")));
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 创建输出物品
     */
    private ItemStack createOutputItem(CompressorData data, Player player) {
        if (data.singularityType == null || data.currentPoints == 0) {

            return null;
        }

        SingularityRecipe recipe = singularityRecipes.get(data.singularityType);
        if (recipe == null) {
            return null;
        }

        if (data.currentPoints >= recipe.requiredPoints) {

            return createSingularity(recipe.singularityType, player);
        } else {

            return null;
        }
    }

    /**
     * 创建奇点物品
     */
    private ItemStack createSingularity(String singularityType, Player player) {

        String itemId = singularityType + "_singularity";
        return createItemFromId(itemId, player);
    }

    /**
     * 从items.yml创建物品
     */
    private ItemStack createItemFromId(String itemId, Player player) {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemSection = itemsConfig.getConfigurationSection("items." + itemId);

        if (itemSection == null) {
            plugin.getLogger().warning("未找到物品定义: " + itemId);
            return new ItemStack(Material.BARRIER);
        }

        return createItemFromSection(itemSection, player, itemId);
    }

    /**
     * 从配置段创建物品
     */
    private ItemStack createItemFromSection(ConfigurationSection section, Player player, String itemId) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);

        String cmd = section.getString("custom_model_data");
        if (cmd != null) {
            item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
        }

        ItemMeta meta = item.getItemMeta();
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

        item = PDCUtil.addPluginData(item, itemId);

        return item;
    }

    /**
     * 获取材料名称
     */
    private String getMaterialName(Material material, Player player) {
        String key = "materials." + material.name();
        String name = languageManager.getMessage(player, key);
        if (!name.equals(key)) {
            return name;
        }

        return formatMaterialName(material.name());
    }

    /**
     * 格式化材料名称
     */
    private String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
    private SingularityRecipe findRecipeByMaterial(Material material) {
        for (SingularityRecipe recipe : singularityRecipes.values()) {
            if (recipe.acceptsMaterial(material)) {
                return recipe;
            }
        }
        return null;
    }
    private String getMaterialSingularityType(Material material) {
        SingularityRecipe recipe = findRecipeByMaterial(material);
        return recipe != null ? recipe.singularityType : null;
    }
    private boolean canAcceptMaterial(String singularityType, Material material) {
        SingularityRecipe recipe = singularityRecipes.get(singularityType);
        return recipe != null && recipe.acceptsMaterial(material);
    }
    private boolean isPluginItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return PDCUtil.isPluginItem(item);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        String expectedTitle = languageManager.getMessage(player, "neutronium_compressor_gui.title");

        if (!title.equals(expectedTitle))
            return;

        int slot = event.getRawSlot();
        Location loc = viewingCompressors.get(player.getUniqueId());
        if (loc == null)
            return;

        CompressorData data = compressors.get(loc);
        if (data == null)
            return;

        if (slot >= 9) {

            if (event.isShiftClick()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {

                    if (isPluginItem(clicked)) {

                        return;
                    }

                    String singularityType = getMaterialSingularityType(clicked.getType());
                    if (singularityType != null) {
                        event.setCancelled(true);

                        if (data.singularityType == null || data.singularityType.equals(singularityType)) {
                            ItemStack inputSlot = event.getInventory().getItem(0);

                            int spaceLeft = 64;
                            if (inputSlot != null && inputSlot.getType() != Material.AIR) {
                                if (inputSlot.getType() == clicked.getType()) {
                                    spaceLeft = 64 - inputSlot.getAmount();
                                } else {
                                    return;
                                }
                            }

                            int amountToMove = Math.min(clicked.getAmount(), spaceLeft);
                            if (amountToMove > 0) {

                                if (inputSlot == null || inputSlot.getType() == Material.AIR) {
                                    inputSlot = clicked.clone();
                                    inputSlot.setAmount(amountToMove);
                                    event.getInventory().setItem(0, inputSlot);
                                } else {
                                    inputSlot.setAmount(inputSlot.getAmount() + amountToMove);
                                }

                                if (clicked.getAmount() <= amountToMove) {
                                    event.setCurrentItem(new ItemStack(Material.AIR));
                                } else {
                                    clicked.setAmount(clicked.getAmount() - amountToMove);
                                }

                                handleInputItem(player, loc, data, event.getInventory().getItem(0),
                                        event.getInventory());
                            }
                        } else {
                            player.sendMessage("§c压缩机正在处理其他奇点，无法放入不同类型的材料！");
                        }
                    }
                }
            }
            return;
        }

        if (slot == 0) {

            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            if (cursor != null && cursor.getType() != Material.AIR) {

                if (isPluginItem(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c插件物品不能放入压缩机！");
                    return;
                }

                String singularityType = getMaterialSingularityType(cursor.getType());
                if (singularityType == null) {
                    event.setCancelled(true);
                    player.sendMessage("§c这不是有效的压缩材料！");
                    return;
                }

                if (data.singularityType != null && !data.singularityType.equals(singularityType)) {
                    event.setCancelled(true);
                    player.sendMessage("§c压缩机正在处理其他奇点，无法放入不同类型的材料！");
                    return;
                }
            }

            event.setCancelled(false);

            final Inventory guiInv = event.getInventory();

            FoliaUtil.runAtLocationDelayed(plugin, loc, () -> {
                ItemStack inputItem = guiInv.getItem(0);
                if (inputItem != null && inputItem.getType() != Material.AIR) {

                    if (isPluginItem(inputItem)) {

                        guiInv.setItem(0, null);
                        player.getInventory().addItem(inputItem);
                        player.sendMessage("§c插件物品不能放入压缩机！");
                        return;
                    }

                    String type = getMaterialSingularityType(inputItem.getType());
                    if (type == null) {

                        guiInv.setItem(0, null);
                        player.getInventory().addItem(inputItem);
                        player.sendMessage("§c这不是有效的压缩材料！");
                        return;
                    }

                    if (data.singularityType != null && !data.singularityType.equals(type)) {

                        guiInv.setItem(0, null);
                        player.getInventory().addItem(inputItem);
                        player.sendMessage("§c压缩机正在处理其他奇点，无法放入不同类型的材料！当前: §e" +
                                languageManager.getMessage(player,
                                        "items." + data.singularityType + "_singularity.name"));
                        return;
                    }

                    handleInputItem(player, loc, data, inputItem, guiInv);
                }

            }, 1L);
        } else if (slot == 8) {

            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            if (cursor != null && cursor.getType() != Material.AIR) {

                event.setCancelled(true);
                return;
            }

            if (current != null && current.getType() != Material.AIR) {

                event.setCancelled(false);

                FoliaUtil.runAtLocationDelayed(plugin, loc, () -> {
                    data.singularityType = null;
                    data.currentPoints = 0;

                    if (viewingCompressors.containsKey(player.getUniqueId())) {
                        updateGUI(player.getOpenInventory().getTopInventory(), data, player);
                    }
                    player.sendMessage(languageManager.getMessage(player, "neutronium_compressor_gui.collected"));
                }, 1L);
            } else {
                event.setCancelled(true);
            }
        } else {

            event.setCancelled(true);
        }
    }

    /**
     * 处理输入物品
     */
    private void handleInputItem(Player player, Location loc, CompressorData data, ItemStack inputItem, Inventory gui) {

        if (isPluginItem(inputItem)) {
            player.sendMessage("§c插件物品不能放入压缩机！");
            gui.setItem(0, null);
            player.getInventory().addItem(inputItem);
            return;
        }

        Material material = inputItem.getType();
        SingularityRecipe recipe = findRecipeByMaterial(material);

        if (recipe == null) {

            player.sendMessage("§c这不是有效的压缩材料！");
            gui.setItem(0, null);
            player.getInventory().addItem(inputItem);
            return;
        }

        if (data.singularityType != null && !data.singularityType.equals(recipe.singularityType)) {
            player.sendMessage("§c不能混合不同的奇点材料！当前正在压缩: §e" +
                    languageManager.getMessage(player, "items." + data.singularityType + "_singularity.name"));

            gui.setItem(0, null);
            player.getInventory().addItem(inputItem);
            return;
        }

        int pointsPerItem = recipe.getPoints(material);
        int itemCount = inputItem.getAmount();
        int addedPoints = pointsPerItem * itemCount;

        if (data.singularityType == null) {
            data.singularityType = recipe.singularityType;
        }

        data.currentPoints += addedPoints;

        gui.setItem(0, null);

        updateGUI(gui, data, player);

        String materialName = getMaterialName(material, player);
        player.sendMessage("§a已添加 §e" + itemCount + " §a个 §e" + materialName +
                " §a(+" + addedPoints + "点) §a到压缩机！ (§e" + data.currentPoints + "/" + recipe.requiredPoints + "§a)");
    }

    /**
     * 关闭GUI时清理
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();

        String title = event.getView().getTitle();
        String expectedTitle = languageManager.getMessage(player, "neutronium_compressor_gui.title");

        if (title.equals(expectedTitle)) {
            viewingCompressors.remove(player.getUniqueId());
        }
    }
}
