package com.example.locatebarfolia.command;

import com.example.locatebarfolia.LocateBarPlugin;
import com.example.locatebarfolia.service.LocateBarService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
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
                sender.sendMessage(ChatColor.RED + "Console must specify reload.");
                return true;
            }
            if (!sender.hasPermission("locatebar.use")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use LocateBarFolia.");
                return true;
            }

            this.locateBarService.toggle(player);
            sender.sendMessage(ChatColor.YELLOW + "LocateBar participation is now " + stateWord(this.locateBarService.isEnabledFor(player)) + ChatColor.YELLOW + ".");
            return true;
        }

        final String subcommand = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("reload".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload LocateBarFolia.");
                return true;
            }

            this.plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "LocateBarFolia configuration reloaded.");
            return true;
        }

        if ("radius".equals(subcommand)) {
            if (!sender.hasPermission("locatebar.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to change LocateBarFolia radius.");
                return true;
            }

            if (args.length == 1) {
                sender.sendMessage(ChatColor.YELLOW + "Current LocateBar radius: " + ChatColor.GREEN + formatRadius(this.locateBarService.scanRadius()));
                return true;
            }

            final double radius;
            try {
                radius = Double.parseDouble(args[1]);
            } catch (final NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Radius must be a number.");
                return true;
            }

            final var config = this.plugin.updateScanRadius(radius);
            sender.sendMessage(ChatColor.GREEN + "LocateBar radius updated to " + formatRadius(config.scanRadius()) + " blocks.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use that subcommand.");
            return true;
        }
        if (!sender.hasPermission("locatebar.use")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use LocateBarFolia.");
            return true;
        }

        switch (subcommand) {
            case "on" -> this.locateBarService.setEnabled(player, true);
            case "off" -> this.locateBarService.setEnabled(player, false);
            case "toggle" -> this.locateBarService.toggle(player);
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <on|off|toggle|radius|reload>");
                return true;
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "LocateBar participation is now " + stateWord(this.locateBarService.isEnabledFor(player)) + ChatColor.YELLOW + ".");
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

    private String stateWord(final boolean enabled) {
        return enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
    }

    private String formatRadius(final double radius) {
        if (radius == Math.rint(radius)) {
            return Long.toString((long) radius);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", radius);
    }
}
