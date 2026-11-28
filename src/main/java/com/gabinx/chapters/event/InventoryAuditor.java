package com.gabinx.chapters.event;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import com.gabinx.chapters.logic.InventoryPlan;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.LockResolver;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class InventoryAuditor {
    private InventoryAuditor() {
    }

    /**
     * Periodically drops locked items (e.g. after /give, loot, or stages changed without going through
     * {@link com.gabinx.chapters.api.ChaptersAPI#removeStage}).
     */
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            auditNow(player);
        }
    }

    public static void auditNow(ServerPlayer player) {
        StageBook book = StageManager.get().book();
        if (book.hasNoItemOrFluidLocks()) {
            return;
        }
        Inventory inventory = player.getInventory();
        List<Identifier> slotIds = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            slotIds.add(stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        List<Integer> toDrop = InventoryPlan.slotsToDrop(
                slotIds,
                book,
                EffectiveStages.view(player),
                itemId -> LockResolver.bucketFluidId(BuiltInRegistries.ITEM.getValue(itemId)));
        for (int slot : toDrop) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack drop = stack.copy();
            inventory.setItem(slot, ItemStack.EMPTY);
            player.drop(drop, false);
        }
    }
}
