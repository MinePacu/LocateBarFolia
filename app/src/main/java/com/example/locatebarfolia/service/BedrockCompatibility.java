package com.example.locatebarfolia.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BedrockCompatibility {
    private final Plugin plugin;
    private final Object floodgateApi;
    private final Method isFloodgatePlayerMethod;
    private boolean warningLogged;

    public BedrockCompatibility(final Plugin plugin) {
        this.plugin = plugin;
        Object api = null;
        Method method = null;

        try {
            final Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            api = apiClass.getMethod("getInstance").invoke(null);
            method = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            plugin.getLogger().info("Floodgate API detected. Bedrock fallback display is available.");
        } catch (final ClassNotFoundException ignored) {
            plugin.getLogger().info("Floodgate API was not found. Bedrock fallback display is disabled.");
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            plugin.getLogger().log(Level.WARNING, "Floodgate API could not be initialized.", exception);
        }

        this.floodgateApi = api;
        this.isFloodgatePlayerMethod = method;
    }

    public boolean isBedrockPlayer(final Player player) {
        if (this.floodgateApi == null || this.isFloodgatePlayerMethod == null) {
            return false;
        }

        try {
            return (boolean) this.isFloodgatePlayerMethod.invoke(this.floodgateApi, player.getUniqueId());
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            if (!this.warningLogged) {
                this.plugin.getLogger().log(Level.WARNING, "Unable to query Floodgate player state.", exception);
                this.warningLogged = true;
            }
            return false;
        }
    }
}
