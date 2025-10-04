package org.little100.avaritia_Plugin.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.little100.avaritia_Plugin.Avaritia_Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIConfigManager {

    private final Avaritia_Plugin plugin;
    private final File guiFolder;

    private final Map<UUID, String> editingGuis = new HashMap<>();

    public GUIConfigManager(Avaritia_Plugin plugin) {
        this.plugin = plugin;
        this.guiFolder = new File(plugin.getDataFolder(), "guis");

        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }
    }

    public void setEditingGui(Player player, String fileName) {
        editingGuis.put(player.getUniqueId(), fileName);
    }

    public String getEditingGui(Player player) {
        return editingGuis.get(player.getUniqueId());
    }

    public void removeEditingGui(Player player) {
        editingGuis.remove(player.getUniqueId());
    }

    public boolean saveGui(String fileName, Inventory inventory, Player player) {
        try {
            File guiFile = new File(guiFolder, fileName + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("title", inventory.getType().name());
            config.set("size", inventory.getSize());
            config.set("creator", player.getName());
            config.set("created_at", System.currentTimeMillis());

            ConfigurationSection itemsSection = config.createSection("items");
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    itemsSection.set(String.valueOf(i), item);
                }
            }

            config.save(guiFile);

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info(
                        "已保存GUI: " + fileName + " (大小: " + inventory.getSize() + ", 创建者: " + player.getName() + ")");
            }

            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("保存GUI失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Inventory loadGui(String fileName, org.bukkit.Server server) {
        try {
            File guiFile = new File(guiFolder, fileName + ".yml");
            if (!guiFile.exists()) {
                return null;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(guiFile);

            int size = config.getInt("size", 27);
            String title = config.getString("title", fileName);

            Inventory inventory = server.createInventory(null, size, title);

            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    ItemStack item = itemsSection.getItemStack(key);
                    if (item != null && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("已加载GUI: " + fileName + " (大小: " + size + ")");
            }

            return inventory;
        } catch (Exception e) {
            plugin.getLogger().severe("加载GUI失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean guiExists(String fileName) {
        File guiFile = new File(guiFolder, fileName + ".yml");
        return guiFile.exists();
    }

    public String[] getAllGuiNames() {
        File[] files = guiFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return new String[0];
        }

        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName().replace(".yml", "");
        }
        return names;
    }

    public boolean deleteGui(String fileName) {
        File guiFile = new File(guiFolder, fileName + ".yml");
        if (guiFile.exists()) {
            return guiFile.delete();
        }
        return false;
    }
}
