package org.little100.avaritia_Plugin.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.little100.avaritia_Plugin.Avaritia_Plugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockDatabase {
    
    private final Avaritia_Plugin plugin;
    private Connection connection;
    private final File databaseFile;
    
    public BlockDatabase(Avaritia_Plugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "blocks.db");
    }
    
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            createTables();
            
            plugin.getLogger().info("方块数据库初始化成功！");
        } catch (Exception e) {
            plugin.getLogger().severe("方块数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS custom_blocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "block_type TEXT NOT NULL," +
                    "armor_stand_uuid TEXT NOT NULL," +
                    "item_id TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(world, x, y, z)" +
                    ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public void saveBlock(Location location, String blockType, UUID armorStandUUID, String itemId) {
        String sql = "INSERT OR REPLACE INTO custom_blocks (world, x, y, z, block_type, armor_stand_uuid, item_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.setString(5, blockType);
            pstmt.setString(6, armorStandUUID.toString());
            pstmt.setString(7, itemId);
            pstmt.executeUpdate();
            
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("保存方块到数据库: " + blockType + " at " + location);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("保存方块失败: " + e.getMessage());
        }
    }
    
    public void removeBlock(Location location) {
        String sql = "DELETE FROM custom_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.executeUpdate();
            
            if (plugin.getConfig().getBoolean("debug", true)) {
                plugin.getLogger().info("从数据库删除方块: " + location);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("删除方块失败: " + e.getMessage());
        }
    }
    
    public boolean hasBlock(Location location) {
        String sql = "SELECT COUNT(*) FROM custom_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("检查方块失败: " + e.getMessage());
        }
        
        return false;
    }
    
    public UUID getArmorStandUUID(Location location) {
        String sql = "SELECT armor_stand_uuid FROM custom_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("armor_stand_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取盔甲架UUID失败: " + e.getMessage());
        }
        
        return null;
    }
    
    public String getBlockType(Location location) {
        String sql = "SELECT block_type FROM custom_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("block_type");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取方块类型失败: " + e.getMessage());
        }
        
        return null;
    }
    
    public Map<Location, UUID> loadAllBlocks() {
        Map<Location, UUID> blocks = new HashMap<>();
        String sql = "SELECT * FROM custom_blocks";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                UUID armorStandUUID = UUID.fromString(rs.getString("armor_stand_uuid"));
                
                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                blocks.put(loc, armorStandUUID);
            }
            
            plugin.getLogger().info("从数据库加载了 " + blocks.size() + " 个方块");
        } catch (SQLException e) {
            plugin.getLogger().warning("加载方块失败: " + e.getMessage());
        }
        
        return blocks;
    }
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("方块数据库已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库失败: " + e.getMessage());
            }
        }
    }
}
