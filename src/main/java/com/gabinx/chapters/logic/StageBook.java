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
    private Map<Identifier, Set<Identifier>> itemStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> fluidStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> chemicalStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> recipeStagesIndex = Map.of();
    private Map<Identifier, Set<Identifier>> dimensionStagesIndex = Map.of();

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

        Map<Identifier, Set<Identifier>> itemMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> fluidMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> chemicalMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> recipeMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> dimensionMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            for (Identifier itemId : def.items()) {
                itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (TagKey<Item> tag : def.tags()) {
                for (Identifier itemId : catalog.itemsInTag(tag.location())) {
                    itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.namespaces()) {
                for (Identifier itemId : catalog.itemsInNamespace(ns)) {
                    itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (Identifier fluidId : def.fluids()) {
                fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (TagKey<Fluid> tag : def.fluidTags()) {
                for (Identifier fluidId : catalog.fluidsInTag(tag.location())) {
                    fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.fluidNamespaces()) {
                for (Identifier fluidId : catalog.fluidsInNamespace(ns)) {
                    fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (Identifier chemicalId : def.chemicals()) {
                chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (Identifier tagId : def.chemicalTags()) {
                for (Identifier chemicalId : catalog.chemicalsInTag(tagId)) {
                    chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.chemicalNamespaces()) {
                for (Identifier chemicalId : catalog.chemicalsInNamespace(ns)) {
                    chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }

            for (Identifier recipeId : def.recipes()) {
                recipeMap.computeIfAbsent(recipeId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (Identifier dimensionId : def.dimensions()) {
                dimensionMap.computeIfAbsent(dimensionId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (Identifier tagId : def.dimensionTags()) {
                for (Identifier dimensionId : catalog.dimensionsInTag(tagId)) {
                    dimensionMap.computeIfAbsent(dimensionId, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.dimensionNamespaces()) {
                for (Identifier dimensionId : catalog.dimensionsInNamespace(ns)) {
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

    private static Map<Identifier, Set<Identifier>> freeze(Map<Identifier, Set<Identifier>> source) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<Identifier, Set<Identifier>> e : source.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
