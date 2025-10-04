package org.little100.avaritia_Plugin.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage;

    private static final String[] SUPPORTED_LANGUAGES = { "zh_cn", "en_us", "lzh" };

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;

        defaultLanguage = plugin.getConfig().getString("default_language", "zh_cn");
        loadLanguages();
        loadPlayerLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        for (String lang : SUPPORTED_LANGUAGES) {
            File langFile = new File(langFolder, lang + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + lang + ".yml", false);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            languages.put(lang, config);
            plugin.getLogger().info("已加载语言文件: " + lang + ".yml");
        }
    }

    private void loadPlayerLanguages() {
        File playerLangFile = new File(plugin.getDataFolder(), "player_languages.yml");
        if (playerLangFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerLangFile);
            for (String uuidString : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String language = config.getString(uuidString);
                    if (language != null && languages.containsKey(language)) {
                        playerLanguages.put(uuid, language);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的UUID在player_languages.yml中: " + uuidString);
                }
            }
        }
    }

    public void savePlayerLanguages() {
        File playerLangFile = new File(plugin.getDataFolder(), "player_languages.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, String> entry : playerLanguages.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(playerLangFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家语言设置失败: " + e.getMessage());
        }
    }

    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }

    public boolean setPlayerLanguage(Player player, String language) {
        if (!languages.containsKey(language)) {
            return false;
        }

        playerLanguages.put(player.getUniqueId(), language);
        savePlayerLanguages();
        return true;
    }

    public String getMessage(Player player, String key, Object... args) {
        String language = getPlayerLanguage(player);
        return getMessage(language, key, args);
    }

    public String getMessage(String language, String key, Object... args) {
        FileConfiguration config = languages.get(language);
        if (config == null) {
            config = languages.get(defaultLanguage);
        }

        String message = config.getString(key);
        if (message == null) {

            config = languages.get(defaultLanguage);
            if (config != null) {
                message = config.getString(key);
            }
        }

        if (message == null) {
            return "Missing translation: " + key;
        }

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }

        return message.replace("&", "§");
    }

    public void sendMessage(Player player, String key, Object... args) {
        player.sendMessage(getMessage(player, key, args));
    }

    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }

    public boolean isLanguageSupported(String language) {
        return languages.containsKey(language);
    }

    public String getLanguageDisplayName(String language) {
        FileConfiguration config = languages.get(language);
        if (config != null) {
            return config.getString("language.display_name", language);
        }
        return language;
    }

    public void reload() {
        languages.clear();
        loadLanguages();
        plugin.getLogger().info("语言文件已重新加载");
    }
}
