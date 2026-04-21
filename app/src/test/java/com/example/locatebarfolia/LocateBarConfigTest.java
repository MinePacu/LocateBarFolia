package com.example.locatebarfolia;

import com.example.locatebarfolia.config.LocateBarConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocateBarConfigTest {
    @Test
    void clampsTooSmallScanRadius() {
        assertEquals(8.0D, LocateBarConfig.sanitizeScanRadius(2.0D));
    }

    @Test
    void replacesInvalidScanRadiusWithDefault() {
        assertEquals(48.0D, LocateBarConfig.sanitizeScanRadius(Double.NaN));
    }

    @Test
    void withScanRadiusKeepsOtherSettings() {
        final LocateBarConfig original = new LocateBarConfig(true, 48.0D, 2304.0D, 0.5D, true, false, true, 20L, 4);
        final LocateBarConfig updated = original.withScanRadius(64.0D);

        assertEquals(64.0D, updated.scanRadius());
        assertEquals(4096.0D, updated.scanRadiusSquared());
        assertEquals(original.enabledByDefault(), updated.enabledByDefault());
        assertEquals(original.movementThresholdBlocks(), updated.movementThresholdBlocks());
        assertEquals(original.ignoreSpectators(), updated.ignoreSpectators());
        assertEquals(original.ignoreVanished(), updated.ignoreVanished());
        assertEquals(original.bedrockFallbackEnabled(), updated.bedrockFallbackEnabled());
        assertEquals(original.bedrockActionbarIntervalTicks(), updated.bedrockActionbarIntervalTicks());
        assertEquals(original.bedrockMaxTargets(), updated.bedrockMaxTargets());
    }
}
