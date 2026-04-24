package com.example.locatebarfolia;

import com.example.locatebarfolia.model.PlayerSnapshot;
import com.example.locatebarfolia.service.BedrockActionBarRenderer;
import java.util.List;
import java.util.UUID;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockActionBarRendererTest {
    private static final UUID WORLD = UUID.randomUUID();
    private final BedrockActionBarRenderer renderer = new BedrockActionBarRenderer();

    @Test
    void rendersEmptyWhenNoTargetsExist() {
        assertEquals("", this.renderer.render(snapshot("Viewer", 0.0D, 0.0D, 0.0F), List.of(), 4));
    }

    @Test
    void rendersNearestTargetsWithDirectionAndDistance() {
        final PlayerSnapshot viewer = snapshot("Viewer", 0.0D, 0.0D, 0.0F);
        final PlayerSnapshot front = snapshot("Alex", 0.0D, 10.0D, 0.0F);
        final PlayerSnapshot left = snapshot("Steve", 20.0D, 0.0D, 0.0F);

        assertEquals("Locate: F Alex 10m | L Steve 20m", this.renderer.render(viewer, List.of(left, front), 4));
    }

    @Test
    void respectsTargetLimit() {
        final PlayerSnapshot viewer = snapshot("Viewer", 0.0D, 0.0D, 0.0F);
        assertEquals(
            "Locate: F Alex 10m",
            this.renderer.render(viewer, List.of(snapshot("Alex", 0.0D, 10.0D, 0.0F), snapshot("Steve", 0.0D, 20.0D, 0.0F)), 1)
        );
    }

    @Test
    void rendersKoreanPrefixForKoreanRecipients() {
        final PlayerSnapshot viewer = snapshot("Viewer", 0.0D, 0.0D, 0.0F, "ko_kr");

        assertEquals("추적: F Alex 10m", this.renderer.render(viewer, List.of(snapshot("Alex", 0.0D, 10.0D, 0.0F)), 1));
    }

    private static PlayerSnapshot snapshot(final String name, final double x, final double z, final float yaw) {
        return snapshot(name, x, z, yaw, "en_us");
    }

    private static PlayerSnapshot snapshot(final String name, final double x, final double z, final float yaw, final String localeTag) {
        return new PlayerSnapshot(UUID.randomUUID(), WORLD, x, 64.0D, z, yaw, localeTag, 0xFFFFFF, name, GameMode.SURVIVAL, true);
    }
}
