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
    private Map<Identifier, Set<Identifier>> itemJei = Map.of();
    private Map<Identifier, Set<Identifier>> fluidJei = Map.of();
    private Map<Identifier, Set<Identifier>> chemicalJei = Map.of();
    private Map<Identifier, Set<Identifier>> recipeJei = Map.of();
    private Map<Identifier, Set<Identifier>> fluidTransfer = Map.of();
    private Map<Identifier, Set<Identifier>> chemicalTransfer = Map.of();

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
            Map<Identifier, Set<Identifier>> nextRecipes,
            Map<Identifier, Set<Identifier>> nextItemJei,
            Map<Identifier, Set<Identifier>> nextFluidJei,
            Map<Identifier, Set<Identifier>> nextChemicalJei,
            Map<Identifier, Set<Identifier>> nextRecipeJei,
            Map<Identifier, Set<Identifier>> nextFluidTransfer,
            Map<Identifier, Set<Identifier>> nextChemicalTransfer
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

    public synchronized Map<Identifier, Set<Identifier>> itemJeiView() {
        return itemJei;
    }

    public synchronized Map<Identifier, Set<Identifier>> fluidJeiView() {
        return fluidJei;
    }

    public synchronized Map<Identifier, Set<Identifier>> chemicalJeiView() {
        return chemicalJei;
    }

    public synchronized Map<Identifier, Set<Identifier>> recipeJeiView() {
        return recipeJei;
    }

    public synchronized Map<Identifier, Set<Identifier>> fluidTransferView() {
        return fluidTransfer;
    }

    public synchronized Map<Identifier, Set<Identifier>> chemicalTransferView() {
        return chemicalTransfer;
    }

    /** Ids the recipe viewer hides for the current stages: one consistent snapshot of stages and indices. */
    public record Hidden(
            Set<Identifier> items,
            Set<Identifier> fluids,
            Set<Identifier> chemicals,
            Set<Identifier> recipes) {
    }

    /**
     * Content locked for the current stages whose locking stages enable {@code hideInJei}. Filled buckets follow
     * their fluid, so {@code catalog} maps each hidden fluid to its bucket item.
     */
    public synchronized Hidden hidden(ContentCatalog catalog) {
        Set<Identifier> active = snapshotStages();
        Set<Identifier> hiddenFluids = computeFlagRestricted(fluids, fluidJei, active);
        Set<Identifier> hiddenItems = computeFlagRestricted(items, itemJei, active);
        if (catalog != null) {
            for (Identifier fluidId : hiddenFluids) {
                hiddenItems.addAll(catalog.bucketItemsForFluid(fluidId));
            }
        }
        return new Hidden(
                hiddenItems,
                hiddenFluids,
                computeFlagRestricted(chemicals, chemicalJei, active),
                computeFlagRestricted(recipes, recipeJei, active));
    }

    /**
     * Matches {@link StageBook#isRestricted}: locked on the full defining set, and present in the
     * flag-filtered index (at least one defining stage enables that flag).
     */
    public static Set<Identifier> computeFlagRestricted(
            Map<Identifier, Set<Identifier>> lockIndex,
            Map<Identifier, Set<Identifier>> flagIndex,
            Set<Identifier> activeStages
    ) {
        Set<Identifier> restricted = new LinkedHashSet<>();
        if (lockIndex == null || lockIndex.isEmpty() || flagIndex == null || flagIndex.isEmpty()) {
            return restricted;
        }
        Set<Identifier> owned = activeStages == null ? Set.of() : activeStages;
        // The flag index only lists ids that are also in the lock index, and is usually the smaller one.
        for (Identifier id : flagIndex.keySet()) {
            if (LockRules.isLocked(lockIndex.get(id), owned)) {
                restricted.add(id);
            }
        }
        return restricted;
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
