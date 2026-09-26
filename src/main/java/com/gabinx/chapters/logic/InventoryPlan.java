package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Decides which inventory slots hold locked items and should be dropped.
 */
public final class InventoryPlan {
    private InventoryPlan() {
    }

    /**
     * @param slotItemIds item registry key per slot ({@code null} or empty = empty slot)
     * @param isItemLocked predicate over item registry keys
     * @return slot indices to clear and drop
     */
    public static List<Integer> slotsToDrop(List<Identifier> slotItemIds, Predicate<Identifier> isItemLocked) {
        List<Integer> slots = new ArrayList<>();
        if (slotItemIds == null) {
            return slots;
        }
        for (int i = 0; i < slotItemIds.size(); i++) {
            Identifier itemId = slotItemIds.get(i);
            if (itemId == null) {
                continue;
            }
            if (isItemLocked.test(itemId)) {
                slots.add(i);
            }
        }
        return slots;
    }

    public static List<Integer> slotsToDrop(
            List<Identifier> slotItemIds,
            StageBook book,
            Set<Identifier> ownedStages
    ) {
        return slotsToDrop(slotItemIds, itemId -> book.isItemLocked(itemId, ownedStages));
    }
}
