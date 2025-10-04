package org.little100.avaritia_Plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.little100.avaritia_Plugin.Avaritia_Plugin;
import org.little100.avaritia_Plugin.database.BlockDatabase;
import org.little100.avaritia_Plugin.util.FoliaUtil;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class BlockListener implements Listener {

    private final Avaritia_Plugin plugin;
    private final BlockDatabase database;
    private static final double Y_OFFSET = -0.39;
    private final NamespacedKey BLOCK_TYPE_KEY;
    private final NamespacedKey ITEM_ID_KEY;

    public BlockListener(Avaritia_Plugin plugin, BlockDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.BLOCK_TYPE_KEY = new NamespacedKey(plugin, "block_type");
        this.ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!PDCUtil.isPluginItem(item)) {
            return;
        }

        String itemId = PDCUtil.getItemId(item);
        if (itemId == null) {
            return;
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("[方块放置] 检测到插件物品: " + itemId);
        }

        boolean allowPlaceBlock = plugin.getConfig().getBoolean("allow_place_block", false);

        boolean isSpecialBlock = itemId.equals("compressed_crafting_table") ||
                itemId.equals("double_compressed_crafting_table") ||
                itemId.equals("crystal_matrix");

        if (isSpecialBlock && !allowPlaceBlock) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(player, "block.place_disabled");
            if (player.hasPermission("avaritia.admin")) {
                plugin.getLanguageManager().sendMessage(player, "block.place_disabled_hint");
            }
            return;
        }

        boolean isPlaceableBlock = itemId.equals("extreme_crafting_table") ||
                itemId.equals("neutron_collector") ||
                itemId.equals("neutronium_compressor") ||
                (isSpecialBlock && allowPlaceBlock);

        if (!isPlaceableBlock) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("[方块放置] 已拦截物品放置: " + itemId + " (此物品不是方块)");
            }
            return;
        }

        if (isPlaceableBlock) {
            Block block = event.getBlockPlaced();
            Location blockLoc = block.getLocation();

            block.setType(Material.PRISMARINE_STAIRS, false);

            if (block.getBlockData() instanceof Stairs) {
                Stairs stairsData = (Stairs) block.getBlockData();
                stairsData.setHalf(Bisected.Half.TOP);
                stairsData.setFacing(player.getFacing().getOppositeFace());
                block.setBlockData(stairsData, true);
            }

            Location armorStandLoc = blockLoc.clone().add(0.5, Y_OFFSET, 0.5);
            ArmorStand armorStand = (ArmorStand) blockLoc.getWorld().spawnEntity(armorStandLoc, EntityType.ARMOR_STAND);

            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setBasePlate(false);
            armorStand.setArms(false);
            armorStand.setSmall(true);
            armorStand.setMarker(true);

            ItemStack displayItem = item.clone();
            displayItem.setAmount(1);
            armorStand.getEquipment().setHelmet(displayItem);

            PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
            pdc.set(BLOCK_TYPE_KEY, PersistentDataType.STRING, itemId);
            pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);

            plugin.getLogger().info("保存到盔甲架PDC: BLOCK_TYPE_KEY='" + itemId + "', ITEM_ID_KEY='" + itemId + "'");

            database.saveBlock(blockLoc, itemId, armorStand.getUniqueId(), itemId);

            if (itemId.equals("neutron_collector")) {
                plugin.getNeutronCollectorGUI().registerCollector(blockLoc);
            }

            if (itemId.equals("neutronium_compressor")) {
                plugin.getNeutroniumCompressorGUI().registerCompressor(blockLoc);
            }

            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("玩家 " + player.getName() + " 放置了 " + itemId + " 在 " + blockLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        // 检查数据库中是否有此方块
        if (!database.hasBlock(blockLoc)) {
            return;
        }

        // 从数据库获取盔甲架UUID
        UUID armorStandUUID = database.getArmorStandUUID(blockLoc);
        if (armorStandUUID == null) {
            return;
        }

        // 防止原版掉落
        event.setCancelled(true);

        // 查找盔甲架
        Entity armorStandEntity = Bukkit.getEntity(armorStandUUID);
        String itemId = null;

        if (armorStandEntity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) armorStandEntity;

            // 获取itemId
            PersistentDataContainer pdc = stand.getPersistentDataContainer();
            if (pdc.has(ITEM_ID_KEY, PersistentDataType.STRING)) {
                itemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
            }

            ItemStack helmet = stand.getEquipment().getHelmet();
            if (helmet != null && helmet.getType() != Material.AIR) {
                ItemStack dropItem = helmet.clone();
                dropItem.setAmount(1); // 确保只掉落1个
                block.getWorld().dropItemNaturally(blockLoc, dropItem);
            }

            stand.remove();
        }

        // 移除方块
        block.setType(Material.AIR);

        if (itemId != null && itemId.equals("neutron_collector")) {
            plugin.getNeutronCollectorGUI().unregisterCollector(blockLoc);
        }

        if (itemId != null && itemId.equals("neutronium_compressor")) {
            // 检查是否启用了压缩机保护
            if (plugin.getConfig().getBoolean("compressor_protect", false)) {
                // 获取压缩机内部进度并掉落对应的低阶材料
                List<ItemStack> drops = plugin.getNeutroniumCompressorGUI().getCompressorDrops(blockLoc);
                for (ItemStack drop : drops) {
                    block.getWorld().dropItemNaturally(blockLoc, drop);
                }
            }

            plugin.getNeutroniumCompressorGUI().unregisterCompressor(blockLoc);
        }

        // 从数据库删除记录
        database.removeBlock(blockLoc);

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info("玩家 " + event.getPlayer().getName() + " 破坏了 " + (itemId != null ? itemId : "特殊方块"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) event.getRightClicked();
            PersistentDataContainer pdc = stand.getPersistentDataContainer();

            if (pdc.has(BLOCK_TYPE_KEY, PersistentDataType.STRING)) {
                event.setCancelled(true);

                String blockType = pdc.get(BLOCK_TYPE_KEY, PersistentDataType.STRING);
                Player player = event.getPlayer();

                plugin.getLogger().info("玩家 " + player.getName() + " 右键盔甲架，blockType: '" + blockType + "'");

                if ("extreme_crafting_table".equals(blockType)) {
                    plugin.getExtremeTableGUI().openMainGUI(player);

                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 打开了终极工作台GUI");
                    }
                } else if ("neutron_collector".equals(blockType)) {
                    plugin.getNeutronCollectorGUI().openCollectorGUI(player,
                            stand.getLocation().getBlock().getLocation());

                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 打开了中子态素收集器GUI");
                    }
                } else if ("neutronium_compressor".equals(blockType)) {
                    plugin.getNeutroniumCompressorGUI().openCompressorGUI(player,
                            stand.getLocation().getBlock().getLocation());

                    if (plugin.getConfig().getBoolean("debug", true)) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 打开了中子态素压缩机GUI");
                    }
                } else {
                    player.sendMessage("§c此方块暂无功能");
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) event.getEntity();
            PersistentDataContainer pdc = stand.getPersistentDataContainer();

            if (pdc.has(BLOCK_TYPE_KEY, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Stairs)) {
            return;
        }

        Location blockLoc = block.getLocation();

        Collection<Entity> nearbyEntities = block.getWorld().getNearbyEntities(
                blockLoc.clone().add(0.5, 0, 0.5),
                0.5, 0.5, 0.5,
                entity -> entity.getType() == EntityType.ARMOR_STAND);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                PersistentDataContainer pdc = stand.getPersistentDataContainer();

                if (pdc.has(BLOCK_TYPE_KEY, PersistentDataType.STRING)) {
                    String blockType = pdc.get(BLOCK_TYPE_KEY, PersistentDataType.STRING);

                    if ("extreme_crafting_table".equals(blockType)) {
                        event.setCancelled(true);
                        Player player = event.getPlayer();

                        plugin.getExtremeTableGUI().openMainGUI(player);

                        if (plugin.getConfig().getBoolean("debug", true)) {
                            plugin.getLogger().info("玩家 " + player.getName() + " 打开了终极工作台GUI");
                        }
                    } else if ("neutron_collector".equals(blockType)) {
                        event.setCancelled(true);
                        Player player = event.getPlayer();

                        plugin.getNeutronCollectorGUI().openCollectorGUI(player, blockLoc);

                        if (plugin.getConfig().getBoolean("debug", true)) {
                            plugin.getLogger().info("玩家 " + player.getName() + " 打开了中子态素收集器GUI");
                        }
                    } else if ("neutronium_compressor".equals(blockType)) {
                        event.setCancelled(true);
                        Player player = event.getPlayer();

                        plugin.getNeutroniumCompressorGUI().openCompressorGUI(player, blockLoc);

                        if (plugin.getConfig().getBoolean("debug", true)) {
                            plugin.getLogger().info("玩家 " + player.getName() + " 打开了中子态素压缩机GUI");
                        }
                    } else {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage("§c此方块暂无功能");
                    }
                    break;
                }
            }
        }
    }
}