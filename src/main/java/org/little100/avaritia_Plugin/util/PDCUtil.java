package org.little100.avaritia_Plugin.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class PDCUtil {

    private static NamespacedKey PLUGIN_KEY;
    private static NamespacedKey ITEM_ID_KEY;
    private static NamespacedKey ITEM_VERSION_KEY;
    private static NamespacedKey CUSTOM_ITEM_KEY;

    private static final String CURRENT_ITEM_VERSION = "1.0.0";

    public static void initialize(JavaPlugin plugin) {
        PLUGIN_KEY = new NamespacedKey(plugin, "avaritia_plugin");
        ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
        ITEM_VERSION_KEY = new NamespacedKey(plugin, "item_version");
        CUSTOM_ITEM_KEY = new NamespacedKey(plugin, "custom_item");
    }

    public static ItemStack addPluginData(ItemStack itemStack, String itemId) {
        if (itemStack == null)
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return itemStack;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(PLUGIN_KEY, PersistentDataType.STRING, "avaritia");

        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        pdc.set(ITEM_VERSION_KEY, PersistentDataType.STRING, CURRENT_ITEM_VERSION);

        pdc.set(CUSTOM_ITEM_KEY, PersistentDataType.BOOLEAN, true);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static boolean isPluginItem(ItemStack itemStack) {
        if (itemStack == null)
            return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(PLUGIN_KEY, PersistentDataType.STRING) &&
                "avaritia".equals(pdc.get(PLUGIN_KEY, PersistentDataType.STRING));
    }

    public static String getItemId(ItemStack itemStack) {
        if (!isPluginItem(itemStack))
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public static String getItemVersion(ItemStack itemStack) {
        if (!isPluginItem(itemStack))
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(ITEM_VERSION_KEY, PersistentDataType.STRING);
    }

    public static boolean isCustomItem(ItemStack itemStack) {
        if (!isPluginItem(itemStack))
            return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(CUSTOM_ITEM_KEY, PersistentDataType.BOOLEAN) &&
                Boolean.TRUE.equals(pdc.get(CUSTOM_ITEM_KEY, PersistentDataType.BOOLEAN));
    }

    public static ItemStack addCustomData(ItemStack itemStack, String key, String value) {
        if (itemStack == null)
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return itemStack;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey customKey = new NamespacedKey(PLUGIN_KEY.getNamespace(), key);
        pdc.set(customKey, PersistentDataType.STRING, value);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static String getCustomData(ItemStack itemStack, String key) {
        if (itemStack == null)
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey customKey = new NamespacedKey(PLUGIN_KEY.getNamespace(), key);
        return pdc.get(customKey, PersistentDataType.STRING);
    }

    public static NamespacedKey[] getPluginKeys() {
        return new NamespacedKey[] {
                PLUGIN_KEY,
                ITEM_ID_KEY,
                ITEM_VERSION_KEY,
                CUSTOM_ITEM_KEY
        };
    }

    public static ItemStack clearPluginData(ItemStack itemStack) {
        if (itemStack == null)
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return itemStack;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : getPluginKeys()) {
            pdc.remove(key);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
