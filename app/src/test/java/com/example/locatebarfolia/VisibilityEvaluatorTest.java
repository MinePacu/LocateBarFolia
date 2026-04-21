package com.example.locatebarfolia;

import com.example.locatebarfolia.config.LocateBarConfig;
import com.example.locatebarfolia.model.PlayerSnapshot;
import com.example.locatebarfolia.service.VisibilityEvaluator;
import java.util.UUID;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibilityEvaluatorTest {
    private static final UUID WORLD = UUID.randomUUID();
    private static final LocateBarConfig CONFIG = new LocateBarConfig(true, 48.0D, 2304.0D, 0.5D, true, false);
    private final VisibilityEvaluator evaluator = new VisibilityEvaluator();

    @Test
    void tracksPlayersInSameWorldAndRadius() {
        assertTrue(this.evaluator.shouldTrack(
            snapshot(UUID.randomUUID(), WORLD, 0.0D, 64.0D, 0.0D, true),
            snapshot(UUID.randomUUID(), WORLD, 10.0D, 64.0D, 0.0D, true),
            CONFIG
        ));
    }

    @Test
    void rejectsOtherWorlds() {
        assertFalse(this.evaluator.shouldTrack(
            snapshot(UUID.randomUUID(), WORLD, 0.0D, 64.0D, 0.0D, true),
            snapshot(UUID.randomUUID(), UUID.randomUUID(), 10.0D, 64.0D, 0.0D, true),
            CONFIG
        ));
    }

    @Test
    void rejectsDisabledTargets() {
        assertFalse(this.evaluator.shouldTrack(
            snapshot(UUID.randomUUID(), WORLD, 0.0D, 64.0D, 0.0D, true),
            snapshot(UUID.randomUUID(), WORLD, 10.0D, 64.0D, 0.0D, false),
            CONFIG
        ));
    }

    @Test
    void rejectsSpectatorsWhenConfigured() {
        assertFalse(this.evaluator.shouldTrack(
            snapshot(UUID.randomUUID(), WORLD, 0.0D, 64.0D, 0.0D, true),
            new PlayerSnapshot(UUID.randomUUID(), WORLD, 10.0D, 64.0D, 0.0D, GameMode.SPECTATOR, true),
            CONFIG
        ));
    }

    private static PlayerSnapshot snapshot(
        final UUID playerId,
        final UUID worldId,
        final double x,
        final double y,
        final double z,
        final boolean enabled
    ) {
        return new PlayerSnapshot(playerId, worldId, x, y, z, GameMode.SURVIVAL, enabled);
    }
}
