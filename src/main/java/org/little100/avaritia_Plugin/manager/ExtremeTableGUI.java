package org.little100.avaritia_Plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ExtremeTableGUI implements Listener {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;

    private final Map<String, Map<String, Integer>> recipes = new HashMap<>();

    private final Map<UUID, String> playerCurrentRecipe = new HashMap<>();

    private final Map<String, Long> craftCooldown = new HashMap<>();
    private static final long CRAFT_COOLDOWN_MS = 500;

    private static final String MAIN_GUI_TITLE = "§6§l终极工作台";
    private static final String CRAFT_GUI_PREFIX = "§6合成: ";

    public ExtremeTableGUI(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        loadRecipes();
    }

    private void loadRecipes() {
        File recipeFile = new File(plugin.getDataFolder(), "extreme_crafting_table.yml");

        if (!recipeFile.exists()) {
            plugin.saveResource("extreme_crafting_table.yml", false);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("已复制 extreme_crafting_table.yml 到插件文件夹");
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);

        for (String itemId : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(itemId);
            if (section != null) {
                Map<String, Integer> ingredients = new HashMap<>();
                for (String materialKey : section.getKeys(false)) {
                    ingredients.put(materialKey, section.getInt(materialKey));
                }
                recipes.put(itemId, ingredients);

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("加载终极工作台配方: " + itemId + " -> " + ingredients);
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("终极工作台配方加载完成，共 " + recipes.size() + " 个配方");
        }
    }

    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, MAIN_GUI_TITLE);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("打开终极工作台主GUI，配方数量: " + recipes.size());
        }

        int slot = 0;
        for (String itemId : recipes.keySet()) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("尝试创建物品: " + itemId);
            }

            ItemStack item = createItemFromId(itemId, player);
            if (item != null && slot < 54) {
                gui.setItem(slot++, item);

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("成功添加物品到槽位 " + (slot - 1) + ": " + itemId);
                }
            }
        }

        if (slot == 0 && plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().warning("主GUI没有添加任何物品！");
        }

        player.openInventory(gui);
    }

    private void openCraftGUI(Player player, String itemId) {
        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== 打开合成GUI ===");
            plugin.getLogger().info("玩家: " + player.getName());
            plugin.getLogger().info("物品ID: " + itemId);
            plugin.getLogger().info("GUI标题: " + CRAFT_GUI_PREFIX + getItemName(itemId, player));
        }

        Inventory gui = Bukkit.createInventory(null, 54, CRAFT_GUI_PREFIX + getItemName(itemId, player));

        gui.setItem(0, createInfoCompass(player, itemId));

        ItemStack glassPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glassPane.setItemMeta(glassMeta);
        }

        for (int i = 1; i < 9; i++)
            gui.setItem(i, glassPane);

        for (int i = 45; i < 54; i++) {
            if (i != 49)
                gui.setItem(i, glassPane);
        }

        for (int i = 9; i < 45; i += 9) {
            gui.setItem(i, glassPane);
            gui.setItem(i + 8, glassPane);
        }

        gui.setItem(49, createBarrier(player));

        player.openInventory(gui);
    }

    private ItemStack createInfoCompass(Player player, String itemId) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            String itemName = getItemName(itemId, player);
            String displayName = languageManager.getMessage(player, "extreme_table_gui.current_item")
                    .replace("{0}", itemName);
            meta.setDisplayName(displayName);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(languageManager.getMessage(player, "extreme_table_gui.required_materials"));

            Map<String, Integer> ingredients = recipes.get(itemId);
            if (ingredients != null) {
                for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                    String materialName = getMaterialName(entry.getKey(), player);
                    lore.add("§f  • " + materialName + " §7x" + entry.getValue());
                }
            }

            lore.add("");
            lore.add(languageManager.getMessage(player, "extreme_table_gui.put_materials"));
            lore.add(languageManager.getMessage(player, "extreme_table_gui.click_to_craft"));

            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    private ItemStack createBarrier(Player player) {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(player, "extreme_table_gui.materials_insufficient"));
            meta.setLore(Arrays.asList(languageManager.getMessage(player, "extreme_table_gui.put_required_materials")));
            barrier.setItemMeta(meta);
        }
        return barrier;
    }

    private ItemStack createEmeraldBlock(Player player) {
        ItemStack emerald = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = emerald.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(player, "extreme_table_gui.click_craft"));
            meta.setLore(Arrays.asList(
                    languageManager.getMessage(player, "extreme_table_gui.materials_satisfied"),
                    languageManager.getMessage(player, "extreme_table_gui.click_to_craft")));
            emerald.setItemMeta(meta);
        }
        return emerald;
    }

    private boolean checkMaterials(Inventory inv, String itemId) {

        if (plugin.getConfig().getBoolean("extreme_crafting_table.debug", false)) {

            for (int i = 0; i < inv.getSize(); i++) {
                if (isDecorativeSlot(i))
                    continue;
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("[终极工作台Debug] 检测到物品，允许合成: " + itemId);
                    }
                    return true;
                }
            }
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[终极工作台Debug] 工作区为空，不允许合成");
            }
            return false;
        }

        Map<String, Integer> required = recipes.get(itemId);
        if (required == null) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().warning("配方不存在: " + itemId);
            }
            return false;
        }

        Map<String, Integer> available = new HashMap<>();

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== 开始扫描GUI物品 ===");
            plugin.getLogger().info("GUI大小: " + inv.getSize());
        }

        for (int i = 0; i < inv.getSize(); i++) {

            if (isDecorativeSlot(i)) {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        plugin.getLogger().info("槽位 " + i + ": " + item.getType() + " (装饰格子，已跳过)");
                    }
                }
                continue;
            }

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                continue;

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("槽位 " + i + ": " + item.getType() + " × " + item.getAmount());
            }

            String materialId = getMaterialId(item);
            available.put(materialId, available.getOrDefault(materialId, 0) + item.getAmount());
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== GUI扫描完成 ===");
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("检查材料 - 物品ID: " + itemId);
            plugin.getLogger().info("需要的材料: " + required);
            plugin.getLogger().info("可用的材料: " + available);
        }

        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String requiredMaterial = normalizeMaterialId(entry.getKey());
            int requiredAmount = entry.getValue();

            int availableAmount = available.getOrDefault(requiredMaterial, 0);

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger()
                        .info("  材料: " + requiredMaterial + " - 需要: " + requiredAmount + ", 可用: " + availableAmount);
            }

            if (availableAmount < requiredAmount) {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().warning("  材料不足: " + requiredMaterial);
                }
                return false;
            }
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("材料检查通过！");
        }

        return true;
    }

    private boolean isDecorativeSlot(int slot) {

        if (slot >= 0 && slot < 9)
            return true;

        if (slot >= 45 && slot < 54)
            return true;

        if (slot % 9 == 0 || slot % 9 == 8)
            return true;

        return false;
    }

    private void craft(Player player, Inventory inv, String itemId) {

        if (!checkMaterials(inv, itemId)) {
            player.sendMessage("§c材料不足！");
            return;
        }

        boolean debugMode = plugin.getConfig().getBoolean("extreme_crafting_table.debug", false);
        if (!debugMode) {

            Map<String, Integer> required = recipes.get(itemId);
            if (required == null)
                return;

            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                String requiredMaterial = normalizeMaterialId(entry.getKey());
                int remaining = entry.getValue();

                for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
                    if (isDecorativeSlot(i))
                        continue;

                    ItemStack item = inv.getItem(i);
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    String itemMaterialId = getMaterialId(item);
                    if (itemMaterialId.equals(requiredMaterial)) {
                        int toRemove = Math.min(item.getAmount(), remaining);
                        item.setAmount(item.getAmount() - toRemove);
                        remaining -= toRemove;

                        if (item.getAmount() <= 0) {
                            inv.setItem(i, null);
                        }
                    }
                }
            }
        } else {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[终极工作台Debug] Debug模式，不消耗材料");
            }
        }

        ItemStack result = createItemFromId(itemId, player);
        if (result != null) {
            player.getInventory().addItem(result);
            String successMsg = languageManager.getMessage(player, "extreme_table_gui.craft_success")
                    .replace("{0}", getItemName(itemId, player));
            player.sendMessage(successMsg);
        }
    }

    private ItemStack createItemFromId(String itemId, Player player) {

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection section = config.getConfigurationSection("items." + itemId);
        if (section != null) {
            return createItemFromSection(section, player);
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().warning("找不到物品定义: " + itemId);
        }
        return null;
    }

    private ItemStack createItemFromSection(ConfigurationSection section, Player player) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().warning("无效的材质: " + materialName);
            }
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);

        String cmd = section.getString("custom_model_data");
        if (cmd != null) {
            item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {

            String displayName = section.getString("display_name");
            if (displayName != null) {
                if (displayName.startsWith("items.")) {
                    displayName = languageManager.getMessage(player, displayName);
                }
                meta.setDisplayName(displayName);
            }

            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> translatedLore = new ArrayList<>();
                for (String line : lore) {
                    if (line.startsWith("items.")) {
                        translatedLore.add(languageManager.getMessage(player, line));
                    } else {
                        translatedLore.add(line);
                    }
                }
                meta.setLore(translatedLore);
            }

            item.setItemMeta(meta);
        }

        String itemId = section.getName();

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("创建物品: " + itemId + ", 材质: " + material.name());
        }

        return PDCUtil.addPluginData(item, itemId);
    }

    private String getItemName(String itemId, Player player) {
        String key = "items." + itemId + ".name";
        String name = languageManager.getMessage(player, key);
        return name.equals(key) ? itemId : name;
    }

    private String getMaterialName(String materialId, Player player) {

        String originalId = materialId;

        if (materialId.contains(":")) {
            materialId = materialId.substring(materialId.indexOf(":") + 1);
        }

        Material mat = Material.getMaterial(materialId.toUpperCase());

        if (mat != null) {

            String materialKey = "materials." + materialId.toUpperCase();
            String materialName = languageManager.getMessage(player, materialKey);
            if (!materialName.equals(materialKey)) {
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', materialName);
            }

            return formatMaterialName(mat.name());
        }

        String itemKey = "items." + materialId + ".name";
        String itemName = languageManager.getMessage(player, itemKey);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("查找材料翻译 - materialId: " + materialId);
            plugin.getLogger().info("  翻译键: " + itemKey);
            plugin.getLogger().info("  翻译结果: " + itemName);
            plugin.getLogger().info("  是否找到: " + !itemName.equals(itemKey));
        }

        if (!itemName.equals(itemKey)) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', itemName);
        }

        return formatMaterialName(materialId);
    }

    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return result.toString();
    }

    private String getMaterialId(ItemStack item) {

        String itemId = PDCUtil.getItemId(item);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("获取物品材料ID:");
            plugin.getLogger().info("  物品类型: " + item.getType());
            plugin.getLogger()
                    .info("  物品显示名: " + (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : "无"));
            plugin.getLogger().info("  PDC中的ItemID: " + itemId);
        }

        if (itemId != null) {

            if (itemId.contains(":")) {
                String normalized = itemId.substring(itemId.indexOf(":") + 1);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("  规范化后: " + normalized);
                }
                return normalized;
            }
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("  直接返回: " + itemId);
            }
            return itemId;
        }

        String materialName = item.getType().name();
        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("  原版物品，返回: " + materialName);
        }
        return materialName;
    }

    private String normalizeMaterialId(String materialId) {
        if (materialId == null)
            return null;

        if (materialId.contains(":")) {
            return materialId.substring(materialId.indexOf(":") + 1);
        }

        return materialId;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== GUI点击事件 ===");
            plugin.getLogger().info("标题: " + title);
            plugin.getLogger().info("槽位: " + event.getRawSlot());
            plugin.getLogger().info("主GUI标题: " + MAIN_GUI_TITLE);
            plugin.getLogger().info("合成GUI前缀: " + CRAFT_GUI_PREFIX);
            plugin.getLogger().info("是否匹配主GUI: " + title.equals(MAIN_GUI_TITLE));
            plugin.getLogger().info("是否匹配合成GUI: " + title.startsWith(CRAFT_GUI_PREFIX));
        }

        if (title.equals(MAIN_GUI_TITLE)) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("触发主GUI事件处理");
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                String itemId = PDCUtil.getItemId(clicked);
                if (itemId != null && recipes.containsKey(itemId)) {
                    openCraftGUI(player, itemId);
                }
            }
            return;
        }

        if (title.startsWith(CRAFT_GUI_PREFIX)) {
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("触发合成GUI事件处理");
            }

            String foundItemId = null;
            for (Map.Entry<String, Map<String, Integer>> entry : recipes.entrySet()) {
                String recipeItemId = entry.getKey();
                String expectedTitle = CRAFT_GUI_PREFIX + getItemName(recipeItemId, player);
                if (title.equals(expectedTitle)) {
                    foundItemId = recipeItemId;
                    break;
                }
            }

            if (foundItemId == null) {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().warning("无法从GUI标题解析物品ID！");
                    plugin.getLogger().warning("GUI标题: " + title);
                }
                return;
            }

            final String itemId = foundItemId;

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("从标题解析到物品ID: " + itemId);
            }

            int slot = event.getRawSlot();

            if (slot < event.getInventory().getSize() && isDecorativeSlot(slot)) {
                event.setCancelled(true);

                if (slot == 49) {
                    ItemStack clicked = event.getCurrentItem();
                    if (clicked != null && clicked.getType() == Material.EMERALD_BLOCK) {

                        String cooldownKey = player.getUniqueId().toString() + ":" + itemId;
                        long now = System.currentTimeMillis();
                        Long lastCraft = craftCooldown.get(cooldownKey);

                        if (lastCraft != null && (now - lastCraft) < CRAFT_COOLDOWN_MS) {

                            if (plugin.getConfig().getBoolean("debug", true)) {
                                plugin.getLogger().info("[终极工作台] 合成冷却中，忽略点击");
                            }
                            return;
                        }

                        craftCooldown.put(cooldownKey, now);

                        event.getInventory().setItem(49, createBarrier(player));

                        craft(player, event.getInventory(), itemId);

                        org.little100.avaritia_Plugin.util.FoliaUtil.runSyncLater(plugin, () -> {
                            if (player.getOpenInventory().getTopInventory().equals(event.getInventory())) {
                                if (checkMaterials(event.getInventory(), itemId)) {
                                    event.getInventory().setItem(49, createEmeraldBlock(player));
                                } else {
                                    event.getInventory().setItem(49, createBarrier(player));
                                }
                            }
                        }, 3L);
                    }
                }
                return;
            }

            if (slot >= event.getInventory().getSize()) {

                if (event.isShiftClick()) {

                    org.little100.avaritia_Plugin.util.FoliaUtil.runSyncLater(plugin, () -> {
                        if (player.getOpenInventory().getTopInventory().equals(event.getInventory())) {
                            if (checkMaterials(event.getInventory(), itemId)) {
                                event.getInventory().setItem(49, createEmeraldBlock(player));
                            } else {
                                event.getInventory().setItem(49, createBarrier(player));
                            }
                        }
                    }, 3L);
                }
                return;
            }

            org.little100.avaritia_Plugin.util.FoliaUtil.runSyncLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().equals(event.getInventory())) {
                    if (checkMaterials(event.getInventory(), itemId)) {
                        event.getInventory().setItem(49, createEmeraldBlock(player));
                    } else {
                        event.getInventory().setItem(49, createBarrier(player));
                    }
                }
            }, 3L);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        String title = event.getView().getTitle();

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== GUI拖拽事件 ===");
            plugin.getLogger().info("标题: " + title);
            plugin.getLogger().info("拖拽槽位: " + event.getRawSlots());
        }

        if (title.equals(MAIN_GUI_TITLE)) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("主GUI - 取消拖拽事件");
            }
            return;
        }

        if (title.startsWith(CRAFT_GUI_PREFIX)) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getInventory().getSize() && isDecorativeSlot(slot)) {
                    event.setCancelled(true);
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("合成GUI - 尝试拖拽到装饰格子，已取消");
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("=== GUI关闭事件 ===");
                plugin.getLogger().info("玩家: " + player.getName());
                plugin.getLogger().info("标题: " + title);
            }

            playerCurrentRecipe.remove(player.getUniqueId());
        }
    }
}
