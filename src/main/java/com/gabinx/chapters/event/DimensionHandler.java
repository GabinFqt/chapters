package com.gabinx.chapters.event;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.logic.DimensionGate;
import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import java.util.Set;

/**
 * Blocks travel into locked dimensions and ejects players already inside one.
 */
public final class DimensionHandler {
    private DimensionHandler() {
    }

    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        ResourceKey<Level> destination = event.getDimension();
        ResourceKey<Level> current = entity.level().dimension();
        if (destination == null || current == null || destination.equals(current)) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            if (tryBlockPlayerTravel(player, destination)) {
                event.setCanceled(true);
            }
            return;
        }

        // Vehicle carrying locked players: cancel the whole trip.
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof ServerPlayer player && tryBlockPlayerTravel(player, destination)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean tryBlockPlayerTravel(ServerPlayer player, ResourceKey<Level> destination) {
        ResourceLocation destId = destination.location();
        boolean locked = LockResolver.isDimensionLocked(player, destId);
        if (!DimensionGate.shouldBlockTravel(locked, false)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("commands.chapters.dimension.blocked", destId.toString()), true);
        return true;
    }

    public static void auditNow(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ResourceLocation currentId = player.level().dimension().location();
        ResourceLocation overworldId = Level.OVERWORLD.location();
        boolean currentLocked = LockResolver.isDimensionLocked(player, currentId);
        boolean overworldLocked = LockResolver.isDimensionLocked(player, overworldId);
        if (!DimensionGate.shouldEject(currentLocked, overworldLocked)) {
            if (currentLocked && overworldLocked) {
                Chapters.LOGGER.warn(
                        "Player {} is in locked dimension {} and Overworld is also locked; skipping eject",
                        player.getGameProfile().getName(),
                        currentId);
            }
            return;
        }

        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        var pos = overworld.getSharedSpawnPos();
        float yaw = overworld.getSharedSpawnAngle();
        player.teleportTo(
                overworld,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                Set.of(),
                yaw,
                0.0F);
        player.displayClientMessage(Component.translatable("commands.chapters.dimension.ejected", currentId.toString()), true);
    }

    /** Convenience for callers that hold a generic {@link Player}. */
    public static void auditIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            auditNow(serverPlayer);
        }
    }
}
