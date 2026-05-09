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
        }
    }

    public static void onRespawn(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChaptersAPI.syncAll(player);
        }
    }
}
