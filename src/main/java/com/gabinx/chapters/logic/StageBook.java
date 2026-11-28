package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /** Every index built from the merged definitions. Swapped whole, so lock checks never take a lock. */
    private volatile Indices indices = Indices.EMPTY;

    /** Edge maps are contentId → (stageId → flags); the others are the plain and flag-filtered stage sets. */
    private record Indices(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> itemEdges,
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> fluidEdges,
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> chemicalEdges,
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> recipeEdges,
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> dimensionEdges,
            Map<ResourceLocation, Set<ResourceLocation>> itemStages,
            Map<ResourceLocation, Set<ResourceLocation>> fluidStages,
            Map<ResourceLocation, Set<ResourceLocation>> chemicalStages,
            Map<ResourceLocation, Set<ResourceLocation>> recipeStages,
            Map<ResourceLocation, Set<ResourceLocation>> dimensionStages,
            Map<ResourceLocation, Set<ResourceLocation>> itemJeiStages,
            Map<ResourceLocation, Set<ResourceLocation>> fluidJeiStages,
            Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiStages,
            Map<ResourceLocation, Set<ResourceLocation>> recipeJeiStages,
            Map<ResourceLocation, Set<ResourceLocation>> fluidTransferStages,
            Map<ResourceLocation, Set<ResourceLocation>> chemicalTransferStages
    ) {
        static final Indices EMPTY = new Indices(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of());
    }

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
        Map<ResourceLocation, StageDefinition> merged = allDefinitionsLocked();
        ContentCatalog catalog = new MemoizedCatalog(this.catalog);

        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> itemMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> fluidMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> chemicalMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> recipeMap = new HashMap<>();
        Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> dimensionMap = new HashMap<>();

        for (StageDefinition def : merged.values()) {
            ResourceLocation stageId = def.id();
            // Tags / namespaces first (OR within the stage), then explicit ids replace for that stage.
            for (Map.Entry<TagKey<Item>, RestrictionFlags> e : def.tags().entrySet()) {
                for (ResourceLocation itemId : catalog.itemsInTag(e.getKey().location())) {
                    putEdgeOr(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.namespaces().entrySet()) {
                for (ResourceLocation itemId : catalog.itemsInNamespace(e.getKey())) {
                    putEdgeOr(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.items().entrySet()) {
                putEdgeReplace(itemMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<TagKey<Fluid>, RestrictionFlags> e : def.fluidTags().entrySet()) {
                for (ResourceLocation fluidId : catalog.fluidsInTag(e.getKey().location())) {
                    putEdgeOr(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.fluidNamespaces().entrySet()) {
                for (ResourceLocation fluidId : catalog.fluidsInNamespace(e.getKey())) {
                    putEdgeOr(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.fluids().entrySet()) {
                putEdgeReplace(fluidMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.chemicalTags().entrySet()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInTag(e.getKey())) {
                    putEdgeOr(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.chemicalNamespaces().entrySet()) {
                for (ResourceLocation chemicalId : catalog.chemicalsInNamespace(e.getKey())) {
                    putEdgeOr(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.chemicals().entrySet()) {
                putEdgeReplace(chemicalMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.recipes().entrySet()) {
                putEdgeOr(recipeMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.dimensionTags().entrySet()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInTag(e.getKey())) {
                    putEdgeOr(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.dimensionNamespaces().entrySet()) {
                for (ResourceLocation dimensionId : catalog.dimensionsInNamespace(e.getKey())) {
                    putEdgeOr(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, RestrictionFlags> e : def.dimensions().entrySet()) {
                putEdgeReplace(dimensionMap, e.getKey(), stageId, e.getValue());
            }
        }

        var itemEdges = freezeEdges(itemMap);
        var fluidEdges = freezeEdges(fluidMap);
        var chemicalEdges = freezeEdges(chemicalMap);
        var recipeEdges = freezeEdges(recipeMap);
        var dimensionEdges = freezeEdges(dimensionMap);
        indices = new Indices(
                itemEdges,
                fluidEdges,
                chemicalEdges,
                recipeEdges,
                dimensionEdges,
                stageIndexFrom(itemEdges),
                stageIndexFrom(fluidEdges),
                stageIndexFrom(chemicalEdges),
                stageIndexFrom(recipeEdges),
                stageIndexFrom(dimensionEdges),
                flagIndexFrom(itemEdges, Restriction.JEI),
                flagIndexFrom(fluidEdges, Restriction.JEI),
                flagIndexFrom(chemicalEdges, Restriction.JEI),
                flagIndexFrom(recipeEdges, Restriction.JEI),
                flagIndexFrom(fluidEdges, Restriction.TRANSFER),
                flagIndexFrom(chemicalEdges, Restriction.TRANSFER));
    }

    /** OR-merge flags for the same stage (tag / namespace expansions). */
    private static void putEdgeOr(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> map,
            ResourceLocation contentId,
            ResourceLocation stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).merge(stageId, f, RestrictionFlags::or);
    }

    /** Explicit id replaces any prior flags for this stage (overrides tag / namespace). */
    private static void putEdgeReplace(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> map,
            ResourceLocation contentId,
            ResourceLocation stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).put(stageId, f);
    }

    public Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndexView() {
        return indices.itemStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndexView() {
        return indices.fluidStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> chemicalStagesIndexView() {
        return indices.chemicalStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> recipeStagesIndexView() {
        return indices.recipeStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> dimensionStagesIndexView() {
        return indices.dimensionStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> itemJeiStagesIndexView() {
        return indices.itemJeiStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> fluidJeiStagesIndexView() {
        return indices.fluidJeiStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiStagesIndexView() {
        return indices.chemicalJeiStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> recipeJeiStagesIndexView() {
        return indices.recipeJeiStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> fluidTransferStagesIndexView() {
        return indices.fluidTransferStages();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> chemicalTransferStagesIndexView() {
        return indices.chemicalTransferStages();
    }

    public synchronized Map<ResourceLocation, StageDefinition> allDefinitions() {
        return allDefinitionsLocked();
    }

    private Map<ResourceLocation, StageDefinition> allDefinitionsLocked() {
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

    public boolean isItemLocked(ResourceLocation itemId, Set<ResourceLocation> ownedStages) {
        if (itemId == null) {
            return false;
        }
        return LockRules.isLocked(indices.itemStages().get(itemId), ownedStages);
    }

    public boolean isFluidLocked(ResourceLocation fluidKindId, Set<ResourceLocation> ownedStages) {
        if (fluidKindId == null) {
            return false;
        }
        return LockRules.isLocked(indices.fluidStages().get(fluidKindId), ownedStages);
    }

    public boolean isChemicalLocked(ResourceLocation chemicalId, Set<ResourceLocation> ownedStages) {
        if (chemicalId == null) {
            return false;
        }
        return LockRules.isLocked(indices.chemicalStages().get(chemicalId), ownedStages);
    }

    public boolean isRecipeLocked(ResourceLocation recipeId, Set<ResourceLocation> ownedStages) {
        if (recipeId == null) {
            return false;
        }
        return LockRules.isLocked(indices.recipeStages().get(recipeId), ownedStages);
    }

    public boolean isDimensionLocked(ResourceLocation dimensionId, Set<ResourceLocation> ownedStages) {
        if (dimensionId == null) {
            return false;
        }
        return LockRules.isLocked(indices.dimensionStages().get(dimensionId), ownedStages);
    }

    public boolean isItemRestricted(
            ResourceLocation itemId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(indices.itemEdges().get(itemId), ownedStages, restriction);
    }

    public boolean isFluidRestricted(
            ResourceLocation fluidKindId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(indices.fluidEdges().get(fluidKindId), ownedStages, restriction);
    }

    public boolean isChemicalRestricted(
            ResourceLocation chemicalId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(indices.chemicalEdges().get(chemicalId), ownedStages, restriction);
    }

    public boolean isRecipeRestricted(
            ResourceLocation recipeId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(indices.recipeEdges().get(recipeId), ownedStages, restriction);
    }

    public boolean isDimensionRestricted(
            ResourceLocation dimensionId, Set<ResourceLocation> ownedStages, Restriction restriction) {
        return isRestricted(indices.dimensionEdges().get(dimensionId), ownedStages, restriction);
    }

    /**
     * Item restriction including the filled-bucket rule: a bucket counts as restricted while its fluid is.
     *
     * @param bucketFluidId fluid kind held by the item when it is a filled bucket, else {@code null}
     */
    public boolean isItemOrFluidBucketRestricted(
            ResourceLocation itemId,
            @Nullable ResourceLocation bucketFluidId,
            Set<ResourceLocation> ownedStages,
            Restriction restriction) {
        Indices current = indices;
        return isRestricted(current.itemEdges().get(itemId), ownedStages, restriction)
                || (bucketFluidId != null
                        && isRestricted(current.fluidEdges().get(bucketFluidId), ownedStages, restriction));
    }

    /** True when no item and no fluid is gated, so inventory checks can stop early. */
    public boolean hasNoItemOrFluidLocks() {
        Indices current = indices;
        return current.itemEdges().isEmpty() && current.fluidEdges().isEmpty();
    }

    /** Whether a stage locks a dimension through a tag or a namespace, which only the running server can resolve. */
    public synchronized boolean usesDimensionExpansion() {
        for (StageDefinition def : allDefinitionsLocked().values()) {
            if (!def.dimensionTags().isEmpty() || !def.dimensionNamespaces().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Rebuilds every index from the current definitions, after the registries the catalog reads have changed. */
    public synchronized void rebuild() {
        rebuildLocked();
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

    private static Map<ResourceLocation, Set<ResourceLocation>> flagIndexFrom(
            Map<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> edges,
            Restriction restriction
    ) {
        if (edges.isEmpty() || restriction == null) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, RestrictionFlags>> e : edges.entrySet()) {
            LinkedHashSet<ResourceLocation> stages = new LinkedHashSet<>();
            for (Map.Entry<ResourceLocation, RestrictionFlags> edge : e.getValue().entrySet()) {
                if (edge.getValue() != null && edge.getValue().allows(restriction)) {
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
