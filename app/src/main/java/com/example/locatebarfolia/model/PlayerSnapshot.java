package com.example.locatebarfolia.model;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record PlayerSnapshot(
    UUID playerId,
    UUID worldId,
    double x,
    double y,
    double z,
    float yaw,
    String name,
    GameMode gameMode,
    boolean locatorEnabled
) {
    public static PlayerSnapshot capture(final Player player, final boolean locatorEnabled) {
        return capture(player, player.getLocation(), locatorEnabled);
    }

    public static PlayerSnapshot capture(final Player player, final Location location, final boolean locatorEnabled) {
        final var world = location.getWorld();
        return new PlayerSnapshot(
            player.getUniqueId(),
            world == null ? new UUID(0L, 0L) : world.getUID(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            player.getName(),
            player.getGameMode(),
            locatorEnabled
        );
    }
}
