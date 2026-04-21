package com.example.locatebarfolia.service;

import com.example.locatebarfolia.model.PlayerSnapshot;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStateRegistry {
    private final Map<UUID, PlayerRuntimeState> states = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();

    public PlayerRuntimeState stateFor(final UUID playerId) {
        return this.states.computeIfAbsent(playerId, ignored -> new PlayerRuntimeState());
    }

    public PlayerRuntimeState removeState(final UUID playerId) {
        this.snapshots.remove(playerId);
        return this.states.remove(playerId);
    }

    public void updateSnapshot(final PlayerSnapshot snapshot) {
        this.snapshots.put(snapshot.playerId(), snapshot);
    }

    public PlayerSnapshot snapshotFor(final UUID playerId) {
        return this.snapshots.get(playerId);
    }

    public Collection<PlayerSnapshot> snapshots() {
        return this.snapshots.values();
    }

    public Collection<Map.Entry<UUID, PlayerRuntimeState>> states() {
        return this.states.entrySet();
    }

    public static final class PlayerRuntimeState {
        private volatile boolean enabled = true;
        private volatile boolean bedrockClient;
        private volatile ScheduledTask bedrockActionBarTask;
        private final Set<UUID> visibleTargetIds = ConcurrentHashMap.newKeySet();

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBedrockClient() {
            return this.bedrockClient;
        }

        public void setBedrockClient(final boolean bedrockClient) {
            this.bedrockClient = bedrockClient;
        }

        public ScheduledTask bedrockActionBarTask() {
            return this.bedrockActionBarTask;
        }

        public void setBedrockActionBarTask(final ScheduledTask bedrockActionBarTask) {
            this.bedrockActionBarTask = bedrockActionBarTask;
        }

        public Set<UUID> visibleTargetIds() {
            return this.visibleTargetIds;
        }
    }
}
