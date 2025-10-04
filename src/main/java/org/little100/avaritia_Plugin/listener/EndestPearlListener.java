package org.little100.avaritia_Plugin.listener;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.FoliaUtil;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class EndestPearlListener implements Listener {
    
    private final Avaritia_Plugin plugin;
    private final Random random = new Random();
    
    public EndestPearlListener(Avaritia_Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听玩家右键投掷终望珍珠
     */
    @EventHandler
    public void onPlayerUseEndestPearl(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // 检查是否是终望珍珠
        String itemId = PDCUtil.getItemId(item);
        if (!"endest_pearl".equals(itemId)) return;
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // 投掷终望珍珠
        Snowball pearl = player.launchProjectile(Snowball.class);
        pearl.setVelocity(player.getLocation().getDirection().multiply(1.5));
        
        ItemStack displayItem = item.clone();
        displayItem.setAmount(1);
        pearl.setItem(displayItem);
        
        ItemStack pearlItem = pearl.getItem();
        PDCUtil.addPluginData(pearlItem, "endest_pearl");
        pearl.setItem(pearlItem);
        
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        
        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 投掷了终望珍珠");
        }
    }
    
    /**
     * 监听终望珍珠落地
     */
    @EventHandler
    public void onEndestPearlHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        
        Snowball pearl = (Snowball) event.getEntity();
        
        ItemStack pearlItem = pearl.getItem();
        String itemId = PDCUtil.getItemId(pearlItem);
        
        if (!"endest_pearl".equals(itemId)) return;
        
        Location location = pearl.getLocation();
        
        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("终望珍珠在 " + location + " 落地，开始黑洞效果");
        }
        
        createBlackHole(location);
    }
    
    private void createBlackHole(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        
        // 黑洞持续时间（tick）
        final int duration = 100; // 5秒
        final double pullRadius = 20.0; // 吸引半径
        final double damageRadius = 3.0; // 伤害半径
        
        AtomicInteger tick = new AtomicInteger(0);
        
        FoliaUtil.runTimer(plugin, center, task -> {
            int currentTick = tick.getAndIncrement();
            
            if (currentTick >= duration) {
                world.createExplosion(center, 8.0F, false, true);
                world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
                
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("黑洞在 " + center + " 爆炸");
                }
                
                task.cancel();
                return;
            }
            
            spawnBlackHoleParticles(center);
            
            if (currentTick % 20 == 0 && random.nextInt(3) == 0) {
                world.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.7F);
            }
            
            Collection<Entity> nearbyEntities = world.getNearbyEntities(center, pullRadius, pullRadius, pullRadius);
            
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof LivingEntity) && !(entity instanceof Item) && !(entity instanceof Snowball)) {
                    continue;
                }
                
                Location entityLoc = entity.getLocation();
                double distance = entityLoc.distance(center);
                
                if (distance > 0.5) {
                    Vector direction = center.toVector().subtract(entityLoc.toVector()).normalize();
                    double pullStrength = Math.min(1.0, (pullRadius - distance) / pullRadius) * 0.8;
                    entity.setVelocity(direction.multiply(pullStrength));
                }
            
                if (entity instanceof LivingEntity && distance < damageRadius) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(2.0);
                    living.sendMessage(ChatColor.DARK_PURPLE + "你被黑洞吞噬！");
                    
                    world.spawnParticle(Particle.PORTAL, entityLoc, 20, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }, 1L, 1L);
    }
    
    private void spawnBlackHoleParticles(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.LARGE_SMOKE, center, 10, 0.3, 0.3, 0.3, 0.02);
        world.spawnParticle(Particle.SQUID_INK, center, 5, 0.2, 0.2, 0.2, 0.01);
        
        for (int i = 0; i < 3; i++) {
            double angle = (System.currentTimeMillis() / 100.0 + i * 120) % 360;
            double radius = 2.0 + Math.sin(System.currentTimeMillis() / 500.0) * 0.5;
            
            double x = center.getX() + radius * Math.cos(Math.toRadians(angle));
            double z = center.getZ() + radius * Math.sin(Math.toRadians(angle));
            double y = center.getY() + Math.sin(Math.toRadians(angle * 2)) * 0.5;
            
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.PORTAL, particleLoc, 2, 0, 0, 0, 0.1);
            world.spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
        }
        
        world.spawnParticle(Particle.DRAGON_BREATH, center, 15, 2.5, 2.5, 2.5, 0.05);
        
        if (random.nextInt(10) == 0) {
            world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0F, 0.5F);
        }
    }
}

