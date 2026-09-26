package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of owned stages + lock indices; computes sets hidden from recipe viewers.
 */
public final class ClientStageView {
    private final Set<Identifier> stages = new LinkedHashSet<>();
    private Map<Identifier, Set<Identifier>> items = Map.of();
    private Map<Identifier, Set<Identifier>> fluids = Map.of();
    private Map<Identifier, Set<Identifier>> chemicals = Map.of();
    private Map<Identifier, Set<Identifier>> recipes = Map.of();

    public synchronized void setStages(Set<Identifier> next) {
        stages.clear();
        if (next != null) {
            stages.addAll(next);
        }
    }

    public synchronized void addStage(Identifier stage) {
        stages.add(stage);
    }

    public synchronized void removeStage(Identifier stage) {
        stages.remove(stage);
    }

    public synchronized Set<Identifier> snapshotStages() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(stages));
    }

    public synchronized void replaceIndices(
            Map<Identifier, Set<Identifier>> nextItems,
            Map<Identifier, Set<Identifier>> nextFluids,
            Map<Identifier, Set<Identifier>> nextChemicals,
            Map<Identifier, Set<Identifier>> nextRecipes
    ) {
        items = freeze(nextItems);
        fluids = freeze(nextFluids);
        chemicals = freeze(nextChemicals);
        recipes = freeze(nextRecipes);
    }

    public synchronized Map<Identifier, Set<Identifier>> itemsView() {
        return items;
    }

    public synchronized Map<Identifier, Set<Identifier>> fluidsView() {
        return fluids;
    }

    public synchronized Map<Identifier, Set<Identifier>> chemicalsView() {
        return chemicals;
    }

    public synchronized Map<Identifier, Set<Identifier>> recipesView() {
        return recipes;
    }

    public synchronized Set<Identifier> lockedFluidIds() {
        return computeLocked(fluids, snapshotStages());
    }

    public synchronized Set<Identifier> lockedChemicalIds() {
        return computeLocked(chemicals, snapshotStages());
    }

    public synchronized Set<Identifier> lockedRecipeIds() {
        return computeLocked(recipes, snapshotStages());
    }

    /**
     * Locked item ids plus filled buckets whose fluid is locked.
     */
    public synchronized Set<Identifier> lockedItemIds(ContentCatalog catalog) {
        Set<Identifier> locked = computeLocked(items, snapshotStages());
        Set<Identifier> lockedFluids = computeLocked(fluids, snapshotStages());
        if (catalog != null) {
            for (Identifier fluidId : lockedFluids) {
                locked.addAll(catalog.bucketItemsForFluid(fluidId));
            }
        }
        return locked;
    }

    public static Set<Identifier> computeLocked(
            Map<Identifier, Set<Identifier>> index,
            Set<Identifier> activeStages
    ) {
        Set<Identifier> locked = new LinkedHashSet<>();
        if (index == null || index.isEmpty()) {
            return locked;
        }
        Set<Identifier> owned = activeStages == null ? Set.of() : activeStages;
        for (Map.Entry<Identifier, Set<Identifier>> entry : index.entrySet()) {
            if (LockRules.isLocked(entry.getValue(), owned)) {
                locked.add(entry.getKey());
            }
        }
        return locked;
    }

    private static Map<Identifier, Set<Identifier>> freeze(Map<Identifier, Set<Identifier>> next) {
        if (next == null || next.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> copy = new LinkedHashMap<>(next.size());
        for (Map.Entry<Identifier, Set<Identifier>> e : next.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
