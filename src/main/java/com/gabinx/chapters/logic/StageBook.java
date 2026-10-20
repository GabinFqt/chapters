package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Datapack + runtime stage definitions and the five lock indices. Independent of the
 * {@link com.gabinx.chapters.stage.StageManager} singleton so unit tests can own an instance.
 */
public final class StageBook {
    private final ContentCatalog catalog;
    private final Map<Identifier, StageDefinition> datapackDefinitions = new LinkedHashMap<>();
    private final Map<Identifier, StageDefinition> runtimeDefinitions = new LinkedHashMap<>();

    private List<StageDefinition> mergedDefinitions = List.of();

    /** contentId → (stageId → flags) */
    private Map<Identifier, Map<Identifier, RestrictionFlags>> itemEdges = Map.of();
    private Map<Identifier, Map<Identifier, RestrictionFlags>> fluidEdges = Map.of();
    private Map<Identifier, Map<Identifier, RestrictionFlags>> chemicalEdges = Map.of();
    private Map<Identifier, Map<Identifier, RestrictionFlags>> recipeEdges = Map.of();
    private Map<Identifier, Map<Identifier, RestrictionFlags>> dimensionEdges = Map.of();

    private Map<Identifier, Set<Identifier>> itemStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> fluidStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> chemicalStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> recipeStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> dimensionStagesIndex = Map.of();

    private Map<Identifier, Set<Identifier>> itemJeiStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> fluidJeiStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> chemicalJeiStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> recipeJeiStagesIndex = Map.of();

    public StageBook(ContentCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public ContentCatalog catalog() {
        return catalog;
    }

    public synchronized void replaceDatapack(Map<Identifier, StageDefinition> next) {
        datapackDefinitions.clear();
        if (next != null) {
            datapackDefinitions.putAll(next);
        }
        rebuildLocked();
    }

    public synchronized void replaceDatapackFromJson(Map<Identifier, JsonObject> objects) {
        Map<Identifier, StageDefinition> next = new LinkedHashMap<>();
        if (objects != null) {
            objects.forEach((id, json) -> next.put(id, StageDefinition.fromJson(id, json)));
        }
        replaceDatapack(next);
    }

    public synchronized void setRuntimeDefinitions(Collection<StageDefinition> runtime) {
        runtimeDefinitions.clear();
        if (runtime != null) {
            for (StageDefinition definition : runtime) {
                runtimeDefinitions.put(definition.id(), definition);
            }
        }
        rebuildLocked();
    }

    private void rebuildLocked() {
        Map<Identifier, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        mergedDefinitions = List.copyOf(merged.values());

        Map<Identifier, Map<Identifier, RestrictionFlags>> itemMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> fluidMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> chemicalMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> recipeMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> dimensionMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            Identifier stageId = def.id();
            for (Map.Entry<Identifier, RestrictionFlags> e : def.items().entrySet()) {
                putEdge(itemMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<TagKey<Item>, RestrictionFlags> e : def.tags().entrySet()) {
                for (Identifier itemId : catalog.itemsInTag(e.getKey().location())) {
                    putEdge(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.namespaces().entrySet()) {
                for (Identifier itemId : catalog.itemsInNamespace(e.getKey())) {
                    putEdge(itemMap, itemId, stageId, e.getValue());
                }
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.fluids().entrySet()) {
                putEdge(fluidMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<TagKey<Fluid>, RestrictionFlags> e : def.fluidTags().entrySet()) {
                for (Identifier fluidId : catalog.fluidsInTag(e.getKey().location())) {
                    putEdge(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.fluidNamespaces().entrySet()) {
                for (Identifier fluidId : catalog.fluidsInNamespace(e.getKey())) {
                    putEdge(fluidMap, fluidId, stageId, e.getValue());
                }
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.chemicals().entrySet()) {
                putEdge(chemicalMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.chemicalTags().entrySet()) {
                for (Identifier chemicalId : catalog.chemicalsInTag(e.getKey())) {
                    putEdge(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.chemicalNamespaces().entrySet()) {
                for (Identifier chemicalId : catalog.chemicalsInNamespace(e.getKey())) {
                    putEdge(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.recipes().entrySet()) {
                putEdge(recipeMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.dimensions().entrySet()) {
                putEdge(dimensionMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.dimensionTags().entrySet()) {
                for (Identifier dimensionId : catalog.dimensionsInTag(e.getKey())) {
                    putEdge(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.dimensionNamespaces().entrySet()) {
                for (Identifier dimensionId : catalog.dimensionsInNamespace(e.getKey())) {
                    putEdge(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
        }

        itemEdges = freezeEdges(itemMap);
        fluidEdges = freezeEdges(fluidMap);
        chemicalEdges = freezeEdges(chemicalMap);
        recipeEdges = freezeEdges(recipeMap);
        dimensionEdges = freezeEdges(dimensionMap);

        itemStagesIndex = stageIndexFrom(itemEdges);
        fluidStagesIndex = stageIndexFrom(fluidEdges);
        chemicalStagesIndex = stageIndexFrom(chemicalEdges);
        recipeStagesIndex = stageIndexFrom(recipeEdges);
        dimensionStagesIndex = stageIndexFrom(dimensionEdges);

        itemJeiStagesIndex = jeiIndexFrom(itemEdges);
        fluidJeiStagesIndex = jeiIndexFrom(fluidEdges);
        chemicalJeiStagesIndex = jeiIndexFrom(chemicalEdges);
        recipeJeiStagesIndex = jeiIndexFrom(recipeEdges);
    }

    private static void putEdge(
            Map<Identifier, Map<Identifier, RestrictionFlags>> map,
            Identifier contentId,
            Identifier stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).merge(stageId, f, RestrictionFlags::or);
    }

    public synchronized Map<Identifier, Set<Identifier>> itemStagesIndexView() {
        return itemStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> fluidStagesIndexView() {
        return fluidStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> chemicalStagesIndexView() {
        return chemicalStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> recipeStagesIndexView() {
        return recipeStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> dimensionStagesIndexView() {
        return dimensionStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> itemJeiStagesIndexView() {
        return itemJeiStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> fluidJeiStagesIndexView() {
        return fluidJeiStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> chemicalJeiStagesIndexView() {
        return chemicalJeiStagesIndex;
    }

    public synchronized Map<Identifier, Set<Identifier>> recipeJeiStagesIndexView() {
        return recipeJeiStagesIndex;
    }

    public synchronized Map<Identifier, StageDefinition> allDefinitions() {
        Map<Identifier, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        return merged;
    }

    public synchronized Optional<StageDefinition> get(Identifier stageId) {
        StageDefinition runtime = runtimeDefinitions.get(stageId);
        if (runtime != null) {
            return Optional.of(runtime);
        }
        return Optional.ofNullable(datapackDefinitions.get(stageId));
    }

    public synchronized Set<Identifier> stageIds() {
        Set<Identifier> ids = new LinkedHashSet<>(datapackDefinitions.keySet());
        ids.addAll(runtimeDefinitions.keySet());
        return ids;
    }

    public synchronized boolean isItemLocked(Identifier itemId, Set<Identifier> ownedStages) {
        if (itemId == null) {
            return false;
        }
        return LockRules.isLocked(itemStagesIndex.get(itemId), ownedStages);
    }

    public synchronized boolean isFluidLocked(Identifier fluidKindId, Set<Identifier> ownedStages) {
        if (fluidKindId == null) {
            return false;
        }
        return LockRules.isLocked(fluidStagesIndex.get(fluidKindId), ownedStages);
    }

    public synchronized boolean isChemicalLocked(Identifier chemicalId, Set<Identifier> ownedStages) {
        if (chemicalId == null) {
            return false;
        }
        return LockRules.isLocked(chemicalStagesIndex.get(chemicalId), ownedStages);
    }

    public synchronized boolean isRecipeLocked(Identifier recipeId, Set<Identifier> ownedStages) {
        if (recipeId == null) {
            return false;
        }
        return LockRules.isLocked(recipeStagesIndex.get(recipeId), ownedStages);
    }

    public synchronized boolean isDimensionLocked(Identifier dimensionId, Set<Identifier> ownedStages) {
        if (dimensionId == null) {
            return false;
        }
        return LockRules.isLocked(dimensionStagesIndex.get(dimensionId), ownedStages);
    }

    public synchronized boolean isItemRestricted(
            Identifier itemId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(itemEdges.get(itemId), ownedStages, restriction);
    }

    public synchronized boolean isFluidRestricted(
            Identifier fluidKindId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(fluidEdges.get(fluidKindId), ownedStages, restriction);
    }

    public synchronized boolean isChemicalRestricted(
            Identifier chemicalId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(chemicalEdges.get(chemicalId), ownedStages, restriction);
    }

    public synchronized boolean isRecipeRestricted(
            Identifier recipeId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(recipeEdges.get(recipeId), ownedStages, restriction);
    }

    public synchronized boolean isDimensionRestricted(
            Identifier dimensionId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(dimensionEdges.get(dimensionId), ownedStages, restriction);
    }

    /**
     * Item lock including filled-bucket → fluid gating (same rule as LockResolver).
     */
    public synchronized boolean isItemOrFluidBucketLocked(Identifier itemId, Set<Identifier> ownedStages) {
        if (isItemLocked(itemId, ownedStages)) {
            return true;
        }
        for (Map.Entry<Identifier, Set<Identifier>> e : fluidStagesIndex.entrySet()) {
            if (!LockRules.isLocked(e.getValue(), ownedStages)) {
                continue;
            }
            if (catalog.bucketItemsForFluid(e.getKey()).contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isItemOrFluidBucketRestricted(
            Identifier itemId, Set<Identifier> ownedStages, Restriction restriction) {
        if (isItemRestricted(itemId, ownedStages, restriction)) {
            return true;
        }
        for (Map.Entry<Identifier, Map<Identifier, RestrictionFlags>> e : fluidEdges.entrySet()) {
            if (!isRestricted(e.getValue(), ownedStages, restriction)) {
                continue;
            }
            if (catalog.bucketItemsForFluid(e.getKey()).contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    static boolean isRestricted(
            Map<Identifier, RestrictionFlags> edges,
            Set<Identifier> ownedStages,
            Restriction restriction
    ) {
        if (edges == null || edges.isEmpty() || restriction == null) {
            return false;
        }
        if (!LockRules.isLocked(edges.keySet(), ownedStages)) {
            return false;
        }
        for (RestrictionFlags flags : edges.values()) {
            if (flags != null && flags.allows(restriction)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Identifier, Set<Identifier>> stageIndexFrom(
            Map<Identifier, Map<Identifier, RestrictionFlags>> edges
    ) {
        if (edges.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> out = new LinkedHashMap<>(edges.size());
        for (Map.Entry<Identifier, Map<Identifier, RestrictionFlags>> e : edges.entrySet()) {
            out.put(e.getKey(), Set.copyOf(e.getValue().keySet()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<Identifier, Set<Identifier>> jeiIndexFrom(
            Map<Identifier, Map<Identifier, RestrictionFlags>> edges
    ) {
        if (edges.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> out = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Map<Identifier, RestrictionFlags>> e : edges.entrySet()) {
            LinkedHashSet<Identifier> stages = new LinkedHashSet<>();
            for (Map.Entry<Identifier, RestrictionFlags> edge : e.getValue().entrySet()) {
                if (edge.getValue() != null && edge.getValue().jei()) {
                    stages.add(edge.getKey());
                }
            }
            if (!stages.isEmpty()) {
                out.put(e.getKey(), Set.copyOf(stages));
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static Map<Identifier, Map<Identifier, RestrictionFlags>> freezeEdges(
            Map<Identifier, Map<Identifier, RestrictionFlags>> source
    ) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Map<Identifier, RestrictionFlags>> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<Identifier, Map<Identifier, RestrictionFlags>> e : source.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
