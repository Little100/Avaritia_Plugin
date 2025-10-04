/**
 * Claude 4生成
 */
package org.little100.avaritia_Plugin.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Logger;

/**
 * CustomModelData工具类
 * 在高版本服务器(1.20.5+)上同时设置String和int类型的CustomModelData，完美兼容1.14-最新版的所有客户端
 * 在低版本服务器上只设置int类型的CustomModelData
 * 
 * 兼容策略：
 * - 高版本服务器：同时设置String类型(ResourceLocation/NamespacedKey)和int类型
 * - 低版本服务器：只设置int类型(传统方式)
 * - String类型：供1.20.5+客户端使用，格式为minecraft:数值
 * - int类型：供1.14-1.20.4客户端使用，直接数值
 */
public class CustomModelDataUtil {
    
    private static boolean isHighVersion = false;
    private static Logger logger;
    private static FileConfiguration config;
    private static boolean debugMode = false;
    private static boolean debugCustomModelData = false;
    
    static {
        try {
            // 检查是否为高版本 (1.20.5+)
            Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            isHighVersion = true;
        } catch (ClassNotFoundException e) {
            isHighVersion = false;
        }
    }
    
    /**
     * 初始化工具类
     * @param pluginLogger 插件的日志记录器
     * @param pluginConfig 插件配置
     */
    public static void initialize(Logger pluginLogger, FileConfiguration pluginConfig) {
        logger = pluginLogger;
        config = pluginConfig;
        debugMode = config.getBoolean("debug", true);
        debugCustomModelData = debugMode; // 调试模式开启时显示所有信息
        
        if (logger != null && debugMode) {
            logger.info("CustomModelDataUtil已初始化 - " + 
                       (isHighVersion ? "高版本API (1.21.4+)" : "传统API"));
        }
    }
    
    /**
     * 设置物品的CustomModelData
     * 在高版本同时设置String和int类型，确保完美兼容1.14-最新版的所有客户端
     * 
     * @param itemStack 要设置的物品
     * @param modelData 模型数据 (可以是String或int)
     * @return 设置后的物品
     */
    public static ItemStack setCustomModelData(ItemStack itemStack, Object modelData) {
        if (itemStack == null) {
            return null;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }
        
        if (isHighVersion) {
            // 高版本：同时设置String和int类型，兼容所有客户端
            boolean stringSuccess = false;
            boolean intSuccess = false;
            
            // 先设置int类型的CustomModelData（这会创建component）
            try {
                setLowVersionCustomModelData(meta, modelData);
                intSuccess = true;
                if (debugMode && logger != null) {
                    logger.info("✓ 成功设置int类型CustomModelData: " + modelData);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.warning("✗ int类型CustomModelData设置失败: " + e.getMessage());
                }
            }
            
            // 如果int类型成功，先应用meta然后重新获取，确保component被创建
            if (intSuccess) {
                // 先应用int类型的设置
                itemStack.setItemMeta(meta);
                // 重新获取meta，这样component应该被正确创建了
                meta = itemStack.getItemMeta();
                if (meta == null) {
                    if (logger != null) {
                        logger.severe("✗ 应用int类型后meta变为null");
                    }
                    return itemStack;
                }
                
                try {
                    // 获取原始字符串值（不是转换后的）
                    String originalStringValue;
                    if (modelData instanceof String) {
                        originalStringValue = (String) modelData;
                    } else {
                        originalStringValue = String.valueOf(modelData);
                    }
                    
                    // 现在重新获取component（应该存在了）
                    org.bukkit.inventory.meta.components.CustomModelDataComponent component = meta.getCustomModelDataComponent();
                    if (component != null) {
                        // 使用原始字符串值设置
                        component.setStrings(java.util.Collections.singletonList(originalStringValue));
                        meta.setCustomModelDataComponent(component);
                        stringSuccess = true;
                        if (debugMode && logger != null) {
                            logger.info("✓ 成功设置String类型CustomModelData: \"" + originalStringValue + "\"");
                        }
                    } else {
                        if (debugMode && logger != null) {
                            logger.warning("✗ 应用int后component仍为null，可能服务器版本不支持CustomModelDataComponent");
                        }
                    }
                } catch (Exception e) {
                    if (debugMode && logger != null) {
                        logger.warning("✗ String类型CustomModelData设置失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            // 最后再应用一次meta（包含String类型的设置）
            if (stringSuccess || intSuccess) {
                itemStack.setItemMeta(meta);
                if (logger != null) {
                    logger.info("✅ CustomModelData设置完成 - String: " + stringSuccess + ", Int: " + intSuccess);
                }
            } else {
                if (logger != null) {
                    logger.severe("❌ 无法设置任何类型的CustomModelData");
                }
            }
        } else {
            // 低版本：只设置int类型的CustomModelData
            try {
                setLowVersionCustomModelData(meta, modelData);
                itemStack.setItemMeta(meta);
                if (debugMode && logger != null) {
                    logger.info("✓ 成功设置CustomModelData: " + modelData);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.severe("✗ 无法设置CustomModelData: " + e.getMessage());
                }
            }
        }
        
        return itemStack;
    }
    
    /**
     * 设置高版本的CustomModelData (String类型)
     * 采用直接类型转换，避免反射权限问题
     */
    private static void setHighVersionCustomModelData(ItemMeta meta, Object modelData) throws Exception {
        String stringValue;
        if (modelData instanceof String) {
            stringValue = (String) modelData;
        } else if (modelData instanceof Integer) {
            stringValue = String.valueOf(modelData);
        } else {
            stringValue = modelData.toString();
        }
        
        try {
            // 直接使用类型转换，避免反射权限问题
            org.bukkit.inventory.meta.components.CustomModelDataComponent component = meta.getCustomModelDataComponent();
            
            if (component != null) {
                // 组件存在，直接设置字符串
                component.setStrings(java.util.Collections.singletonList(stringValue));
                meta.setCustomModelDataComponent(component);
                
                if (debugMode && logger != null) {
                    logger.info("成功设置String类型CustomModelData: " + stringValue);
                }
            } else {
                // 组件不存在的情况下，我们跳过String类型设置
                // 因为创建新组件需要复杂的操作，只要int类型工作就够了
                if (debugMode && logger != null) {
                    logger.info("CustomModelDataComponent为null，跳过String类型设置，依赖int类型兼容");
                }
                throw new Exception("CustomModelDataComponent组件不存在，无法设置String类型");
            }
                
        } catch (Exception e) {
            // 任何异常都抛出，让上层处理
            throw new Exception("设置高版本CustomModelData失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置低版本的CustomModelData (int类型)
     */
    private static void setLowVersionCustomModelData(ItemMeta meta, Object modelData) throws Exception {
        int intValue;
        if (modelData instanceof Integer) {
            intValue = (Integer) modelData;
        } else if (modelData instanceof String) {
            try {
                intValue = Integer.parseInt((String) modelData);
            } catch (NumberFormatException e) {
                // 如果字符串无法转为int，使用hashCode
                intValue = modelData.hashCode();
            }
        } else {
            intValue = modelData.hashCode();
        }
        
        // 使用传统的setCustomModelData方法
        meta.setCustomModelData(intValue);
    }
    
    /**
     * 获取物品的CustomModelData
     * 在高版本优先获取String类型，没有则获取int类型
     * 在低版本只获取int类型
     * 
     * @param itemStack 物品
     * @return 返回CustomModelData，可能是String或Integer，如果没有则返回null
     */
    public static Object getCustomModelData(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        if (isHighVersion) {
            // 高版本：优先获取String类型，没有则获取int类型
            try {
                Object stringData = getHighVersionCustomModelData(meta);
                if (stringData != null) {
                    return stringData;
                }
            } catch (Exception e) {
                // String类型获取失败，继续尝试int类型
            }
            
            try {
                return getLowVersionCustomModelData(meta);
            } catch (Exception e) {
                return null;
            }
        } else {
            // 低版本：只获取int类型
            try {
                return getLowVersionCustomModelData(meta);
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    /**
     * 获取高版本的CustomModelData (String类型)
     * 直接使用类型转换，避免反射权限问题
     */
    private static Object getHighVersionCustomModelData(ItemMeta meta) throws Exception {
        try {
            org.bukkit.inventory.meta.components.CustomModelDataComponent component = meta.getCustomModelDataComponent();
            if (component != null) {
                java.util.List<String> strings = component.getStrings();
                if (strings != null && !strings.isEmpty()) {
                    return strings.get(0);
                }
            }
        } catch (Exception e) {
            if (debugMode && logger != null) {
                logger.fine("获取String类型CustomModelData失败: " + e.getMessage());
            }
            throw new Exception("无法获取高版本CustomModelData: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取低版本的CustomModelData
     */
    private static Object getLowVersionCustomModelData(ItemMeta meta) throws Exception {
        if (meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }
        return null;
    }
    
    /**
     * 检查当前服务器版本是否为高版本
     * 
     * @return true如果是高版本(1.20.5+)，false如果是低版本
     */
    public static boolean isHighVersion() {
        return isHighVersion;
    }
    
    /**
     * 检查是否支持新的String类型CustomModelData API
     * 
     * @return true如果支持新API(1.20.5+)，false如果不支持
     */
    public static boolean isNewApiSupported() {
        return isHighVersion;
    }
    
    /**
     * 专门获取String类型的CustomModelData
     * 
     * @param itemStack 物品
     * @return String类型的CustomModelData，如果没有或不支持则返回null
     */
    public static String getCustomModelDataString(ItemStack itemStack) {
        if (itemStack == null || !isHighVersion) {
            return null;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        try {
            Object stringData = getHighVersionCustomModelData(meta);
            if (stringData instanceof String) {
                return (String) stringData;
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        
        return null;
    }
    
    /**
     * 专门获取int类型的CustomModelData
     * 
     * @param itemStack 物品
     * @return int类型的CustomModelData，如果没有则返回0
     */
    public static int getCustomModelDataInt(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        try {
            Object intData = getLowVersionCustomModelData(meta);
            if (intData instanceof Integer) {
                return (Integer) intData;
            }
        } catch (Exception e) {
            // 忽略异常，返回0
        }
        
        return 0;
    }
}
