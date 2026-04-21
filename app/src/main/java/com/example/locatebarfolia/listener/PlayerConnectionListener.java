package com.example.locatebarfolia.listener;

import com.example.locatebarfolia.service.LocateBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {
    private final LocateBarService locateBarService;

    public PlayerConnectionListener(final LocateBarService locateBarService) {
        this.locateBarService = locateBarService;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        this.locateBarService.track(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.locateBarService.untrack(event.getPlayer().getUniqueId());
    }
}
