package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of owned stages + lock indices; computes sets hidden from recipe viewers.
 */
public final class ClientStageView {
    private final Set<ResourceLocation> stages = new LinkedHashSet<>();
    private Map<ResourceLocation, Set<ResourceLocation>> items = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluids = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicals = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> recipes = Map.of();

    public synchronized void setStages(Set<ResourceLocation> next) {
        stages.clear();
        if (next != null) {
            stages.addAll(next);
        }
    }

    public synchronized void addStage(ResourceLocation stage) {
        stages.add(stage);
    }

    public synchronized void removeStage(ResourceLocation stage) {
        stages.remove(stage);
    }

    public synchronized Set<ResourceLocation> snapshotStages() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(stages));
    }

    public synchronized void replaceIndices(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes
    ) {
        items = freeze(nextItems);
        fluids = freeze(nextFluids);
        chemicals = freeze(nextChemicals);
        recipes = freeze(nextRecipes);
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> itemsView() {
        return items;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidsView() {
        return fluids;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> chemicalsView() {
        return chemicals;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> recipesView() {
        return recipes;
    }

    public synchronized Set<ResourceLocation> lockedFluidIds() {
        return computeLocked(fluids, snapshotStages());
    }

    public synchronized Set<ResourceLocation> lockedChemicalIds() {
        return computeLocked(chemicals, snapshotStages());
    }

    public synchronized Set<ResourceLocation> lockedRecipeIds() {
        return computeLocked(recipes, snapshotStages());
    }

    /**
     * Locked item ids plus filled buckets whose fluid is locked.
     */
    public synchronized Set<ResourceLocation> lockedItemIds(ContentCatalog catalog) {
        Set<ResourceLocation> locked = computeLocked(items, snapshotStages());
        Set<ResourceLocation> lockedFluids = computeLocked(fluids, snapshotStages());
        if (catalog != null) {
            for (ResourceLocation fluidId : lockedFluids) {
                locked.addAll(catalog.bucketItemsForFluid(fluidId));
            }
        }
        return locked;
    }

    public static Set<ResourceLocation> computeLocked(
            Map<ResourceLocation, Set<ResourceLocation>> index,
            Set<ResourceLocation> activeStages
    ) {
        Set<ResourceLocation> locked = new LinkedHashSet<>();
        if (index == null || index.isEmpty()) {
            return locked;
        }
        Set<ResourceLocation> owned = activeStages == null ? Set.of() : activeStages;
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : index.entrySet()) {
            if (LockRules.isLocked(entry.getValue(), owned)) {
                locked.add(entry.getKey());
            }
        }
        return locked;
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> freeze(Map<ResourceLocation, Set<ResourceLocation>> next) {
        if (next == null || next.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> copy = new LinkedHashMap<>(next.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : next.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
