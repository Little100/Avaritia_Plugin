package org.little100.avaritia_Plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.manager.CustomCraftingManager;
import org.little100.avaritia_Plugin.util.PDCUtil;

public class CraftingListener implements Listener {

    private final Avaritia_Plugin plugin;
    private final CustomCraftingManager craftingManager;

    public CraftingListener(Avaritia_Plugin plugin, CustomCraftingManager craftingManager) {
        this.plugin = plugin;
        this.craftingManager = craftingManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("=== 合成事件检查开始 ===");
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    String itemDesc = matrix[i].getType().name();
                    if (PDCUtil.isPluginItem(matrix[i])) {
                        itemDesc += " (插件物品: " + PDCUtil.getItemId(matrix[i]) + ")";
                    }
                    plugin.getLogger().info("位置 " + i + ": " + itemDesc);
                }
            }
        }

        boolean hasPluginMaterial = false;
        for (ItemStack item : matrix) {
            if (item != null && PDCUtil.isPluginItem(item)) {
                hasPluginMaterial = true;
                break;
            }
        }

        if (hasPluginMaterial) {

            String language = "zh_cn";
            if (event.getView() != null && event.getView().getPlayer() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getView().getPlayer();
                language = plugin.getLanguageManager().getPlayerLanguage(player);
            }

            ItemStack customResult = craftingManager.findMatchingRecipe(matrix, language);
            if (customResult != null) {

                event.getInventory().setResult(customResult);

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger()
                            .info("✓ 检测到有效自定义合成: " + PDCUtil.getItemId(customResult) + " (语言: " + language + ")");
                }
            } else {

                event.getInventory().setResult(null);

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("✗ 阻止了无效的插件物品合成（材料验证失败）");
                }
            }
        } else {

            ItemStack bukkitResult = event.getInventory().getResult();
            if (bukkitResult != null && PDCUtil.isPluginItem(bukkitResult)) {

                String language = "zh_cn";
                if (event.getView() != null && event.getView().getPlayer() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getView().getPlayer();
                    language = plugin.getLanguageManager().getPlayerLanguage(player);
                }

                ItemStack translatedResult = craftingManager.createLocalizedItem(bukkitResult, language);
                if (translatedResult != null) {
                    event.getInventory().setResult(translatedResult);
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("✓ 纯原版材料合成插件物品（已本地化）: " + PDCUtil.getItemId(translatedResult) + " (语言: "
                                + language + ")");
                    }
                } else {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("✓ 纯原版材料合成插件物品: " + PDCUtil.getItemId(bukkitResult));
                    }
                }
            } else {
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("○ 纯原版物品合成");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        ItemStack actualResult = event.getInventory().getResult();

        if (plugin.getConfig().getBoolean("debug", true)) {
            String resultDesc = actualResult != null ? actualResult.getType().name() : "null";
            if (actualResult != null && PDCUtil.isPluginItem(actualResult)) {
                resultDesc += " (插件物品: " + PDCUtil.getItemId(actualResult) + ")";
            }
            plugin.getLogger().info("=== 合成物品事件: " + resultDesc + " ===");
        }

        boolean hasPluginMaterial = false;
        for (ItemStack item : matrix) {
            if (item != null && PDCUtil.isPluginItem(item)) {
                hasPluginMaterial = true;
                break;
            }
        }

        if (hasPluginMaterial) {

            if (actualResult != null && PDCUtil.isPluginItem(actualResult)) {
                craftingManager.consumeIngredients(matrix, actualResult);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("✓ 完成自定义合成: " + PDCUtil.getItemId(actualResult));
                }
            } else {

                event.setCancelled(true);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("✗ 取消了无效的插件物品合成");
                }
            }
        } else {

            if (actualResult != null && PDCUtil.isPluginItem(actualResult)) {

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("✓ 完成原版材料的插件物品合成: " + PDCUtil.getItemId(actualResult));
                }
            } else {

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("○ 纯原版物品合成");
                }
            }
        }
    }
}
