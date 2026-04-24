package com.example.locatebarfolia;

import com.example.locatebarfolia.service.WaypointColorResolver;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WaypointColorResolverTest {
    @Test
    void returnsStableColorForSamePlayer() {
        final UUID playerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        assertEquals(WaypointColorResolver.colorFor(playerId), WaypointColorResolver.colorFor(playerId));
    }

    @Test
    void returnsDifferentColorsForDifferentPlayers() {
        assertNotEquals(
            WaypointColorResolver.colorFor(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
            WaypointColorResolver.colorFor(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
        );
    }
}
