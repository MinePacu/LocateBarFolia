package com.example.locatebarfolia.service;

import com.example.locatebarfolia.model.PlayerSnapshot;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class WaypointPacketSender {
    public void upsertWaypoint(final Player recipient, final UUID targetId, final PlayerSnapshot snapshot) {
        final ServerPlayer serverPlayer = ((CraftPlayer) recipient).getHandle();
        final Waypoint.Icon icon = new Waypoint.Icon();
        icon.style = WaypointStyleAssets.DEFAULT;
        icon.color = Optional.of(snapshot.waypointColorRgb());

        final BlockPos blockPos = new BlockPos(
            (int) Math.floor(snapshot.x()),
            (int) Math.floor(snapshot.y()),
            (int) Math.floor(snapshot.z())
        );

        serverPlayer.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(targetId, icon, blockPos));
    }

    public void removeWaypoint(final Player recipient, final UUID targetId) {
        final ServerPlayer serverPlayer = ((CraftPlayer) recipient).getHandle();
        serverPlayer.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(targetId));
    }

    public void removeWaypoints(final Player recipient, final Collection<UUID> targetIds) {
        for (final UUID targetId : targetIds) {
            removeWaypoint(recipient, targetId);
        }
    }
}
