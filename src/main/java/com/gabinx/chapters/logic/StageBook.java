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
    private Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> chemicalStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> recipeStagesIndex = Map.of();
    private Map<ResourceLocation, Set<ResourceLocation>> dimensionStagesIndex = Map.of();

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

        Map<ResourceLocation, Set<ResourceLocation>> itemMap = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> fluidMap = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> chemicalMap = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> recipeMap = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> dimensionMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            for (ResourceLocation itemId : def.items()) {
                itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (TagKey<Item> tag : def.tags()) {
                for (ResourceLocation itemId : catalog.itemsInTag(tag.location())) {
                    itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.namespaces()) {
                for (ResourceLocation itemId : catalog.itemsInNamespace(ns)) {
                    itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (ResourceLocation fluidId : def.fluids()) {
                fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (TagKey<Fluid> tag : def.fluidTags()) {
                for (ResourceLocation fluidId : catalog.fluidsInTag(tag.location())) {
                    fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.fluidNamespaces()) {
                for (ResourceLocation fluidId : catalog.fluidsInNamespace(ns)) {
                    fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (ResourceLocation chemicalId : def.chemicals()) {
                chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (ResourceLocation tagId : def.chemicalTags()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInTag(tagId)) {
                    chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.chemicalNamespaces()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInNamespace(ns)) {
                    chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (ResourceLocation recipeId : def.recipes()) {
                recipeMap.computeIfAbsent(recipeId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (ResourceLocation dimensionId : def.dimensions()) {
                dimensionMap.computeIfAbsent(dimensionId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (ResourceLocation tagId : def.dimensionTags()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInTag(tagId)) {
                    dimensionMap.computeIfAbsent(dimensionId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.dimensionNamespaces()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInNamespace(ns)) {
                    dimensionMap.computeIfAbsent(dimensionId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
        }

        itemStagesIndex = freeze(itemMap);
        fluidStagesIndex = freeze(fluidMap);
        chemicalStagesIndex = freeze(chemicalMap);
        recipeStagesIndex = freeze(recipeMap);
        dimensionStagesIndex = freeze(dimensionMap);
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

    private static Map<ResourceLocation, Set<ResourceLocation>> freeze(Map<ResourceLocation, Set<ResourceLocation>> source) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : source.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
