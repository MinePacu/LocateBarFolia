package com.example.locatebarfolia.listener;

import com.example.locatebarfolia.service.LocateBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMovementListener implements Listener {
    private final LocateBarService locateBarService;

    public PlayerMovementListener(final LocateBarService locateBarService) {
        this.locateBarService = locateBarService;
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        this.locateBarService.handleMovement(event.getPlayer(), event.getFrom(), event.getTo());
    }
}
