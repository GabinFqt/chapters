package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
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
    private final Map<ResourceLocation, StageDefinition> datapackDefinitions = new LinkedHashMap<>();
    private final Map<ResourceLocation, StageDefinition> runtimeDefinitions = new LinkedHashMap<>();

    private List<StageDefinition> mergedDefinitions = List.of();

    /** contentId → (stageId → flags) */
    private Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> itemEdges = Map.of();
    private Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> fluidEdges = Map.of();
    private Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> chemicalEdges = Map.of();
    private Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> recipeEdges = Map.of();
    private Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> dimensionEdges = Map.of();

    private Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicalStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> recipeStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> dimensionStagesIndex = Map.of();

    private Map<ResourceLocation, Set<ResourceLocation>> itemJeiStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluidJeiStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> recipeJeiStagesIndex = Map.of();

    public StageBook(ContentCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public ContentCatalog catalog() {
        return catalog;
    }

    public synchronized void replaceDatapack(Map<ResourceLocation, StageDefinition> next) {
        datapackDefinitions.clear();
        if (next != null) {
            datapackDefinitions.putAll(next);
        }
        rebuildLocked();
    }

    public synchronized void replaceDatapackFromJson(Map<ResourceLocation, JsonObject> objects) {
        Map<ResourceLocation, StageDefinition> next = new LinkedHashMap<>();
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
        Map<ResourceLocation, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        mergedDefinitions = List.copyOf(merged.values());

        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> itemMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> fluidMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> chemicalMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> recipeMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> dimensionMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            ResourceLocation stageId = def.id();
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.items().entrySet()) {
                putEdge(itemMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<TagKey<Item>, RestrictionFlags> e : def.tags().entrySet()) {
                for (ResourceLocation itemId : catalog.itemsInTag(e.getKey().location())) {
                    putEdge(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.namespaces().entrySet()) {
                for (ResourceLocation itemId : catalog.itemsInNamespace(e.getKey())) {
                    putEdge(itemMap, itemId, stageId, e.getValue());
                }
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.fluids().entrySet()) {
                putEdge(fluidMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<TagKey<Fluid>, RestrictionFlags> e : def.fluidTags().entrySet()) {
                for (ResourceLocation fluidId : catalog.fluidsInTag(e.getKey().location())) {
                    putEdge(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.fluidNamespaces().entrySet()) {
                for (ResourceLocation fluidId : catalog.fluidsInNamespace(e.getKey())) {
                    putEdge(fluidMap, fluidId, stageId, e.getValue());
                }
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.chemicals().entrySet()) {
                putEdge(chemicalMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.chemicalTags().entrySet()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInTag(e.getKey())) {
                    putEdge(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.chemicalNamespaces().entrySet()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInNamespace(e.getKey())) {
                    putEdge(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.recipes().entrySet()) {
                putEdge(recipeMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.dimensions().entrySet()) {
                putEdge(dimensionMap, e.getKey(), stageId, e.getValue());
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.dimensionTags().entrySet()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInTag(e.getKey())) {
                    putEdge(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.dimensionNamespaces().entrySet()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInNamespace(e.getKey())) {
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
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> map,
            ResourceLocation contentId,
            ResourceLocation stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).merge(stageId, f, RestrictionFlags::or);
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndexView() {
        return itemStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndexView() {
        return fluidStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> chemicalStagesIndexView() {
        return chemicalStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> recipeStagesIndexView() {
        return recipeStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> dimensionStagesIndexView() {
        return dimensionStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> itemJeiStagesIndexView() {
        return itemJeiStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidJeiStagesIndexView() {
        return fluidJeiStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiStagesIndexView() {
        return chemicalJeiStagesIndex;
    }

    public synchronized Map<ResourceLocation, Set<ResourceLocation>> recipeJeiStagesIndexView() {
        return recipeJeiStagesIndex;
    }

    public synchronized Map<ResourceLocation, StageDefinition> allDefinitions() {
        Map<ResourceLocation, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        return merged;
    }

    public synchronized Optional<StageDefinition> get(ResourceLocation stageId) {
        StageDefinition runtime = runtimeDefinitions.get(stageId);
        if (runtime != null) {
            return Optional.of(runtime);
        }
        return Optional.ofNullable(datapackDefinitions.get(stageId));
    }

    public synchronized Set<ResourceLocation> stageIds() {
        Set<ResourceLocation> ids = new LinkedHashSet<>(datapackDefinitions.keySet());
        ids.addAll(runtimeDefinitions.keySet());
        return ids;
    }

    public synchronized boolean isItemLocked(ResourceLocation itemId, Set<ResourceLocation> ownedStages) {
        if (itemId == null) {
            return false;
        }
        return LockRules.isLocked(itemStagesIndex.get(itemId), ownedStages);
    }

    public synchronized boolean isFluidLocked(ResourceLocation fluidKindId, Set<ResourceLocation> ownedStages) {
        if (fluidKindId == null) {
            return false;
        }
        return LockRules.isLocked(fluidStagesIndex.get(fluidKindId), ownedStages);
    }

    public synchronized boolean isChemicalLocked(ResourceLocation chemicalId, Set<ResourceLocation> ownedStages) {
        if (chemicalId == null) {
            return false;
        }
        return LockRules.isLocked(chemicalStagesIndex.get(chemicalId), ownedStages);
    }

    public synchronized boolean isRecipeLocked(ResourceLocation recipeId, Set<ResourceLocation> ownedStages) {
        if (recipeId == null) {
            return false;
        }
        return LockRules.isLocked(recipeStagesIndex.get(recipeId), ownedStages);
    }

    public synchronized boolean isDimensionLocked(ResourceLocation dimensionId, Set<ResourceLocation> ownedStages) {
        if (dimensionId == null) {
            return false;
        }
        return LockRules.isLocked(dimensionStagesIndex.get(dimensionId), ownedStages);
    }

    public synchronized boolean isItemRestricted(
            ResourceLocation itemId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(itemEdges.get(itemId), ownedStages, restriction);
    }

    public synchronized boolean isFluidRestricted(
            ResourceLocation fluidKindId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(fluidEdges.get(fluidKindId), ownedStages, restriction);
    }

    public synchronized boolean isChemicalRestricted(
            ResourceLocation chemicalId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(chemicalEdges.get(chemicalId), ownedStages, restriction);
    }

    public synchronized boolean isRecipeRestricted(
            ResourceLocation recipeId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(recipeEdges.get(recipeId), ownedStages, restriction);
    }

    public synchronized boolean isDimensionRestricted(
            ResourceLocation dimensionId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(dimensionEdges.get(dimensionId), ownedStages, restriction);
    }

    /**
     * Item lock including filled-bucket → fluid gating (same rule as LockResolver).
     */
    public synchronized boolean isItemOrFluidBucketLocked(ResourceLocation itemId, Set<ResourceLocation> ownedStages) {
        if (isItemLocked(itemId, ownedStages)) {
            return true;
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : fluidStagesIndex.entrySet()) {
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
            ResourceLocation itemId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        if (isItemRestricted(itemId, ownedStages, restriction)) {
            return true;
        }
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> e : fluidEdges.entrySet()) {
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
            Map<ResourceLocation, RestrictionFlags> edges,
            Set<ResourceLocation> ownedStages,
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

    private static Map<ResourceLocation, Set<ResourceLocation>> stageIndexFrom(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> edges
    ) {
        if (edges.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> out = new LinkedHashMap<>(edges.size());
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> e : edges.entrySet()) {
            out.put(e.getKey(), Set.copyOf(e.getValue().keySet()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> jeiIndexFrom(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> edges
    ) {
        if (edges.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> e : edges.entrySet()) {
            LinkedHashSet<ResourceLocation> stages = new LinkedHashSet<>();
            for (Map.Entry<ResourceLocation, RestrictionFlags> edge : e.getValue().entrySet()) {
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

    private static Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> freezeEdges(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> source
    ) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> e : source.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
