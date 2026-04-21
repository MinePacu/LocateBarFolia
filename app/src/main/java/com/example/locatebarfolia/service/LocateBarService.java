package com.example.locatebarfolia.service;

import com.example.locatebarfolia.config.LocateBarConfig;
import com.example.locatebarfolia.model.PlayerSnapshot;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocateBarService {
    private static final String ENABLED_KEY_NAME = "enabled";

    private final JavaPlugin plugin;
    private final PlayerStateRegistry registry;
    private final VisibilityEvaluator visibilityEvaluator;
    private final WaypointPacketSender packetSender;
    private final BedrockCompatibility bedrockCompatibility;
    private final BedrockActionBarRenderer bedrockActionBarRenderer;
    private final NamespacedKey enabledKey;
    private volatile LocateBarConfig config;

    public LocateBarService(
        final JavaPlugin plugin,
        final LocateBarConfig config,
        final PlayerStateRegistry registry,
        final VisibilityEvaluator visibilityEvaluator,
        final WaypointPacketSender packetSender,
        final BedrockCompatibility bedrockCompatibility,
        final BedrockActionBarRenderer bedrockActionBarRenderer
    ) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.visibilityEvaluator = visibilityEvaluator;
        this.packetSender = packetSender;
        this.bedrockCompatibility = bedrockCompatibility;
        this.bedrockActionBarRenderer = bedrockActionBarRenderer;
        this.enabledKey = new NamespacedKey(plugin, ENABLED_KEY_NAME);
    }

    public void reload(final LocateBarConfig newConfig) {
        this.config = newConfig;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(this.plugin, scheduledTask -> {
                final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
                state.setBedrockClient(isBedrockFallbackRecipient(player));
                this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
                cancelBedrockActionBarTask(state);
                syncBedrockActionBarTask(player, state);
                syncRecipient(player, true);
            }, null);
        }
    }

    public void track(final Player player) {
        player.getScheduler().runDelayed(this.plugin, scheduledTask -> trackOnPlayerScheduler(player), null, 1L);
    }

    private void trackOnPlayerScheduler(final Player player) {
        if (!player.isOnline()) {
            return;
        }

        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        state.setEnabled(loadEnabled(player));
        state.setBedrockClient(isBedrockFallbackRecipient(player));
        state.visibleTargetIds().clear();
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        syncBedrockActionBarTask(player, state);
        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    public void untrack(final UUID playerId) {
        final PlayerStateRegistry.PlayerRuntimeState removedState = this.registry.removeState(playerId);
        if (removedState != null) {
            cancelBedrockActionBarTask(removedState);
            removedState.visibleTargetIds().clear();
        }
        removeTargetFromAllRecipients(playerId);
    }

    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            clearRecipientWaypoints(player);
        }
        for (final var entry : this.registry.states()) {
            cancelBedrockActionBarTask(entry.getValue());
        }
    }

    public boolean isEnabledFor(final Player player) {
        return this.registry.stateFor(player.getUniqueId()).isEnabled();
    }

    public void setEnabled(final Player player, final boolean enabled) {
        setEnabled(player, enabled, ignored -> {
        });
    }

    public void setEnabled(final Player player, final boolean enabled, final Consumer<Boolean> completion) {
        player.getScheduler().run(this.plugin, scheduledTask -> {
            applyEnabled(player, enabled);
            completion.accept(enabled);
        }, null);
    }

    private void applyEnabled(final Player player, final boolean enabled) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        state.setEnabled(enabled);
        state.setBedrockClient(isBedrockFallbackRecipient(player));
        saveEnabled(player, enabled);
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, enabled));
        syncBedrockActionBarTask(player, state);

        if (!enabled) {
            clearRecipientWaypoints(player);
            return;
        }

        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    public void toggle(final Player player) {
        toggle(player, ignored -> {
        });
    }

    public void toggle(final Player player, final Consumer<Boolean> completion) {
        player.getScheduler().run(this.plugin, scheduledTask -> {
            final boolean enabled = !this.registry.stateFor(player.getUniqueId()).isEnabled();
            applyEnabled(player, enabled);
            completion.accept(enabled);
        }, null);
    }

    public double scanRadius() {
        return this.config.scanRadius();
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

        refreshTargetForAllRecipients(player.getUniqueId());
        if (state.isEnabled()) {
            syncRecipient(player, false);
        }
    }

    public void handleStateRefresh(final Player player) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, state.isEnabled()));
        syncRecipient(player, true);
        refreshTargetForAllRecipients(player.getUniqueId());
    }

    public void handleStateRefresh(final Player player, final Location location) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(player.getUniqueId());
        this.registry.updateSnapshot(PlayerSnapshot.capture(player, location, state.isEnabled()));
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
                    if (!recipientState.isBedrockClient()) {
                        this.packetSender.removeWaypoints(recipient, List.copyOf(visibleTargets));
                    }
                    visibleTargets.clear();
                }
                clearBedrockActionBar(recipient, recipientState);
                return;
            }

            if (fullRefresh && !visibleTargets.isEmpty()) {
                if (!recipientState.isBedrockClient()) {
                    this.packetSender.removeWaypoints(recipient, List.copyOf(visibleTargets));
                }
                visibleTargets.clear();
            }

            for (final PlayerSnapshot targetSnapshot : this.registry.snapshots()) {
                syncRecipientTarget(recipient, recipientSnapshot, recipientState, targetSnapshot, true);
            }
        }, null);
    }

    private void refreshTargetForAllRecipients(final UUID targetId) {
        for (final Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.getUniqueId().equals(targetId)) {
                continue;
            }

            recipient.getScheduler().run(this.plugin, scheduledTask -> {
                final PlayerSnapshot recipientSnapshot = this.registry.snapshotFor(recipient.getUniqueId());
                final PlayerStateRegistry.PlayerRuntimeState recipientState = this.registry.stateFor(recipient.getUniqueId());
                final PlayerSnapshot targetSnapshot = this.registry.snapshotFor(targetId);
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
        if (!recipientState.isEnabled()) {
            if (visibleTargets.remove(targetId) && !recipientState.isBedrockClient()) {
                this.packetSender.removeWaypoint(recipient, targetId);
            }
            return;
        }

        final boolean shouldTrack = this.visibilityEvaluator.shouldTrack(recipientSnapshot, targetSnapshot, this.config)
            && isRecipientAllowedToSee(recipient, targetId);

        if (shouldTrack) {
            if (allowUpsertWhenVisible || !visibleTargets.contains(targetId)) {
                if (!recipientState.isBedrockClient()) {
                    this.packetSender.upsertWaypoint(recipient, targetId, targetSnapshot);
                }
            }
            visibleTargets.add(targetId);
        } else if (visibleTargets.remove(targetId)) {
            if (!recipientState.isBedrockClient()) {
                this.packetSender.removeWaypoint(recipient, targetId);
            }
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
                    if (!recipientState.isBedrockClient()) {
                        this.packetSender.removeWaypoint(recipient, targetId);
                    }
                }
            }, null);
        }
    }

    private void clearRecipientWaypoints(final Player recipient) {
        recipient.getScheduler().run(this.plugin, scheduledTask -> {
            final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(recipient.getUniqueId());
            final List<UUID> visible = new ArrayList<>(state.visibleTargetIds());
            if (!visible.isEmpty()) {
                if (!state.isBedrockClient()) {
                    this.packetSender.removeWaypoints(recipient, visible);
                }
                state.visibleTargetIds().clear();
            }
            clearBedrockActionBar(recipient, state);
        }, null);
    }

    private boolean isBedrockFallbackRecipient(final Player player) {
        return this.config.bedrockFallbackEnabled() && this.bedrockCompatibility.isBedrockPlayer(player);
    }

    private void syncBedrockActionBarTask(final Player player, final PlayerStateRegistry.PlayerRuntimeState state) {
        if (!state.isEnabled() || !state.isBedrockClient()) {
            cancelBedrockActionBarTask(state);
            scheduleClearActionBar(player);
            return;
        }

        if (state.bedrockActionBarTask() != null) {
            return;
        }

        final ScheduledTask task = player.getScheduler().runAtFixedRate(
            this.plugin,
            scheduledTask -> renderBedrockActionBar(player),
            () -> {
                if (this.registry.snapshotFor(player.getUniqueId()) != null) {
                    this.registry.stateFor(player.getUniqueId()).setBedrockActionBarTask(null);
                }
            },
            1L,
            this.config.bedrockActionbarIntervalTicks()
        );
        state.setBedrockActionBarTask(task);
    }

    private void cancelBedrockActionBarTask(final PlayerStateRegistry.PlayerRuntimeState state) {
        final ScheduledTask task = state.bedrockActionBarTask();
        if (task != null) {
            task.cancel();
            state.setBedrockActionBarTask(null);
        }
    }

    private void renderBedrockActionBar(final Player recipient) {
        final PlayerStateRegistry.PlayerRuntimeState state = this.registry.stateFor(recipient.getUniqueId());
        if (!state.isEnabled() || !state.isBedrockClient()) {
            clearBedrockActionBar(recipient, state);
            return;
        }

        final PlayerSnapshot recipientSnapshot = this.registry.snapshotFor(recipient.getUniqueId());
        final List<PlayerSnapshot> targets = state.visibleTargetIds().stream()
            .map(this.registry::snapshotFor)
            .filter(snapshot -> snapshot != null
                && this.visibilityEvaluator.shouldTrack(recipientSnapshot, snapshot, this.config)
                && isRecipientAllowedToSee(recipient, snapshot.playerId()))
            .toList();

        final String message = this.bedrockActionBarRenderer.render(recipientSnapshot, targets, this.config.bedrockMaxTargets());
        recipient.sendActionBar(message.isEmpty() ? Component.empty() : Component.text(message));
    }

    private void clearBedrockActionBar(final Player recipient, final PlayerStateRegistry.PlayerRuntimeState state) {
        if (state.isBedrockClient()) {
            recipient.sendActionBar(Component.empty());
        }
    }

    private void scheduleClearActionBar(final Player player) {
        player.getScheduler().run(this.plugin, scheduledTask -> player.sendActionBar(Component.empty()), null);
    }

    private boolean loadEnabled(final Player player) {
        final Byte stored = player.getPersistentDataContainer().get(this.enabledKey, PersistentDataType.BYTE);
        if (stored == null) {
            return this.config.enabledByDefault();
        }
        return stored != 0;
    }

    private void saveEnabled(final Player player, final boolean enabled) {
        player.getPersistentDataContainer().set(this.enabledKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
    }
}
