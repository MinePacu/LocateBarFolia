package com.example.locatebarfolia.service;

import com.example.locatebarfolia.i18n.DisplayLanguage;
import com.example.locatebarfolia.model.PlayerSnapshot;
import java.util.Comparator;
import java.util.List;

public final class BedrockActionBarRenderer {
    public String render(final PlayerSnapshot recipient, final List<PlayerSnapshot> targets, final int maxTargets) {
        if (recipient == null || targets.isEmpty()) {
            return "";
        }

        return targets.stream()
            .sorted(Comparator.comparingDouble(target -> horizontalDistanceSquared(recipient, target)))
            .limit(maxTargets)
            .map(target -> formatTarget(recipient, target))
            .reduce((left, right) -> left + " | " + right)
            .map(message -> actionBarPrefix(recipient.localeTag()) + message)
            .orElse("");
    }

    private String actionBarPrefix(final String localeTag) {
        return DisplayLanguage.fromLocaleTag(localeTag) == DisplayLanguage.KOREAN ? "추적: " : "Locate: ";
    }

    private String formatTarget(final PlayerSnapshot recipient, final PlayerSnapshot target) {
        final double deltaX = target.x() - recipient.x();
        final double deltaZ = target.z() - recipient.z();
        final long distance = Math.round(Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ)));
        return directionCode(deltaX, deltaZ, recipient.yaw()) + " " + target.name() + " " + distance + "m";
    }

    private double horizontalDistanceSquared(final PlayerSnapshot recipient, final PlayerSnapshot target) {
        final double deltaX = target.x() - recipient.x();
        final double deltaZ = target.z() - recipient.z();
        return (deltaX * deltaX) + (deltaZ * deltaZ);
    }

    static String directionCode(final double deltaX, final double deltaZ, final float yawDegrees) {
        final double radians = Math.toRadians(yawDegrees);
        final double sin = Math.sin(radians);
        final double cos = Math.cos(radians);
        final double localX = (-deltaX * cos) - (deltaZ * sin);
        final double localZ = (-deltaX * sin) + (deltaZ * cos);
        final double angle = Math.atan2(localX, localZ);
        final int sector = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8);

        return switch (sector) {
            case 0 -> "F";
            case 1 -> "FR";
            case 2 -> "R";
            case 3 -> "BR";
            case 4 -> "B";
            case 5 -> "BL";
            case 6 -> "L";
            case 7 -> "FL";
            default -> "?";
        };
    }
}
