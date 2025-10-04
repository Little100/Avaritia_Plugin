package org.little100.avaritia_Plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final GUIConfigManager configManager;

    private final Map<UUID, GUISession> editingSessions = new HashMap<>();

    private static class GUISession {
        public final int size;
        public final String mode;
        public final String fileName;
        public final long createTime;

        public GUISession(int size, String mode) {
            this(size, mode, null);
        }

        public GUISession(int size, String mode, String fileName) {
            this.size = size;
            this.mode = mode;
            this.fileName = fileName;
            this.createTime = System.currentTimeMillis();
        }
    }

    public GUIManager(JavaPlugin plugin, LanguageManager languageManager, GUIConfigManager configManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.configManager = configManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openEditGUI(Player player, int size) {
        String title = languageManager.getMessage(player, "gui.edit_title", size);
        Inventory inventory = Bukkit.createInventory(null, size, title);

        addEditModeItems(inventory, size);

        editingSessions.put(player.getUniqueId(), new GUISession(size, "edit"));

        player.openInventory(inventory);
        languageManager.sendMessage(player, "gui.edit_opened", size);
    }

    public void openViewGUI(Player player, int size) {
        String title = languageManager.getMessage(player, "gui.view_title", size);
        Inventory inventory = Bukkit.createInventory(null, size, title);

        addViewModeItems(inventory, size);

        editingSessions.put(player.getUniqueId(), new GUISession(size, "view"));

        player.openInventory(inventory);
        languageManager.sendMessage(player, "gui.view_opened", size);
    }

    public void openNewGUI(Player player, int size) {
        String title = languageManager.getMessage(player, "gui.new_title", size);
        Inventory inventory = Bukkit.createInventory(null, size, title);

        editingSessions.put(player.getUniqueId(), new GUISession(size, "new"));

        player.openInventory(inventory);
        languageManager.sendMessage(player, "gui.new_opened", size);
    }

    public void openCreateGUI(Player player, String fileName, int size) {
        String title = "§6创建GUI: §f" + fileName + " §7(" + size + "槽)";
        Inventory inventory = Bukkit.createInventory(null, size, title);

        editingSessions.put(player.getUniqueId(), new GUISession(size, "create", fileName));

        player.openInventory(inventory);
        player.sendMessage("§a已打开创建模式GUI: §f" + fileName);
        player.sendMessage("§7关闭GUI后将自动保存到文件");
    }

    private void addEditModeItems(Inventory inventory, int size) {

        ItemStack cornerItem = new ItemStack(Material.BARRIER);
        ItemMeta cornerMeta = cornerItem.getItemMeta();
        if (cornerMeta != null) {
            cornerMeta.setDisplayName("§c编辑模式标记");
            cornerMeta.setLore(Arrays.asList(
                    "§7这是编辑模式的GUI",
                    "§7大小: §f" + size + " §7槽位",
                    "§7你可以在这里放置物品进行编辑"));
            cornerItem.setItemMeta(cornerMeta);
        }

        if (size == 1) {

            inventory.setItem(0, cornerItem);
        } else if (size <= 9) {

            inventory.setItem(0, cornerItem);
            if (size > 1) {
                inventory.setItem(size - 1, cornerItem);
            }
        } else {

            inventory.setItem(0, cornerItem);
            if (size >= 9) {
                inventory.setItem(8, cornerItem);
            }
            if (size >= 45) {
                inventory.setItem(size - 9, cornerItem);
                inventory.setItem(size - 1, cornerItem);
            }
        }

        if (size >= 3) {
            addToolItem(inventory, Math.min(1, size - 1), Material.DIAMOND_SWORD, "§b示例武器", "§7这是一个示例物品");
        }
        if (size >= 5) {
            addToolItem(inventory, Math.min(2, size - 1), Material.EMERALD, "§a示例材料", "§7这是一个材料物品");
        }
        if (size >= 18) {
            addToolItem(inventory, 9, Material.BOOK, "§e示例书籍", "§7这是一个知识物品");
        }
    }

    private void addViewModeItems(Inventory inventory, int size) {

        Material material = Material.GLASS_PANE;
        try {

            material = Material.valueOf("LIGHT_BLUE_STAINED_GLASS_PANE");
        } catch (IllegalArgumentException e) {

            material = Material.GLASS_PANE;
        }

        if (size == 1) {

            ItemStack specialItem = new ItemStack(Material.COMPASS);
            ItemMeta specialMeta = specialItem.getItemMeta();
            if (specialMeta != null) {
                specialMeta.setDisplayName("§b单格查看模式");
                specialMeta.setLore(Arrays.asList(
                        "§7这是一个1槽位的GUI",
                        "§7非常紧凑的界面设计"));
                specialItem.setItemMeta(specialMeta);
            }
            inventory.setItem(0, specialItem);
        } else if (size <= 5) {

            Material[] smallGuiMaterials = {
                    Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
                    Material.GREEN_WOOL, Material.BLUE_WOOL
            };

            for (int i = 0; i < size; i++) {
                Material itemMaterial = smallGuiMaterials[i % smallGuiMaterials.length];
                try {
                    ItemStack colorItem = new ItemStack(itemMaterial);
                    ItemMeta colorMeta = colorItem.getItemMeta();
                    if (colorMeta != null) {
                        colorMeta.setDisplayName("§b小型GUI - 位置 " + (i + 1));
                        colorMeta.setLore(Arrays.asList("§7GUI大小: " + size + " 槽位"));
                        colorItem.setItemMeta(colorMeta);
                    }
                    inventory.setItem(i, colorItem);
                } catch (Exception e) {

                    ItemStack fallbackItem = new ItemStack(material);
                    ItemMeta fallbackMeta = fallbackItem.getItemMeta();
                    if (fallbackMeta != null) {
                        fallbackMeta.setDisplayName("§b查看模式 - 位置 " + (i + 1));
                        fallbackMeta.setLore(Arrays.asList("§7GUI大小: " + size + " 槽位"));
                        fallbackItem.setItemMeta(fallbackMeta);
                    }
                    inventory.setItem(i, fallbackItem);
                }
            }
        } else {

            int itemsToPlace = Math.min(size, Math.max(9, size / 6));
            for (int i = 0; i < itemsToPlace && i < size; i++) {
                ItemStack glassPane = new ItemStack(material);
                ItemMeta glassMeta = glassPane.getItemMeta();
                if (glassMeta != null) {
                    glassMeta.setDisplayName("§b查看模式");
                    glassMeta.setLore(Arrays.asList("§7GUI大小: " + size + " 槽位"));
                    glassPane.setItemMeta(glassMeta);
                }
                inventory.setItem(i, glassPane);
            }
        }
    }

    private void addToolItem(Inventory inventory, int slot, Material material, String name, String lore) {
        if (slot >= inventory.getSize())
            return;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        if (!editingSessions.containsKey(playerId))
            return;

        GUISession session = editingSessions.get(playerId);

        if ("view".equals(session.mode)) {
            event.setCancelled(true);
            languageManager.sendMessage(player, "gui.view_mode_readonly");
            return;
        }

        if ("edit".equals(session.mode) || "new".equals(session.mode)) {

            languageManager.sendMessage(player, "gui.item_modified");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!editingSessions.containsKey(playerId))
            return;

        GUISession session = editingSessions.remove(playerId);

        switch (session.mode) {
            case "edit":
                languageManager.sendMessage(player, "gui.edit_closed");
                break;
            case "view":
                languageManager.sendMessage(player, "gui.view_closed");
                break;
            case "new":
                languageManager.sendMessage(player, "gui.new_closed");
                break;
            case "create":

                if (session.fileName != null) {
                    boolean success = configManager.saveGui(session.fileName, event.getInventory(), player);
                    if (success) {
                        player.sendMessage("§a已自动保存GUI到文件: §f" + session.fileName + ".yml");
                        player.sendMessage("§7文件位置: §fplugins/Avaritia_Plugin/guis/" + session.fileName + ".yml");
                    } else {
                        player.sendMessage("§c保存GUI失败！请查看控制台错误信息。");
                    }
                }
                break;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            long duration = System.currentTimeMillis() - session.createTime;
            plugin.getLogger().info("玩家 " + player.getName() + " 关闭了 " + session.mode +
                    " 模式GUI，持续时间: " + (duration / 1000) + "秒");
        }
    }

    public int getActiveSessionsCount() {
        return editingSessions.size();
    }

    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 60 * 60 * 1000;

        editingSessions.entrySet().removeIf(entry -> currentTime - entry.getValue().createTime > expireTime);
    }
}
