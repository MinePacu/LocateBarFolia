package com.example.locatebarfolia.config;

import org.bukkit.configuration.file.FileConfiguration;

public record LocateBarConfig(
    boolean enabledByDefault,
    double scanRadius,
    double scanRadiusSquared,
    double movementThresholdBlocks,
    boolean ignoreSpectators,
    boolean ignoreVanished,
    boolean bedrockFallbackEnabled,
    long bedrockActionbarIntervalTicks,
    int bedrockMaxTargets
) {
    private static final double DEFAULT_SCAN_RADIUS = 48.0D;
    private static final double MIN_SCAN_RADIUS = 8.0D;

    public static LocateBarConfig from(final FileConfiguration config) {
        final double scanRadius = sanitizeScanRadius(config.getDouble("scan-radius", DEFAULT_SCAN_RADIUS));
        final double movementThreshold = Math.max(0.05D, config.getDouble("movement-threshold-blocks", 0.5D));

        return new LocateBarConfig(
            config.getBoolean("enabled-by-default", true),
            scanRadius,
            scanRadius * scanRadius,
            movementThreshold,
            config.getBoolean("ignore-spectators", true),
            config.getBoolean("ignore-vanished", false),
            config.getBoolean("bedrock-fallback.enabled", true),
            Math.max(10L, config.getLong("bedrock-fallback.actionbar-interval-ticks", 20L)),
            Math.max(1, config.getInt("bedrock-fallback.max-targets", 4))
        );
    }

    public LocateBarConfig withScanRadius(final double newScanRadius) {
        final double sanitizedRadius = sanitizeScanRadius(newScanRadius);
        return new LocateBarConfig(
            this.enabledByDefault,
            sanitizedRadius,
            sanitizedRadius * sanitizedRadius,
            this.movementThresholdBlocks,
            this.ignoreSpectators,
            this.ignoreVanished,
            this.bedrockFallbackEnabled,
            this.bedrockActionbarIntervalTicks,
            this.bedrockMaxTargets
        );
    }

    public static double sanitizeScanRadius(final double scanRadius) {
        if (!Double.isFinite(scanRadius)) {
            return DEFAULT_SCAN_RADIUS;
        }
        return Math.max(MIN_SCAN_RADIUS, scanRadius);
    }
}
