package online.yudream.bungeemodlist;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class BungeeModList extends Plugin implements Listener {

    private Configuration config;
    private ServerPing.ModInfo cachedModInfo;

    @Override
    public void onEnable() {
        Path dataFolder = getDataFolder().toPath();
        Path configFile = dataFolder.resolve("config.yml");
        try {
            Files.createDirectories(dataFolder);
            if (!Files.exists(configFile)) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile);
                }
            }
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile.toFile());
        } catch (IOException ex) {
            getLogger().severe("配置文件操作失败: " + ex.getMessage());
        }
        getProxy().getScheduler().schedule(this, this::updateModCache, 0, config.getLong("updateTime"), TimeUnit.SECONDS);
        getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void updateModCache() {
        getProxy().getServerInfo(config.getString("serverName")).ping((result, error) -> {
            if (error != null) {
                getLogger().warning("无法获取子服 Mod 列表: " + error.getMessage());
                return;
            }
            if (result != null && result.getModinfo() != null) {
                cachedModInfo = result.getModinfo();
            }
        });
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        if (cachedModInfo == null) return;
        ServerPing response = event.getResponse() == null ? new ServerPing() : event.getResponse();
        ServerPing.ModInfo modInfo = response.getModinfo();
        modInfo.setType(cachedModInfo.getType());
        modInfo.setModList(cachedModInfo.getModList());
        event.setResponse(response);
    }
}
