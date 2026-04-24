package com.example.locatebarfolia.command;

import com.example.locatebarfolia.i18n.DisplayLanguage;
import com.example.locatebarfolia.LocateBarPlugin;
import com.example.locatebarfolia.service.LocateBarService;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class LocateBarCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("on", "off", "toggle", "color", "radius", "reload");

    private final LocateBarPlugin plugin;
    private final LocateBarService locateBarService;

    public LocateBarCommand(final LocateBarPlugin plugin, final LocateBarService locateBarService) {
        this.plugin = plugin;
        this.locateBarService = locateBarService;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendError(sender, text(sender, "콘솔에서는 reload를 명시해서 사용해야 합니다.", "Console must specify reload."));
                return true;
            }

            this.locateBarService.toggle(player, enabled -> sendParticipationState(player, enabled));
            return true;
        }

        final String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("reload".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sendError(sender, text(sender, "LocateBarFolia를 다시 불러올 권한이 없습니다.", "You do not have permission to reload LocateBarFolia."));
                return true;
            }

            this.plugin.reloadPluginConfig();
            sendSuccess(sender, text(sender, "LocateBarFolia 설정을 다시 불러왔습니다.", "LocateBarFolia configuration reloaded."));
            return true;
        }

        if ("radius".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sendError(sender, text(sender, "LocateBarFolia 반경을 변경할 권한이 없습니다.", "You do not have permission to change LocateBarFolia radius."));
                return true;
            }

            if (args.length == 1) {
                sender.sendMessage(Component.text(text(sender, "현재 LocateBar 반경: ", "Current LocateBar radius: "), NamedTextColor.YELLOW)
                    .append(Component.text(formatRadius(this.locateBarService.scanRadius()), NamedTextColor.GREEN)));
                return true;
            }

            final double radius;
            try {
                radius = Double.parseDouble(args[1]);
            } catch (final NumberFormatException ignored) {
                sendError(sender, text(sender, "반경은 숫자로 입력해야 합니다.", "Radius must be a number."));
                return true;
            }

            final var config = this.plugin.updateScanRadius(radius);
            sendSuccess(
                sender,
                text(
                    sender,
                    "LocateBar 반경을 " + formatRadius(config.scanRadius()) + "블록으로 변경했습니다.",
                    "LocateBar radius updated to " + formatRadius(config.scanRadius()) + " blocks."
                )
            );
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendError(sender, text(sender, "이 하위 명령어는 플레이어만 사용할 수 있습니다.", "Only players can use that subcommand."));
            return true;
        }

        switch (subcommand) {
            case "on" -> this.locateBarService.setEnabled(player, true, enabled -> sendParticipationState(player, enabled));
            case "off" -> this.locateBarService.setEnabled(player, false, enabled -> sendParticipationState(player, enabled));
            case "toggle" -> this.locateBarService.toggle(player, enabled -> sendParticipationState(player, enabled));
            case "color" -> handleColorCommand(player, label, args);
            default -> {
                sendError(sender, text(sender, "사용법: /" + label + " <on|off|toggle|color|radius|reload>", "Usage: /" + label + " <on|off|toggle|color|radius|reload>"));
                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 2 && "color".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            return List.of("#FF5555", "#55FF55", "#5555FF", "#FFFF55", "reset");
        }
        if (args.length == 2 && "radius".equalsIgnoreCase(args[0]) && sender.hasPermission("locatebar.admin")) {
            return List.of("16", "32", "48", "64", "96");
        }
        if (args.length != 1) {
            return List.of();
        }

        final String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
        final List<String> matches = new ArrayList<>();
        for (final String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix)) {
                matches.add(subcommand);
            }
        }
        return matches;
    }

    private void sendParticipationState(final CommandSender sender, final boolean enabled) {
        final DisplayLanguage language = languageOf(sender);
        sender.sendMessage(Component.text(text(language, "LocateBar 표시가 ", "LocateBar display is now "), NamedTextColor.YELLOW)
            .append(Component.text(text(language, enabled ? "활성화" : "비활성화", enabled ? "enabled" : "disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
            .append(Component.text(text(language, "되었습니다.", "."), NamedTextColor.YELLOW)));
    }

    private void handleColorCommand(final Player player, final String label, final String[] args) {
        final DisplayLanguage language = languageOf(player);
        if (args.length == 1) {
            player.sendMessage(colorMessage(language, "현재 웨이포인트 색상은 ", "Your waypoint color is ", this.locateBarService.waypointColorFor(player), NamedTextColor.GREEN));
            return;
        }

        if ("reset".equalsIgnoreCase(args[1])) {
            this.locateBarService.resetWaypointColor(player, color ->
                player.sendMessage(colorMessage(language, "웨이포인트 색상을 기본값으로 되돌렸습니다: ", "Your waypoint color has been reset to ", color, NamedTextColor.GREEN))
            );
            return;
        }

        final Integer color = parseColor(args[1]);
        if (color == null) {
            sendError(player, text(language, "사용법: /" + label + " color <#RRGGBB|RRGGBB|reset>", "Usage: /" + label + " color <#RRGGBB|RRGGBB|reset>"));
            return;
        }

        this.locateBarService.setWaypointColor(
            player,
            color,
            appliedColor -> player.sendMessage(colorMessage(language, "웨이포인트 색상을 변경했습니다: ", "Your waypoint color is now ", appliedColor, NamedTextColor.GREEN)),
            duplicateColor -> player.sendMessage(colorMessage(language, "이미 다른 온라인 플레이어가 사용 중인 색상입니다: ", "That waypoint color is already in use by another online player: ", duplicateColor, NamedTextColor.RED))
        );
    }

    private void sendError(final CommandSender sender, final String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private void sendSuccess(final CommandSender sender, final String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private String formatRadius(final double radius) {
        if (radius == Math.rint(radius)) {
            return Long.toString((long) radius);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", radius);
    }

    private Integer parseColor(final String input) {
        final String normalized = input.startsWith("#") ? input.substring(1) : input;
        if (normalized.length() != 6 || !normalized.chars().allMatch(this::isHexDigit)) {
            return null;
        }

        try {
            return Integer.parseInt(normalized, 16);
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isHexDigit(final int value) {
        return value >= '0' && value <= '9'
            || value >= 'a' && value <= 'f'
            || value >= 'A' && value <= 'F';
    }

    private String formatColor(final int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private Component colorMessage(
        final DisplayLanguage language,
        final String koreanPrefix,
        final String englishPrefix,
        final int color,
        final NamedTextColor accentColor
    ) {
        return Component.text(text(language, koreanPrefix, englishPrefix), accentColor)
            .append(Component.text(formatColor(color), TextColor.color(color)))
            .append(Component.text(" (" + describeColor(language, color) + ") ", accentColor))
            .append(Component.text("\u25A0", TextColor.color(color)))
            .append(Component.text(".", accentColor));
    }

    private String describeColor(final DisplayLanguage language, final int color) {
        final int red = (color >>> 16) & 0xFF;
        final int green = (color >>> 8) & 0xFF;
        final int blue = color & 0xFF;

        final float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        final float hue = hsb[0] * 360.0F;
        final float saturation = hsb[1];
        final float brightness = hsb[2];

        if (brightness <= 0.12F) {
            return text(language, "거의 검정", "near black");
        }
        if (brightness >= 0.95F && saturation <= 0.10F) {
            return text(language, "거의 흰색", "near white");
        }
        if (saturation <= 0.15F) {
            if (brightness < 0.33F) {
                return text(language, "짙은 회색", "dark gray");
            }
            if (brightness < 0.75F) {
                return text(language, "회색", "gray");
            }
            return text(language, "밝은 회색", "light gray");
        }

        final String tone;
        if (brightness < 0.35F) {
            tone = text(language, "어두운 ", "dark ");
        } else if (brightness > 0.85F && saturation < 0.45F) {
            tone = text(language, "부드러운 ", "soft ");
        } else if (brightness > 0.85F && saturation > 0.70F) {
            tone = text(language, "선명한 ", "bright ");
        } else {
            tone = "";
        }

        return tone + hueName(language, hue);
    }

    private String hueName(final DisplayLanguage language, final float hue) {
        if (hue < 15.0F || hue >= 345.0F) {
            return text(language, "빨강", "red");
        }
        if (hue < 45.0F) {
            return text(language, "주황", "orange");
        }
        if (hue < 70.0F) {
            return text(language, "노랑", "yellow");
        }
        if (hue < 150.0F) {
            return text(language, "초록", "green");
        }
        if (hue < 195.0F) {
            return text(language, "청록", "cyan");
        }
        if (hue < 255.0F) {
            return text(language, "파랑", "blue");
        }
        if (hue < 290.0F) {
            return text(language, "보라", "purple");
        }
        if (hue < 345.0F) {
            return text(language, "자홍", "magenta");
        }
        return text(language, "빨강", "red");
    }

    private DisplayLanguage languageOf(final CommandSender sender) {
        if (sender instanceof Player player) {
            return DisplayLanguage.fromLocaleTag(player.locale().toLanguageTag());
        }
        return DisplayLanguage.ENGLISH;
    }

    private String text(final CommandSender sender, final String korean, final String english) {
        return text(languageOf(sender), korean, english);
    }

    private String text(final DisplayLanguage language, final String korean, final String english) {
        return language == DisplayLanguage.KOREAN ? korean : english;
    }
}
