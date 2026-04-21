package com.example.locatebarfolia;

import com.example.locatebarfolia.command.LocateBarCommand;
import com.example.locatebarfolia.config.LocateBarConfig;
import com.example.locatebarfolia.listener.PlayerConnectionListener;
import com.example.locatebarfolia.listener.PlayerMovementListener;
import com.example.locatebarfolia.listener.PlayerWorldListener;
import com.example.locatebarfolia.service.BedrockActionBarRenderer;
import com.example.locatebarfolia.service.BedrockCompatibility;
import com.example.locatebarfolia.service.LocateBarService;
import com.example.locatebarfolia.service.PlayerStateRegistry;
import com.example.locatebarfolia.service.VisibilityEvaluator;
import com.example.locatebarfolia.service.WaypointPacketSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocateBarPlugin extends JavaPlugin {
    private LocateBarConfig pluginConfig;
    private PlayerStateRegistry playerStateRegistry;
    private LocateBarService locateBarService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = LocateBarConfig.from(getConfig());
        this.playerStateRegistry = new PlayerStateRegistry();
        this.locateBarService = new LocateBarService(
            this,
            this.pluginConfig,
            this.playerStateRegistry,
            new VisibilityEvaluator(),
            new WaypointPacketSender(),
            new BedrockCompatibility(this),
            new BedrockActionBarRenderer()
        );

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this.locateBarService), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this.locateBarService), this);
        getServer().getPluginManager().registerEvents(new PlayerWorldListener(this.locateBarService), this);

        final LocateBarCommand command = new LocateBarCommand(this, this.locateBarService);
        final PluginCommand pluginCommand = getCommand("locatebar");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        for (final Player player : getServer().getOnlinePlayers()) {
            this.locateBarService.track(player);
        }
    }

    @Override
    public void onDisable() {
        if (this.locateBarService != null) {
            this.locateBarService.shutdown();
        }
    }

    public LocateBarConfig reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = LocateBarConfig.from(getConfig());
        this.locateBarService.reload(this.pluginConfig);
        return this.pluginConfig;
    }

    public LocateBarConfig updateScanRadius(final double scanRadius) {
        final double sanitizedRadius = LocateBarConfig.sanitizeScanRadius(scanRadius);
        getConfig().set("scan-radius", sanitizedRadius);
        saveConfig();
        this.pluginConfig = this.pluginConfig.withScanRadius(sanitizedRadius);
        this.locateBarService.reload(this.pluginConfig);
        return this.pluginConfig;
    }

    public LocateBarConfig getPluginConfig() {
        return this.pluginConfig;
    }
}
