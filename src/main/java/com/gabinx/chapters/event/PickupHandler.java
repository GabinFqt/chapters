package com.gabinx.chapters.event;

import com.gabinx.chapters.logic.Restriction;
import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public final class PickupHandler {
    private PickupHandler() {
    }

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (LockResolver.isRestricted(player, event.getItemEntity().getItem(), Restriction.PICKUP)) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
