package com.gabinx.stagecraft.event;

import com.gabinx.stagecraft.api.StagecraftAPI;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerJoinHandler {
    private PlayerJoinHandler() {
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StagecraftAPI.syncAll(player);
        }
    }

    public static void onRespawn(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StagecraftAPI.syncAll(player);
        }
    }
}
