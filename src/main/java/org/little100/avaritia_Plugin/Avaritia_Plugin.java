package org.little100.avaritia_Plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.little100.avaritia_Plugin.command.AvaritiaCommand;
import org.little100.avaritia_Plugin.database.BlockDatabase;
import org.little100.avaritia_Plugin.listener.CraftingListener;
import org.little100.avaritia_Plugin.manager.CustomCraftingManager;
import org.little100.avaritia_Plugin.manager.GUIManager;
import org.little100.avaritia_Plugin.manager.ItemManager;
import org.little100.avaritia_Plugin.manager.LanguageManager;
import org.little100.avaritia_Plugin.recipe.RecipeManager;
import org.little100.avaritia_Plugin.util.CustomModelDataUtil;
import org.little100.avaritia_Plugin.util.FoliaUtil;
import org.little100.avaritia_Plugin.util.PDCUtil;

import java.io.File;

public final class Avaritia_Plugin extends JavaPlugin {

    private boolean isPaper;
    private boolean isFolia;
    private LanguageManager languageManager;
    private RecipeManager recipeManager;
    private GUIManager guiManager;
    private CustomCraftingManager customCraftingManager;
    private ItemManager itemManager;
    private BlockDatabase blockDatabase;
    private org.little100.avaritia_Plugin.manager.ExtremeTableGUI extremeTableGUI;
    private org.little100.avaritia_Plugin.manager.NeutronCollectorGUI neutronCollectorGUI;
    private org.little100.avaritia_Plugin.manager.NeutroniumCompressorGUI neutroniumCompressorGUI;

    @Override
    public void onEnable() {
        // 检测服务器类型
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            isPaper = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                isPaper = true;
            } catch (ClassNotFoundException ex) {
                isPaper = false;
            }
        }

        // 保存默认配置文件
        saveDefaultConfig();

        // 保存所有资源文件（如果不存在）
        saveResourceIfNotExists("items.yml");
        saveResourceIfNotExists("recipe.yml");
        saveResourceIfNotExists("extreme_crafting_table.yml");
        saveResourceIfNotExists("neutron_collector.yml");
        saveResourceIfNotExists("neutronium_compressor.yml");
        saveResourceIfNotExists("lang/zh_cn.yml");
        saveResourceIfNotExists("lang/en_us.yml");
        saveResourceIfNotExists("lang/lzh.yml");
        saveResourceIfNotExists("pack/Avaritia Resourcepack.zip");

        // 检查调试模式
        boolean debugMode = getConfig().getBoolean("debug", true);
        if (debugMode) {
            getLogger().info("检测到服务器类型: " + FoliaUtil.getServerType());
        }

        // 初始化工具类
        CustomModelDataUtil.initialize(getLogger(), getConfig());
        PDCUtil.initialize(this);

        // 初始化数据库
        blockDatabase = new BlockDatabase(this);
        blockDatabase.initialize();

        // 初始化语言管理器
        languageManager = new LanguageManager(this);

        // 初始化物品管理器
        itemManager = new ItemManager(this, languageManager);

        // 初始化GUI配置管理器
        org.little100.avaritia_Plugin.manager.GUIConfigManager guiConfigManager = new org.little100.avaritia_Plugin.manager.GUIConfigManager(
                this);

        // 初始化GUI管理器
        guiManager = new GUIManager(this, languageManager, guiConfigManager);

        // 初始化终极工作台GUI管理器
        extremeTableGUI = new org.little100.avaritia_Plugin.manager.ExtremeTableGUI(this, languageManager);

        // 初始化中子态素收集器GUI管理器
        neutronCollectorGUI = new org.little100.avaritia_Plugin.manager.NeutronCollectorGUI(this, languageManager);

        // 初始化中子态素压缩机GUI管理器
        neutroniumCompressorGUI = new org.little100.avaritia_Plugin.manager.NeutroniumCompressorGUI(this,
                languageManager);

        // 初始化自定义合成管理器（必须在RecipeManager.loadRecipes()之前）
        customCraftingManager = new CustomCraftingManager(this);
        customCraftingManager.setLanguageManager(languageManager);

        // 初始化合成表管理器
        recipeManager = new RecipeManager(this, languageManager);

        // 设置CustomCraftingManager到RecipeManager，以便判断哪些合成需要自定义处理
        recipeManager.setCustomCraftingManager(customCraftingManager);

        // 加载合成表
        recipeManager.loadRecipes();

        // 注册主命令
        AvaritiaCommand mainCommand = new AvaritiaCommand(this, languageManager, guiManager, itemManager);
        this.getCommand("avaritia").setExecutor(mainCommand);
        this.getCommand("avaritia").setTabCompleter(mainCommand);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new CraftingListener(this, customCraftingManager), this);
        getServer().getPluginManager()
                .registerEvents(new org.little100.avaritia_Plugin.listener.BlockListener(this, blockDatabase), this);
        getServer().getPluginManager().registerEvents(new org.little100.avaritia_Plugin.listener.FoodListener(this),
                this);
        getServer().getPluginManager()
                .registerEvents(new org.little100.avaritia_Plugin.listener.EndestPearlListener(this), this);
        getServer().getPluginManager()
                .registerEvents(new org.little100.avaritia_Plugin.listener.InfinityToolsListener(this), this);
        getServer().getPluginManager()
                .registerEvents(new org.little100.avaritia_Plugin.listener.InfinityArmorListener(this), this);
        getServer().getPluginManager().registerEvents(extremeTableGUI, this);
        getServer().getPluginManager().registerEvents(neutronCollectorGUI, this);
        getServer().getPluginManager().registerEvents(neutroniumCompressorGUI, this);

        getLogger().info("无尽贪婪插件已启用！");
    }

    @Override
    public void onDisable() {
        // 保存玩家语言设置
        if (languageManager != null) {
            languageManager.savePlayerLanguages();
        }

        // 关闭数据库连接
        if (blockDatabase != null) {
            blockDatabase.close();
        }

        getLogger().info("无尽贪婪插件已禁用！");
    }

    /**
     * 获取语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * 获取合成表管理器
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * 检查是否为Folia服务器
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * 获取GUI管理器
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * 获取自定义合成管理器
     */
    public CustomCraftingManager getCustomCraftingManager() {
        return customCraftingManager;
    }

    /**
     * 获取物品管理器
     */
    public ItemManager getItemManager() {
        return itemManager;
    }

    /**
     * 获取终极工作台GUI管理器
     */
    public org.little100.avaritia_Plugin.manager.ExtremeTableGUI getExtremeTableGUI() {
        return extremeTableGUI;
    }

    /**
     * 获取中子态素收集器GUI管理器
     */
    public org.little100.avaritia_Plugin.manager.NeutronCollectorGUI getNeutronCollectorGUI() {
        return neutronCollectorGUI;
    }

    /**
     * 获取中子态素压缩机GUI管理器
     */
    public org.little100.avaritia_Plugin.manager.NeutroniumCompressorGUI getNeutroniumCompressorGUI() {
        return neutroniumCompressorGUI;
    }

    /**
     * 保存资源文件（如果不存在）
     * 
     * @param resourcePath 资源路径
     */
    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
            getLogger().info("已保存资源文件: " + resourcePath);
        }
    }
}