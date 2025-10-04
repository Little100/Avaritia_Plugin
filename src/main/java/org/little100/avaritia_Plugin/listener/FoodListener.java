package org.little100.avaritia_Plugin.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.PDCUtil;

public class FoodListener implements Listener {

    private final Avaritia_Plugin plugin;

    public FoodListener(Avaritia_Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!PDCUtil.isPluginItem(item)) {
            return;
        }

        String itemId = PDCUtil.getItemId(item);
        if (itemId == null) {
            return;
        }

        switch (itemId) {
            case "cosmic_meatballs":
                handleCosmicMeatballs(player);
                break;
            case "ultimate_stew":
                handleUltimateStew(player);
                break;
            default:

                break;
        }
    }

    private void handleCosmicMeatballs(Player player) {

        player.setFoodLevel(20);

        player.setSaturation(20.0f);

        PotionEffect strength = new PotionEffect(
                PotionEffectType.STRENGTH,
                6000,
                1,
                false,
                true,
                true);
        player.addPotionEffect(strength);

        player.sendMessage("§5§l✦ §d你感受到了宇宙的力量！");

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 食用了寰宇肉丸");
        }
    }

    private void handleUltimateStew(Player player) {

        player.setFoodLevel(20);

        player.setSaturation(20.0f);

        player.setExhaustion(0.0f);

        PotionEffect regeneration = new PotionEffect(
                PotionEffectType.REGENERATION,
                6000,
                1,
                false,
                true,
                true);
        player.addPotionEffect(regeneration);

        player.sendMessage("§6§l✦ §e你感受到了生命的复苏！");

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 食用了超级煲");
        }
    }
}
