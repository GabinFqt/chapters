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
    private Map<ResourceLocation, Set<ResourceLocation>> itemJei = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluidJei = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicalJei = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> recipeJei = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluidTransfer = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicalTransfer = Map.of();

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
        replaceIndices(
                nextItems,
                nextFluids,
                nextChemicals,
                nextRecipes,
                nextItems,
                nextFluids,
                nextChemicals,
                nextRecipes,
                nextFluids,
                nextChemicals);
    }

    public synchronized void replaceIndices(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes,
            Map<ResourceLocation, Set<ResourceLocation>> nextItemJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluidJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicalJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipeJei
    ) {
        replaceIndices(
                nextItems,
                nextFluids,
                nextChemicals,
                nextRecipes,
                nextItemJei,
                nextFluidJei,
                nextChemicalJei,
                nextRecipeJei,
                nextFluids,
                nextChemicals);
    }

    public synchronized void replaceIndices(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes,
            Map<ResourceLocation, Set<ResourceLocation>> nextItemJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluidJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicalJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipeJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluidTransfer,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicalTransfer
    ) {
        items = freeze(nextItems);
        fluids = freeze(nextFluids);
        chemicals = freeze(nextChemicals);
        recipes = freeze(nextRecipes);
        itemJei = freeze(nextItemJei);
        fluidJei = freeze(nextFluidJei);
        chemicalJei = freeze(nextChemicalJei);
        recipeJei = freeze(nextRecipeJei);
        fluidTransfer = freeze(nextFluidTransfer);
        chemicalTransfer = freeze(nextChemicalTransfer);
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

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> itemJeiView() {
        return itemJei;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidJeiView() {
        return fluidJei;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiView() {
        return chemicalJei;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> recipeJeiView() {
        return recipeJei;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidTransferView() {
        return fluidTransfer;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> chemicalTransferView() {
        return chemicalTransfer;
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

    /**
     * Matches {@link StageBook#isRestricted}: locked on the full defining set, and present in the
     * flag-filtered index (at least one defining stage enables that flag).
     */
    public static Set<ResourceLocation> computeFlagRestricted(
            Map<ResourceLocation, Set<ResourceLocation>> lockIndex,
            Map<ResourceLocation, Set<ResourceLocation>> flagIndex,
            Set<ResourceLocation> activeStages
    ) {
        Set<ResourceLocation> restricted = new LinkedHashSet<>();
        if (lockIndex == null || lockIndex.isEmpty() || flagIndex == null || flagIndex.isEmpty()) {
            return restricted;
        }
        Set<ResourceLocation> owned = activeStages == null ? Set.of() : activeStages;
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : lockIndex.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!flagIndex.containsKey(id)) {
                continue;
            }
            if (LockRules.isLocked(entry.getValue(), owned)) {
                restricted.add(id);
            }
        }
        return restricted;
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
