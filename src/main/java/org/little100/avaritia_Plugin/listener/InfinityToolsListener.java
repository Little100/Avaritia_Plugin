package org.little100.avaritia_Plugin.listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.util.PDCUtil;
import org.little100.avaritia_Plugin.util.FoliaUtil;
import org.little100.avaritia_Plugin.manager.LanguageManager;

import java.util.*;

public class InfinityToolsListener implements Listener {

    private final Avaritia_Plugin plugin;
    private final LanguageManager languageManager;
    private final Map<UUID, Long> lastInteractTime = new HashMap<>();

    private final Map<UUID, Boolean> hammerMode = new HashMap<>();

    private final Map<UUID, ItemStack> infinityBowOffhandBackup = new HashMap<>();

    public InfinityToolsListener(Avaritia_Plugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();

        startPickaxeSpeedTask();
    }

    private void startPickaxeSpeedTask() {
        FoliaUtil.runGlobalTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                String itemId = PDCUtil.getItemId(item);

                if ("world_breaker".equals(itemId)) {

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {

                        if (!meta.hasEnchant(Enchantment.EFFICIENCY) ||
                                meta.getEnchantLevel(Enchantment.EFFICIENCY) < 10) {

                            meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
                            item.setItemMeta(meta);

                            if (plugin.getConfig().getBoolean("debug", true)) {
                                plugin.getLogger().info("[世界崩解之镐] 为玩家 " + player.getName() + " 的镐子添加效率X");
                            }
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onWorldBreakerToggle(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking())
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        String itemId = PDCUtil.getItemId(item);
        if (!"world_breaker".equals(itemId))
            return;

        event.setCancelled(true);

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractTime.get(player.getUniqueId());
        if (lastTime != null && currentTime - lastTime < 500) {
            return;
        }
        lastInteractTime.put(player.getUniqueId(), currentTime);

        UUID uuid = player.getUniqueId();
        boolean isHammer = hammerMode.getOrDefault(uuid, false);
        hammerMode.put(uuid, !isHammer);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {

            Integer newCMD = !isHammer ? plugin.getConfig().getInt("infinity_tools.hammer_cmd", 101) : 100;

            Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
            boolean wasUnbreakable = meta.isUnbreakable();

            item.setItemMeta(meta);

            item = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(item,
                    String.valueOf(newCMD));

            meta = item.getItemMeta();
            if (meta == null) {
                plugin.getLogger().warning("[无尽镐] 设置CMD后meta为null！");
                return;
            }

            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                if (!meta.hasEnchant(entry.getKey())) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            if (!meta.hasEnchant(Enchantment.EFFICIENCY) || meta.getEnchantLevel(Enchantment.EFFICIENCY) < 10) {
                meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
            }

            if (wasUnbreakable) {
                meta.setUnbreakable(true);
            }

            List<String> lore = meta.getLore();
            if (lore == null)
                lore = new ArrayList<>();

            lore.removeIf(line -> line.contains("当前模式") || line.contains("Current Mode"));

            if (!isHammer) {
                lore.add(ChatColor.RED + "» 当前模式: 锤子模式");
                player.sendMessage(ChatColor.GOLD + "» " + ChatColor.YELLOW + "切换到" + ChatColor.RED + "锤子模式" +
                        ChatColor.GRAY + " - 可破坏16x16x16范围");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0F, 1.2F);
            } else {
                lore.add(ChatColor.AQUA + "» 当前模式: 镐模式");
                player.sendMessage(ChatColor.GOLD + "» " + ChatColor.YELLOW + "切换到" + ChatColor.AQUA + "镐模式" +
                        ChatColor.GRAY + " - 极速挖掘");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0F, 2.0F);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[世界崩解之镐] 玩家 " + player.getName() + " 切换到" +
                        (!isHammer ? "锤子模式" : "镐模式") +
                        ", CMD: " + newCMD +
                        ", 效率附魔: " + meta.getEnchantLevel(Enchantment.EFFICIENCY));
            }
        }
    }

    @EventHandler
    public void onMatterClusterThrow(ProjectileLaunchEvent event) {

        if (plugin.getConfig().getBoolean("debug", true)) {
            if (event.getEntity() instanceof Snowball) {
                plugin.getLogger().info("[投掷物] 检测到雪球发射事件");
            }
        }

        if (!(event.getEntity() instanceof Snowball))
            return;
        if (!(event.getEntity().getShooter() instanceof Player))
            return;

        Player player = (Player) event.getEntity().getShooter();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        ItemStack item = null;
        boolean isMainHand = false;

        String mainHandId = PDCUtil.getItemId(mainHand);
        String offHandId = PDCUtil.getItemId(offHand);

        if ("matter_cluster".equals(mainHandId)) {
            item = mainHand;
            isMainHand = true;
        } else if ("matter_cluster".equals(offHandId)) {

            item = offHand;
            isMainHand = false;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            if (mainHand != null && mainHand.getType() == Material.SNOWBALL) {
                plugin.getLogger().info("[投掷物] 主手: " + mainHandId);
            }
            if (offHand != null && offHand.getType() == Material.SNOWBALL) {
                plugin.getLogger().info("[投掷物] 副手: " + offHandId);
            }
        }

        if (item == null)
            return;

        event.setCancelled(true);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[物质团] 拦截玩家 " + player.getName() + " 扔出物质团，改为释放物品");
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "» 物质团数据错误");
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey itemsKey = new NamespacedKey(plugin, "stored_items");

        String itemsData = pdc.get(itemsKey, PersistentDataType.STRING);
        if (itemsData == null || itemsData.isEmpty()) {
            player.sendMessage(ChatColor.RED + "» 物质团内没有物品");
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().warning("[物质团] 没有存储数据: " + itemsData);
            }
            return;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[物质团] 存储数据: " + itemsData);
        }

        String[] itemEntries = itemsData.split(";");
        int returnedCount = 0;

        for (String entry : itemEntries) {
            try {
                if (entry.trim().isEmpty())
                    continue;

                String[] parts = entry.split(":");
                if (parts.length < 2)
                    continue;

                Material material = Material.valueOf(parts[0]);
                int amount = Integer.parseInt(parts[1]);

                ItemStack returnItem = new ItemStack(material, amount);

                player.getWorld().dropItemNaturally(player.getLocation(), returnItem);

                returnedCount += amount;

                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[物质团] 掉落: " + material + " x" + amount);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[物质团] 解析物品失败: " + entry + " - " + e.getMessage());
            }
        }

        if (isMainHand) {
            mainHand.setAmount(mainHand.getAmount() - 1);
            player.getInventory().setItemInMainHand(mainHand);
        } else {
            offHand.setAmount(offHand.getAmount() - 1);
            player.getInventory().setItemInOffHand(offHand);
        }

        player.sendMessage(ChatColor.GREEN + "» 物质团已释放，掉落了 " + returnedCount + " 个物品");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR)
            return;

        String itemId = PDCUtil.getItemId(item);
        if (itemId == null)
            return;

        Block block = event.getBlock();

        switch (itemId) {
            case "world_breaker":
                handleWorldBreaker(event, player, block);
                break;
            case "planet_heaver":
                handlePlanetHeaver(event, player, block);
                break;
            case "natures_ruin":
                handleNaturesRuin(event, player, block);
                break;
        }
    }

    private void handleWorldBreaker(BlockBreakEvent event, Player player, Block block) {
        boolean isHammer = hammerMode.getOrDefault(player.getUniqueId(), false);

        if (isHammer) {

            event.setCancelled(true);

            Location center = block.getLocation();
            World world = center.getWorld();

            boolean canBreakBedrock = plugin.getConfig().getBoolean("infinity_tools.hammer_break_bedrock", false);

            Map<Material, Integer> collectedItems = new HashMap<>();
            int broken = 0;
            int radius = 8;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block target = world.getBlockAt(
                                center.getBlockX() + x,
                                center.getBlockY() + y,
                                center.getBlockZ() + z);

                        Material targetType = target.getType();

                        if (targetType == Material.AIR) {
                            continue;
                        }

                        if (targetType == Material.BEDROCK) {
                            if (!canBreakBedrock) {
                                continue;
                            }
                        }

                        Collection<ItemStack> drops = target.getDrops(player.getInventory().getItemInMainHand());
                        for (ItemStack drop : drops) {
                            collectedItems.merge(drop.getType(), drop.getAmount(), Integer::sum);
                        }

                        target.setType(Material.AIR);
                        broken++;

                        if (broken >= 4096)
                            break;
                    }
                    if (broken >= 4096)
                        break;
                }
                if (broken >= 4096)
                    break;
            }

            if (broken > 0) {

                ItemStack matterCluster = createMatterCluster(collectedItems);

                world.dropItemNaturally(center, matterCluster);

                player.sendMessage(ChatColor.GRAY + "» 破坏了 " + ChatColor.YELLOW + broken +
                        ChatColor.GRAY + " 个方块，收集到物质团");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.2F);

                world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 4, 4, 4, 0);
            }
        } else {

        }
    }

    private ItemStack createMatterCluster(Map<Material, Integer> items) {

        ItemStack cluster = new ItemStack(Material.SNOWBALL);

        cluster = PDCUtil.addPluginData(cluster, "matter_cluster");

        ItemMeta meta = cluster.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("[物质团] 无法获取ItemMeta！");
            return cluster;
        }

        String displayName = languageManager.getMessage("zh_cn", "items.matter_cluster.name");
        if (!displayName.startsWith("Missing translation:")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        } else {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "物质团");
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey itemsKey = new NamespacedKey(plugin, "stored_items");

        StringBuilder itemsData = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            if (itemsData.length() > 0) {
                itemsData.append(";");
            }
            itemsData.append(entry.getKey().name()).append(":").append(entry.getValue());
        }

        String itemsDataStr = itemsData.toString();
        pdc.set(itemsKey, PersistentDataType.STRING, itemsDataStr);

        NamespacedKey uuidKey = new NamespacedKey(plugin, "cluster_uuid");
        pdc.set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        List<String> lore = new ArrayList<>();

        String loreLine = languageManager.getMessage("zh_cn", "items.matter_cluster.lore.0");
        if (!loreLine.startsWith("Missing translation:")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
        } else {
            lore.add(ChatColor.GRAY + "右键释放所有收集的物品");
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "包含 " + ChatColor.YELLOW + items.size() + ChatColor.GRAY + " 种物品");
        int totalCount = items.values().stream().mapToInt(Integer::intValue).sum();
        lore.add(ChatColor.GRAY + "总共 " + ChatColor.YELLOW + totalCount + ChatColor.GRAY + " 个");

        meta.setLore(lore);
        cluster.setItemMeta(meta);

        cluster = org.little100.avaritia_Plugin.util.CustomModelDataUtil.setCustomModelData(cluster, "102");

        if (plugin.getConfig().getBoolean("debug", true)) {
            String detectedId = PDCUtil.getItemId(cluster);
            plugin.getLogger().info("[物质团创建] CMD: 102, 物品ID: " + detectedId + ", 数据: " + itemsDataStr);
        }

        return cluster;
    }

    private void handlePlanetHeaver(BlockBreakEvent event, Player player, Block block) {
        event.setCancelled(true);

        Location center = block.getLocation();
        World world = center.getWorld();

        int broken = 0;
        int radius = 2;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                for (int y = 0; y <= 2; y++) {
                    Block target = world.getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() - y,
                            center.getBlockZ() + z);

                    if (target.getType() == Material.AIR || target.getType() == Material.BEDROCK) {
                        continue;
                    }

                    Collection<ItemStack> drops = target.getDrops(player.getInventory().getItemInMainHand());
                    target.setType(Material.AIR);

                    for (ItemStack drop : drops) {
                        world.dropItemNaturally(target.getLocation(), drop);
                    }

                    broken++;
                }
            }
        }

        if (broken > 0) {
            player.sendMessage(ChatColor.GRAY + "» 挖掘了 " + ChatColor.YELLOW + broken + ChatColor.GRAY + " 个方块");
        }
    }

    private void handleNaturesRuin(BlockBreakEvent event, Player player, Block block) {

        if (!block.getType().name().contains("LOG")) {
            return;
        }

        event.setCancelled(true);

        Set<Block> treeBlocks = new HashSet<>();
        findTreeBlocks(block, treeBlocks, 0);

        World world = block.getWorld();
        int broken = 0;

        for (Block treeBlock : treeBlocks) {
            Collection<ItemStack> drops = treeBlock.getDrops(player.getInventory().getItemInMainHand());
            treeBlock.setType(Material.AIR);

            for (ItemStack drop : drops) {
                world.dropItemNaturally(treeBlock.getLocation(), drop);
            }

            broken++;
        }

        if (broken > 0) {
            player.sendMessage(ChatColor.GRAY + "» 砍倒了一棵树，共 " + ChatColor.YELLOW + broken + ChatColor.GRAY + " 个方块");
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0F, 0.8F);
        }
    }

    private void findTreeBlocks(Block block, Set<Block> found, int depth) {
        if (depth > 200 || found.size() > 500)
            return;
        if (found.contains(block))
            return;

        Material type = block.getType();
        if (!type.name().contains("LOG") && !type.name().contains("LEAVES")) {
            return;
        }

        found.add(block);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;

                    Block neighbor = block.getRelative(x, y, z);
                    findTreeBlocks(neighbor, found, depth + 1);
                }
            }
        }
    }

    @EventHandler
    public void onInfinityBowUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = event.getItem();
        if (mainHand == null || mainHand.getType() != Material.BOW)
            return;

        String itemId = PDCUtil.getItemId(mainHand);
        if (!"infinity_bow".equals(itemId))
            return;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (player.getInventory().contains(Material.ARROW) ||
                (offHand != null && offHand.getType() == Material.ARROW)) {

            return;
        }

        UUID uuid = player.getUniqueId();
        if (offHand != null && offHand.getType() != Material.AIR) {
            infinityBowOffhandBackup.put(uuid, offHand.clone());
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[无尽弓] 备份玩家 " + player.getName() + " 副手物品: " + offHand.getType());
            }
        } else {
            infinityBowOffhandBackup.put(uuid, null);
        }

        player.getInventory().setItemInOffHand(new ItemStack(Material.ARROW, 1));

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[无尽弓] 为玩家 " + player.getName() + " 临时替换副手为箭");
        }
    }

    @EventHandler
    public void onInfinityBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();
        if (bow == null)
            return;

        String bowId = PDCUtil.getItemId(bow);
        if (!"infinity_bow".equals(bowId))
            return;

        UUID uuid = player.getUniqueId();
        FoliaUtil.runEntityTaskLater(plugin, player, () -> {
            restoreOffhand(player, uuid);
        }, 1L);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (infinityBowOffhandBackup.containsKey(uuid)) {

            FoliaUtil.runEntityTaskLater(plugin, player, () -> {
                restoreOffhand(player, uuid);
            }, 1L);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        String mainHandId = PDCUtil.getItemId(mainHand);
        if ("infinity_bow".equals(mainHandId)) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[无尽弓] 阻止玩家 " + player.getName() + " 切换副手");
            }
            return;
        }

        if (infinityBowOffhandBackup.containsKey(uuid)) {
            ItemStack backup = infinityBowOffhandBackup.remove(uuid);

            event.setCancelled(true);

            if (backup != null && backup.getType() != Material.AIR) {
                player.getInventory().setItemInMainHand(backup);
            }

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[无尽弓] 玩家 " + player.getName() + " 切换副手，恢复物品");
            }
        }
    }

    private void restoreOffhand(Player player, UUID uuid) {
        if (!infinityBowOffhandBackup.containsKey(uuid))
            return;

        ItemStack backup = infinityBowOffhandBackup.remove(uuid);
        ItemStack currentOffhand = player.getInventory().getItemInOffHand();

        if (currentOffhand == null || currentOffhand.getType() == Material.AIR
                || currentOffhand.getType() == Material.ARROW) {
            if (backup != null && backup.getType() != Material.AIR) {
                player.getInventory().setItemInOffHand(backup);
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[无尽弓] 恢复玩家 " + player.getName() + " 副手物品: " + backup.getType());
                }
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                if (plugin.getConfig().getBoolean("debug", true)) {
                    plugin.getLogger().info("[无尽弓] 清空玩家 " + player.getName() + " 副手");
                }
            }
        } else {

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().warning("[无尽弓] 副手已被 " + currentOffhand.getType() + " 占用，无法恢复备份的 "
                        + (backup != null ? backup.getType() : "null"));
            }
        }
    }

    @EventHandler
    public void onHoeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null)
            return;

        String itemId = PDCUtil.getItemId(item);
        if (!"hoe_of_the_stars".equals(itemId))
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Material type = block.getType();
        if (type != Material.DIRT && type != Material.GRASS_BLOCK && type != Material.FARMLAND) {
            return;
        }

        event.setCancelled(true);

        Location center = block.getLocation();
        World world = center.getWorld();
        int radius = 4;
        int tilled = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block target = world.getBlockAt(
                        center.getBlockX() + x,
                        center.getBlockY(),
                        center.getBlockZ() + z);

                Material targetType = target.getType();
                if (targetType == Material.DIRT || targetType == Material.GRASS_BLOCK) {

                    Block above = target.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        target.setType(Material.FARMLAND);
                        tilled++;
                    }
                }
            }
        }

        if (tilled > 0) {
            player.sendMessage(ChatColor.GRAY + "» 耕地了 " + ChatColor.YELLOW + tilled + ChatColor.GRAY + " 个方块");
            player.playSound(player.getLocation(), Sound.ITEM_HOE_TILL, 1.0F, 1.0F);
        }
    }
}
