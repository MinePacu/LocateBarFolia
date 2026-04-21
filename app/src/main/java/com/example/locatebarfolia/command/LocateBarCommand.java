package com.example.locatebarfolia.command;

import com.example.locatebarfolia.LocateBarPlugin;
import com.example.locatebarfolia.service.LocateBarService;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class LocateBarCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("on", "off", "toggle", "radius", "reload");

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
                sendError(sender, "Console must specify reload.");
                return true;
            }
            if (!sender.hasPermission("locatebar.use")) {
                sendError(sender, "You do not have permission to use LocateBarFolia.");
                return true;
            }

            this.locateBarService.toggle(player);
            sendParticipationState(sender, this.locateBarService.isEnabledFor(player));
            return true;
        }

        final String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("reload".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sendError(sender, "You do not have permission to reload LocateBarFolia.");
                return true;
            }

            this.plugin.reloadPluginConfig();
            sendSuccess(sender, "LocateBarFolia configuration reloaded.");
            return true;
        }

        if ("radius".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sendError(sender, "You do not have permission to change LocateBarFolia radius.");
                return true;
            }

            if (args.length == 1) {
                sender.sendMessage(Component.text("Current LocateBar radius: ", NamedTextColor.YELLOW)
                    .append(Component.text(formatRadius(this.locateBarService.scanRadius()), NamedTextColor.GREEN)));
                return true;
            }

            final double radius;
            try {
                radius = Double.parseDouble(args[1]);
            } catch (final NumberFormatException ignored) {
                sendError(sender, "Radius must be a number.");
                return true;
            }

            final var config = this.plugin.updateScanRadius(radius);
            sendSuccess(sender, "LocateBar radius updated to " + formatRadius(config.scanRadius()) + " blocks.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendError(sender, "Only players can use that subcommand.");
            return true;
        }
        if (!sender.hasPermission("locatebar.use")) {
            sendError(sender, "You do not have permission to use LocateBarFolia.");
            return true;
        }

        switch (subcommand) {
            case "on" -> this.locateBarService.setEnabled(player, true);
            case "off" -> this.locateBarService.setEnabled(player, false);
            case "toggle" -> this.locateBarService.toggle(player);
            default -> {
                sendError(sender, "Usage: /" + label + " <on|off|toggle|radius|reload>");
                return true;
            }
        }

        sendParticipationState(sender, this.locateBarService.isEnabledFor(player));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
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
        sender.sendMessage(Component.text("LocateBar participation is now ", NamedTextColor.YELLOW)
            .append(Component.text(enabled ? "enabled" : "disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
            .append(Component.text(".", NamedTextColor.YELLOW)));
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
}
