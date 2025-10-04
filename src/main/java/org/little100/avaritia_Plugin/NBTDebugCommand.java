package org.little100.avaritia_Plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.little100.avaritia_Plugin.manager.LanguageManager;
import org.little100.avaritia_Plugin.util.CustomModelDataUtil;

public class NBTDebugCommand implements CommandExecutor {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;

    public NBTDebugCommand(Avaritia_Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            languageManager.sendMessage(player, "nbt_debug.no_item");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {

            String cmdString = CustomModelDataUtil.getCustomModelDataString(item);
            int cmdInt = CustomModelDataUtil.getCustomModelDataInt(item);

            if (cmdString != null) {
                languageManager.sendMessage(player, "nbt_debug.custom_model_data_string", cmdString);
            } else {
                languageManager.sendMessage(player, "nbt_debug.no_string_data");
            }

            if (cmdInt > 0) {
                languageManager.sendMessage(player, "nbt_debug.custom_model_data_int", cmdInt);
            } else {
                languageManager.sendMessage(player, "nbt_debug.no_int_data");
            }

            String apiType = CustomModelDataUtil.isNewApiSupported()
                    ? languageManager.getMessage(player, "nbt_debug.api_new")
                    : languageManager.getMessage(player, "nbt_debug.api_old");
            languageManager.sendMessage(player, "nbt_debug.api_version", apiType);

            if (meta.hasLore()) {
                languageManager.sendMessage(player, "nbt_debug.lore_header");
                for (String line : meta.getLore()) {
                    languageManager.sendMessage(player, "nbt_debug.lore_item", line);
                }
            } else {
                languageManager.sendMessage(player, "nbt_debug.no_lore");
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.isEmpty()) {
                languageManager.sendMessage(player, "nbt_debug.pdc_header");
                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    try {

                        String value = "未知类型";

                        if (pdc.has(key, PersistentDataType.STRING)) {
                            value = pdc.get(key, PersistentDataType.STRING);
                        } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.INTEGER));
                        } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.DOUBLE));
                        } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.BOOLEAN));
                        } else if (pdc.has(key, PersistentDataType.LONG)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.LONG));
                        } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.FLOAT));
                        } else if (pdc.has(key, PersistentDataType.BYTE)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.BYTE));
                        } else if (pdc.has(key, PersistentDataType.SHORT)) {
                            value = String.valueOf(pdc.get(key, PersistentDataType.SHORT));
                        }

                        languageManager.sendMessage(player, "nbt_debug.pdc_item", key.toString(), value);
                    } catch (Exception e) {
                        languageManager.sendMessage(player, "nbt_debug.pdc_item", key.toString(),
                                "读取失败: " + e.getMessage());
                    }
                }
            } else {
                languageManager.sendMessage(player, "nbt_debug.no_pdc");
            }
        } else {
            languageManager.sendMessage(player, "nbt_debug.no_metadata");
        }

        return true;
    }
}