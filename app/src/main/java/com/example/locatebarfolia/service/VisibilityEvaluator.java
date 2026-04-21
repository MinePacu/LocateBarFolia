package com.example.locatebarfolia.service;

import com.example.locatebarfolia.config.LocateBarConfig;
import com.example.locatebarfolia.model.PlayerSnapshot;
import org.bukkit.GameMode;

public final class VisibilityEvaluator {
    public boolean shouldTrack(
        final PlayerSnapshot recipient,
        final PlayerSnapshot target,
        final LocateBarConfig config
    ) {
        if (recipient == null || target == null) {
            return false;
        }
        if (!recipient.locatorEnabled() || !target.locatorEnabled()) {
            return false;
        }
        if (recipient.playerId().equals(target.playerId())) {
            return false;
        }
        if (!recipient.worldId().equals(target.worldId())) {
            return false;
        }
        if (config.ignoreSpectators()
            && (recipient.gameMode() == GameMode.SPECTATOR || target.gameMode() == GameMode.SPECTATOR)) {
            return false;
        }

        final double deltaX = target.x() - recipient.x();
        final double deltaY = target.y() - recipient.y();
        final double deltaZ = target.z() - recipient.z();
        final double distanceSquared = (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
        return distanceSquared <= config.scanRadiusSquared();
    }
}
