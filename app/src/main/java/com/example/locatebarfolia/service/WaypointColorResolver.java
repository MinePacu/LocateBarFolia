package com.example.locatebarfolia.service;

import java.awt.Color;
import java.util.UUID;

public final class WaypointColorResolver {
    private WaypointColorResolver() {
    }

    public static int colorFor(final UUID playerId) {
        final long mixed = mix(playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits());
        final float hue = (float) ((mixed & 0xFFFFFFFFL) / (double) 0x1_0000_0000L);
        final float saturation = 0.68F + (float) (((mixed >>> 32) & 0x0FL) / 100.0D);
        final float brightness = 0.86F + (float) (((mixed >>> 40) & 0x07L) / 100.0D);
        return Color.HSBtoRGB(hue, Math.min(saturation, 0.80F), Math.min(brightness, 0.92F)) & 0xFFFFFF;
    }

    private static long mix(final long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
