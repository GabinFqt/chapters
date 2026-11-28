package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Decides which inventory slots hold items that {@code dropFromInventory} forces out.
 */
public final class InventoryPlan {
    private InventoryPlan() {
    }

    /**
     * @param slotItemIds   item registry key per slot ({@code null} = empty slot)
     * @param bucketFluidOf fluid kind held by an item when it is a filled bucket, else {@code null}
     * @return slot indices to clear and drop
     */
    public static List<Integer> slotsToDrop(
            List<ResourceLocation> slotItemIds,
            StageBook book,
            Set<ResourceLocation> ownedStages,
            Function<ResourceLocation, ResourceLocation> bucketFluidOf
    ) {
        List<Integer> slots = new ArrayList<>();
        if (slotItemIds == null || book.hasNoItemOrFluidLocks()) {
            return slots;
        }
        // A full inventory repeats the same few items; check each distinct id once.
        Map<ResourceLocation, Boolean> verdicts = new HashMap<>();
        for (int i = 0; i < slotItemIds.size(); i++) {
            ResourceLocation itemId = slotItemIds.get(i);
            if (itemId == null) {
                continue;
            }
            Boolean drop = verdicts.get(itemId);
            if (drop == null) {
                drop = book.isItemOrFluidBucketRestricted(
                        itemId, bucketFluidOf.apply(itemId), ownedStages, Restriction.INVENTORY);
                verdicts.put(itemId, drop);
            }
            if (drop) {
                slots.add(i);
            }
        }
        return slots;
    }
}
