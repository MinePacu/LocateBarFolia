package com.example.locatebarfolia.listener;

import com.example.locatebarfolia.service.LocateBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerWorldListener implements Listener {
    private final LocateBarService locateBarService;

    public PlayerWorldListener(final LocateBarService locateBarService) {
        this.locateBarService = locateBarService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        this.locateBarService.handleStateRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        this.locateBarService.handleStateRefresh(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event) {
        this.locateBarService.handleStateRefresh(event.getPlayer(), event.getRespawnLocation());
    }
}
