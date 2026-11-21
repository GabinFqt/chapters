package com.gabinx.chapters.event;

import com.gabinx.chapters.api.ChaptersAPI;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerJoinHandler {
    private PlayerJoinHandler() {
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChaptersAPI.syncAll(player);
            DimensionHandler.auditNow(player);
        }
    }

    /**
     * Sync after the new player entity exists and attachments are copied.
     * Prefer this over {@link PlayerEvent.Clone}: Clone can run before NeoForge's
     * {@code copyOnDeath} listener finishes, which would sync an empty stage set to JEI.
     */
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChaptersAPI.syncAll(player);
            DimensionHandler.auditNow(player);
        }
    }
}
