package org.little100.avaritia_Plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.FoliaUtil;

import java.io.File;
import java.util.*;

public class NeutronCollectorGUI implements Listener {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;

    private int ticksPerItem;
    private String outputItemId;
    private Material progressMaterial;
    private int updateInterval;

    private final Map<Location, CollectorData> collectors = new HashMap<>();

    private final Map<UUID, Location> viewingCollectors = new HashMap<>();

    /**
     * 收集器数据类
     */
    public static class CollectorData {
        public long currentTicks;
        public long startTime;
        public io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

        public CollectorData() {
            this.currentTicks = 0;
            this.startTime = System.currentTimeMillis();
            this.task = null;
        }

        public int getProgress(int maxTicks) {
            return (int) ((currentTicks * 100) / maxTicks);
        }

        public int getRemainingSeconds(int maxTicks) {
            long remaining = maxTicks - currentTicks;
            return (int) (remaining / 20);
        }
    }

    public NeutronCollectorGUI(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        loadConfig();

    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "neutron_collector.yml");

        if (!configFile.exists()) {
            plugin.saveResource("neutron_collector.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ticksPerItem = config.getInt("production.ticks_per_item", 7111);
        outputItemId = config.getString("production.output_item", "neutron_dust");

        String materialName = config.getString("gui.progress_item", "LIGHT_BLUE_STAINED_GLASS_PANE");
        progressMaterial = Material.getMaterial(materialName);
        if (progressMaterial == null) {
            progressMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        }

        updateInterval = config.getInt("gui.update_interval", 20);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("中子态素收集器配置加载完成:");
            plugin.getLogger().info("  生产周期: " + ticksPerItem + " ticks");
            plugin.getLogger().info("  产出物品: " + outputItemId);
        }
    }

    /**
     * 注册收集器
     */
    public void registerCollector(Location location) {
        if (!collectors.containsKey(location)) {
            CollectorData data = new CollectorData();
            collectors.put(location, data);

            startProductionTask(location, data);

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("注册中子态素收集器: " + locationToString(location));
            }
        }
    }

    /**
     * 注销收集器
     */
    public void unregisterCollector(Location location) {
        CollectorData data = collectors.remove(location);

        if (data != null && data.task != null) {
            data.task.cancel();
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("注销中子态素收集器: " + locationToString(location));
        }
    }

    /**
     * 打开收集器GUI
     */
    public void openCollectorGUI(Player player, Location location) {
        CollectorData data = collectors.get(location);
        if (data == null) {

            data = new CollectorData();
            collectors.put(location, data);
        }

        String title = languageManager.getMessage(player, "neutron_collector_gui.title");
        Inventory gui = Bukkit.createInventory(null, 9, title);

        updateGUI(gui, data, player);

        viewingCollectors.put(player.getUniqueId(), location);

        player.openInventory(gui);
    }

    /**
     * 更新GUI内容
     */
    private void updateGUI(Inventory gui, CollectorData data, Player player) {
        int progress = data.getProgress(ticksPerItem);
        int remainingSeconds = data.getRemainingSeconds(ticksPerItem);

        ItemStack progressItem = new ItemStack(progressMaterial);
        ItemMeta meta = progressItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(player, "neutron_collector_gui.progress")
                    .replace("{0}", String.valueOf(progress)));

            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage(player, "neutron_collector_gui.time_remaining")
                    .replace("{0}", String.valueOf(remainingSeconds)));
            lore.add(languageManager.getMessage(player, "neutron_collector_gui.producing"));
            meta.setLore(lore);

            progressItem.setItemMeta(meta);
        }

        int[] slots = { 0, 1, 2, 3, 5, 6, 7, 8 };
        for (int slot : slots) {
            gui.setItem(slot, progressItem);
        }

        if (data.currentTicks >= ticksPerItem) {

            ItemStack output = createOutputItem(player);
            if (output != null) {
                gui.setItem(4, output);
            }
        }
    }

    /**
     * 创建产出物品
     */
    private ItemStack createOutputItem(Player player) {

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("items." + outputItemId);

        if (section == null) {
            plugin.getLogger().warning("找不到物品定义: " + outputItemId);
            return null;
        }

        String materialName = section.getString("material", "IRON_NUGGET");
        Material material = Material.getMaterial(materialName);
        if (material == null)
            material = Material.IRON_NUGGET;

        ItemStack item = new ItemStack(material);

        String cmd = section.getString("custom_model_data");
        if (cmd != null) {
            item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item, cmd);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {

            String displayName = section.getString("display_name");
            if (displayName != null && displayName.startsWith("items.")) {
                displayName = languageManager.getMessage(player, displayName);
            }
            meta.setDisplayName(displayName);

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

        return org.little100.avaritia_Plugin.util.PDCUtil.addPluginData(item, outputItemId);
    }

    /**
     * 为单个收集器启动生产任务
     */
    private void startProductionTask(Location location, CollectorData data) {

        data.task = FoliaUtil.runAtLocation(plugin, location, () -> {

            if (data.currentTicks < ticksPerItem) {
                data.currentTicks++;
            }

            for (Map.Entry<UUID, Location> entry : new HashMap<>(viewingCollectors).entrySet()) {
                if (entry.getValue().equals(location)) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        Inventory openInv = player.getOpenInventory().getTopInventory();
                        if (openInv != null && openInv.getSize() == 9) {
                            updateGUI(openInv, data, player);
                        }
                    }
                }
            }
        }, 1L, 1L);
    }

    /**
     * GUI点击事件
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        String expectedTitle = languageManager.getMessage(player, "neutron_collector_gui.title");

        if (!title.equals(expectedTitle))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == 4) {
            Location loc = viewingCollectors.get(player.getUniqueId());
            if (loc != null) {
                CollectorData data = collectors.get(loc);
                if (data != null && data.currentTicks >= ticksPerItem) {

                    ItemStack output = createOutputItem(player);
                    if (output != null) {
                        player.getInventory().addItem(output);

                        data.currentTicks = 0;
                        data.startTime = System.currentTimeMillis();

                        updateGUI(event.getInventory(), data, player);
                    }
                }
            }
        }
    }

    /**
     * GUI关闭事件
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            viewingCollectors.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * 位置转字符串
     */
    private String locationToString(Location loc) {
        return String.format("%s[%d,%d,%d]",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
    }

    /**
     * 获取收集器数据（用于保存）
     */
    public Map<Location, CollectorData> getCollectors() {
        return collectors;
    }
}
