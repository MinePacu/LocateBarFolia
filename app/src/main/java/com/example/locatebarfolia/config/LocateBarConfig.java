package com.example.locatebarfolia.config;

import org.bukkit.configuration.file.FileConfiguration;

public record LocateBarConfig(
    boolean enabledByDefault,
    double scanRadius,
    double scanRadiusSquared,
    double movementThresholdBlocks,
    boolean ignoreSpectators,
    boolean ignoreVanished
) {
    private static final double DEFAULT_SCAN_RADIUS = 48.0D;

    public static LocateBarConfig from(final FileConfiguration config) {
        final double scanRadius = Math.max(8.0D, config.getDouble("scan-radius", DEFAULT_SCAN_RADIUS));
        final double movementThreshold = Math.max(0.05D, config.getDouble("movement-threshold-blocks", 0.5D));

        return new LocateBarConfig(
            config.getBoolean("enabled-by-default", true),
            scanRadius,
            scanRadius * scanRadius,
            movementThreshold,
            config.getBoolean("ignore-spectators", true),
            config.getBoolean("ignore-vanished", false)
        );
    }
}
