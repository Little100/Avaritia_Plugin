package org.little100.avaritia_Plugin.listener;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.PDCUtil;
import org.little100.avaritia_Plugin.util.FoliaUtil;

import java.awt.Color;
import java.util.*;

public class InfinityArmorListener implements Listener {

    private final Avaritia_Plugin plugin;
    private final Set<UUID> flyingPlayers = new HashSet<>();
    private final Map<UUID, Boolean> fullSetNotified = new HashMap<>();

    public InfinityArmorListener(Avaritia_Plugin plugin) {
        this.plugin = plugin;
        startArmorCheckTask();
    }

    private void startArmorCheckTask() {

        FoliaUtil.runGlobalTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkInfinityArmorEffects(player);
            }
        }, 20L, 20L);
    }

    private void checkInfinityArmorEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = "infinity_helmet".equals(PDCUtil.getItemId(helmet));
        boolean hasChestplate = "infinity_chestplate".equals(PDCUtil.getItemId(chestplate));
        boolean hasLeggings = "infinity_leggings".equals(PDCUtil.getItemId(leggings));
        boolean hasBoots = "infinity_boots".equals(PDCUtil.getItemId(boots));

        boolean hasFullSet = hasHelmet && hasChestplate && hasLeggings && hasBoots;
        UUID uuid = player.getUniqueId();

        FoliaUtil.runEntityTask(plugin, player, () -> {

            if (hasHelmet) {

                player.setRemainingAir(player.getMaximumAir());

                player.setFoodLevel(20);
                player.setSaturation(20.0F);

                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, false, false));
            }

            if (hasChestplate) {

                if (!flyingPlayers.contains(uuid)) {
                    flyingPlayers.add(uuid);
                    player.setAllowFlight(true);
                }

                removeDebuffs(player);
            } else {

                if (flyingPlayers.contains(uuid)) {
                    flyingPlayers.remove(uuid);
                    if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                }
            }

            if (hasLeggings) {

                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            }

            if (hasBoots) {

                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 9, false, false));
            }

            if (hasFullSet) {

                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 60, 9, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, true, false));

                if (!fullSetNotified.getOrDefault(uuid, false)) {
                    fullSetNotified.put(uuid, true);

                    player.sendMessage(org.bukkit.ChatColor.AQUA + "» " + org.bukkit.ChatColor.LIGHT_PURPLE +
                            "无尽盔甲已激活！");
                    player.sendMessage(org.bukkit.ChatColor.GRAY + "» 你获得了" + org.bukkit.ChatColor.GOLD + "完全防护");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 2.0F);
                }
            } else {
                fullSetNotified.put(uuid, false);
            }
        });
    }

    private String createRainbowText(String text) {
        StringBuilder result = new StringBuilder();
        Color[] rainbowColors = {
                new Color(255, 0, 0),
                new Color(255, 127, 0),
                new Color(255, 255, 0),
                new Color(0, 255, 0),
                new Color(0, 0, 255),
                new Color(75, 0, 130),
                new Color(148, 0, 211)
        };

        for (int i = 0; i < text.length(); i++) {
            Color color = rainbowColors[i % rainbowColors.length];
            result.append(ChatColor.of(color)).append(text.charAt(i));
        }

        return result.toString();
    }

    private void removeDebuffs(Player player) {
        PotionEffectType[] debuffs = {
                PotionEffectType.SLOWNESS,
                PotionEffectType.MINING_FATIGUE,
                PotionEffectType.WEAKNESS,
                PotionEffectType.HUNGER,
                PotionEffectType.WITHER,
                PotionEffectType.POISON,
                PotionEffectType.LEVITATION,
                PotionEffectType.BLINDNESS,
                PotionEffectType.NAUSEA,
                PotionEffectType.INSTANT_DAMAGE
        };

        for (PotionEffectType debuff : debuffs) {
            if (player.hasPotionEffect(debuff)) {
                player.removePotionEffect(debuff);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = "infinity_helmet".equals(PDCUtil.getItemId(helmet));
        boolean hasChestplate = "infinity_chestplate".equals(PDCUtil.getItemId(chestplate));
        boolean hasLeggings = "infinity_leggings".equals(PDCUtil.getItemId(leggings));
        boolean hasBoots = "infinity_boots".equals(PDCUtil.getItemId(boots));

        boolean hasFullSet = hasHelmet && hasChestplate && hasLeggings && hasBoots;

        if (hasFullSet) {
            event.setCancelled(true);
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (isMagicDamage(cause)) {
            int armorCount = (hasHelmet ? 1 : 0) + (hasChestplate ? 1 : 0) +
                    (hasLeggings ? 1 : 0) + (hasBoots ? 1 : 0);

            if (armorCount > 0) {
                double reduction = 0.20 * armorCount;
                double damage = event.getDamage();
                event.setDamage(damage * (1 - reduction));
            }
        }
    }

    private boolean isMagicDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.MAGIC ||
                cause == EntityDamageEvent.DamageCause.WITHER ||
                cause == EntityDamageEvent.DamageCause.POISON ||
                cause == EntityDamageEvent.DamageCause.DRAGON_BREATH;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        String weaponId = PDCUtil.getItemId(weapon);

        if ("sword_of_the_cosmos".equals(weaponId)) {

            boolean victimHasFullSet = false;
            if (victim instanceof Player) {
                Player victimPlayer = (Player) victim;
                victimHasFullSet = "infinity_helmet".equals(PDCUtil.getItemId(victimPlayer.getInventory().getHelmet()))
                        &&
                        "infinity_chestplate".equals(PDCUtil.getItemId(victimPlayer.getInventory().getChestplate())) &&
                        "infinity_leggings".equals(PDCUtil.getItemId(victimPlayer.getInventory().getLeggings())) &&
                        "infinity_boots".equals(PDCUtil.getItemId(victimPlayer.getInventory().getBoots()));
            }

            if (victimHasFullSet) {

                event.setCancelled(true);
                attacker.sendMessage(org.bukkit.ChatColor.RED + "» 对方穿戴无尽盔甲，免疫你的攻击！");
                ((Player) victim).sendMessage(org.bukkit.ChatColor.GREEN + "» 无尽盔甲保护了你！");

                victim.getWorld().spawnParticle(Particle.ENCHANT, victim.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5,
                        0);
                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 2.0F);
            } else {

                double killDamage = victim.getMaxHealth() + 1000.0;
                event.setDamage(killDamage);

                victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 10, 0.5, 0.5,
                        0.5, 0);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5,
                        0.2);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.5F);

                if (victim instanceof Player) {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("[末日之刃] 玩家 " + attacker.getName() + " 一击击杀玩家 "
                                + ((Player) victim).getName() + " (伤害: " + killDamage + ")");
                    }
                } else {
                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger()
                                .info("[末日之刃] 玩家 " + attacker.getName() + " 一击击杀 " + victim.getType() + " (伤害: "
                                        + killDamage + ", 生命: " + victim.getHealth() + "/" + victim.getMaxHealth()
                                        + ")");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        ItemStack offHand = killer.getInventory().getItemInOffHand();
        String mainHandId = PDCUtil.getItemId(mainHand);
        String offHandId = PDCUtil.getItemId(offHand);

        boolean shouldDropHead = false;

        if ("skullfire_sword".equals(mainHandId)) {
            shouldDropHead = true;
        }

        else if ("skullfire_sword".equals(offHandId)) {
            boolean transferEnabled = plugin.getConfig().getBoolean("infinity_tools.skullfire_offhand_transfer", true);
            if (transferEnabled && mainHand != null && mainHand.getType() != Material.AIR) {
                shouldDropHead = true;
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[骷髅剑] 副手骷髅剑效果传递给主手 " + mainHand.getType());
                }
            }
        }

        if (!shouldDropHead)
            return;

        ItemStack skull = getMobHead(event.getEntityType());
        if (skull != null) {
            event.getDrops().add(skull);

            Location loc = event.getEntity().getLocation();
            loc.getWorld().spawnParticle(Particle.SOUL, loc, 20, 0.5, 0.5, 0.5, 0.05);
            killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3F, 2.0F);
        }
    }

    private ItemStack getMobHead(EntityType type) {
        Material headMaterial;

        switch (type) {
            case SKELETON:
                headMaterial = Material.SKELETON_SKULL;
                break;
            case WITHER_SKELETON:
                headMaterial = Material.WITHER_SKELETON_SKULL;
                break;
            case ZOMBIE:
                headMaterial = Material.ZOMBIE_HEAD;
                break;
            case CREEPER:
                headMaterial = Material.CREEPER_HEAD;
                break;
            case ENDER_DRAGON:
                headMaterial = Material.DRAGON_HEAD;
                break;
            case PLAYER:
                return new ItemStack(Material.PLAYER_HEAD);
            default:

                ItemStack customHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) customHead.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(org.bukkit.ChatColor.YELLOW + type.name() + " Head");
                    customHead.setItemMeta(meta);
                }
                return customHead;
        }

        return new ItemStack(headMaterial);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();
        if (bow == null)
            return;

        String bowId = PDCUtil.getItemId(bow);
        if (!"infinity_bow".equals(bowId))
            return;

        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.setDamage(arrow.getDamage() * 3);
            arrow.setPierceLevel(5);
            arrow.setCritical(true);

            arrow.setCustomName("infinity_arrow");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        flyingPlayers.remove(uuid);
        fullSetNotified.remove(uuid);
    }
}
