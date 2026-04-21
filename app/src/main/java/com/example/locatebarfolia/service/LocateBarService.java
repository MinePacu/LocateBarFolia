package com.example.locatebarfolia.service;

import com.example.locatebarfolia.config.LocateBarConfig;
import com.example.locatebarfolia.model.PlayerSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocateBarService {
    private final JavaPlugin plugin;
    private final PlayerStateRegistry registry;
    private final VisibilityEvaluator visibilityEvaluator;
    private final WaypointPacketSender packetSender;
    private volatile LocateBarConfig config;

    public LocateBarService(
        final JavaPlugin plugin,
        final LocateBarConfig config,
        final PlayerStateRegistry registry,
        final VisibilityEvaluator visibilityEvaluator,
        final WaypointPacketSender packetSender
    ) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.visibilityEvaluator = visibilityEvaluator;
        this.packetSender = packetSender;
    }

    public void reload(final LocateBarConfig newConfig) {
        this.config = newConfig;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
            state.visibleTargetIds().clear();
            state.setEnabled(state.isEnabled());
            this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            syncRecipient(player, true);
        }
    }

    public void track(final Player player) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        state.setEnabled(this.config.enabledByDefault());
        state.visibleTargetIds().clear();
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    public void untrack(final UUID playerId) {
        final PlayerStateRegistry.PlayerRuntimeState removedState = this.registry.removeState(playerId);
        if (removedState != null) {
            removedState.visibleTargetIds().clear();
        }
        removeTargetFromAllRecipients(playerId);
    }

    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            clearRecipientWaypoints(player);
        }
    }

    public boolean isEnabledFor(final Player player) {
        return this.registry.stateFor(player.getUniqueId()).isEnabled();
    }

    public void setEnabled(final Player player, final boolean enabled) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        state.setEnabled(enabled);
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, enabled));

        if (!enabled) {
            clearRecipientWaypoints(player);
            removeTargetFromAllRecipients(player.getUniqueId());
            return;
        }

        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    public void toggle(final Player player) {
        setEnabled(player, !isEnabledFor(player));
    }

    public void handleMovement(final Player player, final Location from, final Location to) {
        if (to == null) {
            return;
        }

        final double threshold = this.config.movementThresholdBlocks();
        if (Math.abs(to.getX() - from.getX()) < threshold
            && Math.abs(to.getY() - from.getY()) < threshold
            && Math.abs(to.getZ() - from.getZ()) < threshold
            && Math.abs(to.getYaw() - from.getYaw()) < 2.0F
            && Math.abs(to.getPitch() - from.getPitch()) < 2.0F) {
            return;
        }

        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        if (!state.isEnabled()) {
            return;
        }

        refreshTargetForAllRecipients(player.getUniqueId());
        syncRecipient(player, false);
    }

    public void handleStateRefresh(final Player player) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    private void syncRecipient(final Player recipient, final boolean fullRefresh) {
        recipient.getScheduler().run(this.plugin, scheduledTask -> {
            final PlayerStateRegistry.PlayerRuntimeState recipientState = this.registry.stateFor(recipient.getUniqueId());
            final Set<UUID> visibleTargets = recipientState.visibleTargetIds();
            final PlayerSnapshot recipientSnapshot = this.registry.snapshotFor(recipient.getUniqueId());

            if (!recipientState.isEnabled()) {
                if (!visibleTargets.isEmpty()) {
                    this.packetSender.removeWaypoints(recipient, List.copyOf(visibleTargets));
                    visibleTargets.clear();
                }
                return;
            }

            if (fullRefresh && !visibleTargets.isEmpty()) {
                this.packetSender.removeWaypoints(recipient, List.copyOf(visibleTargets));
                visibleTargets.clear();
            }

            for (final PlayerSnapshot targetSnapshot : this.registry.snapshots()) {
                syncRecipientTarget(recipient, recipientSnapshot, recipientState, targetSnapshot, true);
            }
        }, null);
    }

    private void refreshTargetForAllRecipients(final UUID targetId) {
        final PlayerSnapshot targetSnapshot = this.registry.snapshotFor(targetId);
        if (targetSnapshot == null) {
            return;
        }

        for (final Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.getUniqueId().equals(targetId)) {
                continue;
            }

            recipient.getScheduler().run(this.plugin, scheduledTask -> {
                final PlayerSnapshot recipientSnapshot = this.registry.snapshotFor(recipient.getUniqueId());
                final PlayerStateRegistry.PlayerRuntimeState recipientState = this.registry.stateFor(recipient.getUniqueId());
                syncRecipientTarget(recipient, recipientSnapshot, recipientState, targetSnapshot, false);
            }, null);
        }
    }

    private void syncRecipientTarget(
        final Player recipient,
        final PlayerSnapshot recipientSnapshot,
        final PlayerStateRegistry.PlayerRuntimeState recipientState,
        final PlayerSnapshot targetSnapshot,
        final boolean allowUpsertWhenVisible
    ) {
        if (targetSnapshot == null) {
            return;
        }

        final UUID targetId = targetSnapshot.playerId();
        final Set<UUID> visibleTargets = recipientState.visibleTargetIds();
        final boolean shouldTrack = this.visibilityEvaluator.shouldTrack(recipientSnapshot, targetSnapshot, this.config)
            && isRecipientAllowedToSee(recipient, targetId);

        if (shouldTrack) {
            if (allowUpsertWhenVisible || !visibleTargets.contains(targetId)) {
                this.packetSender.upsertWaypoint(recipient, targetId, targetSnapshot);
            }
            visibleTargets.add(targetId);
        } else if (visibleTargets.remove(targetId)) {
            this.packetSender.removeWaypoint(recipient, targetId);
        }
    }

    private boolean isRecipientAllowedToSee(final Player recipient, final UUID targetId) {
        if (!this.config.ignoreVanished()) {
            return true;
        }

        final Player target = Bukkit.getPlayer(targetId);
        return target == null || recipient.canSee(target);
    }

    private void removeTargetFromAllRecipients(final UUID targetId) {
        for (final Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.getScheduler().run(this.plugin, scheduledTask -> {
                final PlayerStateRegistry.PlayerRuntimeState recipientState = this.registry.stateFor(recipient.getUniqueId());
                if (recipientState.visibleTargetIds().remove(targetId)) {
                    this.packetSender.removeWaypoint(recipient, targetId);
                }
            }, null);
        }
    }

    private void clearRecipientWaypoints(final Player recipient) {
        recipient.getScheduler().run(this.plugin, scheduledTask -> {
            final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(recipient.getUniqueId());
            final List<UUID> visible = new ArrayList<>(state.visibleTargetIds());
            if (!visible.isEmpty()) {
                this.packetSender.removeWaypoints(recipient, visible);
                state.visibleTargetIds().clear();
            }
        }, null);
    }
}
