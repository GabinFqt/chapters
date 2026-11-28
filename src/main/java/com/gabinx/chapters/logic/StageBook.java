package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
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
    private final Map<Identifier, StageDefinition> datapackDefinitions = new LinkedHashMap<>();
    private final Map<Identifier, StageDefinition> runtimeDefinitions = new LinkedHashMap<>();

    /** Every index built from the merged definitions. Swapped whole, so lock checks never take a lock. */
    private volatile Indices indices = Indices.EMPTY;

    /** Edge maps are contentId → (stageId → flags); the others are the plain and flag-filtered stage sets. */
    private record Indices(
            Map<Identifier, Map<Identifier, RestrictionFlags>> itemEdges,
            Map<Identifier, Map<Identifier, RestrictionFlags>> fluidEdges,
            Map<Identifier, Map<Identifier, RestrictionFlags>> chemicalEdges,
            Map<Identifier, Map<Identifier, RestrictionFlags>> recipeEdges,
            Map<Identifier, Map<Identifier, RestrictionFlags>> dimensionEdges,
            Map<Identifier, Set<Identifier>> itemStages,
            Map<Identifier, Set<Identifier>> fluidStages,
            Map<Identifier, Set<Identifier>> chemicalStages,
            Map<Identifier, Set<Identifier>> recipeStages,
            Map<Identifier, Set<Identifier>> dimensionStages,
            Map<Identifier, Set<Identifier>> itemJeiStages,
            Map<Identifier, Set<Identifier>> fluidJeiStages,
            Map<Identifier, Set<Identifier>> chemicalJeiStages,
            Map<Identifier, Set<Identifier>> recipeJeiStages,
            Map<Identifier, Set<Identifier>> fluidTransferStages,
            Map<Identifier, Set<Identifier>> chemicalTransferStages
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
        Map<Identifier, StageDefinition> merged = allDefinitionsLocked();
        ContentCatalog catalog = new MemoizedCatalog(this.catalog);

        Map<Identifier, Map<Identifier, RestrictionFlags>> itemMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> fluidMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> chemicalMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> recipeMap = new HashMap<>();
        Map<Identifier, Map<Identifier, RestrictionFlags>> dimensionMap = new HashMap<>();

        for (StageDefinition def : merged.values()) {
            Identifier stageId = def.id();
            // Tags / namespaces first (OR within the stage), then explicit ids replace for that stage.
            for (Map.Entry<TagKey<Item>, RestrictionFlags> e : def.tags().entrySet()) {
                for (Identifier itemId : catalog.itemsInTag(e.getKey().location())) {
                    putEdgeOr(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.namespaces().entrySet()) {
                for (Identifier itemId : catalog.itemsInNamespace(e.getKey())) {
                    putEdgeOr(itemMap, itemId, stageId, e.getValue());
                }
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.items().entrySet()) {
                putEdgeReplace(itemMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<TagKey<Fluid>, RestrictionFlags> e : def.fluidTags().entrySet()) {
                for (Identifier fluidId : catalog.fluidsInTag(e.getKey().location())) {
                    putEdgeOr(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.fluidNamespaces().entrySet()) {
                for (Identifier fluidId : catalog.fluidsInNamespace(e.getKey())) {
                    putEdgeOr(fluidMap, fluidId, stageId, e.getValue());
                }
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.fluids().entrySet()) {
                putEdgeReplace(fluidMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.chemicalTags().entrySet()) {
                for (Identifier chemicalId : catalog.chemicalsInTag(e.getKey())) {
                    putEdgeOr(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.chemicalNamespaces().entrySet()) {
                for (Identifier chemicalId : catalog.chemicalsInNamespace(e.getKey())) {
                    putEdgeOr(chemicalMap, chemicalId, stageId, e.getValue());
                }
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.chemicals().entrySet()) {
                putEdgeReplace(chemicalMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.recipes().entrySet()) {
                putEdgeOr(recipeMap, e.getKey(), stageId, e.getValue());
            }

            for (Map.Entry<Identifier, RestrictionFlags> e : def.dimensionTags().entrySet()) {
                for (Identifier dimensionId : catalog.dimensionsInTag(e.getKey())) {
                    putEdgeOr(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<String, RestrictionFlags> e : def.dimensionNamespaces().entrySet()) {
                for (Identifier dimensionId : catalog.dimensionsInNamespace(e.getKey())) {
                    putEdgeOr(dimensionMap, dimensionId, stageId, e.getValue());
                }
            }
            for (Map.Entry<Identifier, RestrictionFlags> e : def.dimensions().entrySet()) {
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
            Map<Identifier, Map<Identifier, RestrictionFlags>> map,
            Identifier contentId,
            Identifier stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).merge(stageId, f, RestrictionFlags::or);
    }

    /** Explicit id replaces any prior flags for this stage (overrides tag / namespace). */
    private static void putEdgeReplace(
            Map<Identifier, Map<Identifier, RestrictionFlags>> map,
            Identifier contentId,
            Identifier stageId,
            RestrictionFlags flags
    ) {
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        map.computeIfAbsent(contentId, k -> new LinkedHashMap<>()).put(stageId, f);
    }

    public Map<Identifier, Set<Identifier>> itemStagesIndexView() {
        return indices.itemStages();
    }

    public Map<Identifier, Set<Identifier>> fluidStagesIndexView() {
        return indices.fluidStages();
    }

    public Map<Identifier, Set<Identifier>> chemicalStagesIndexView() {
        return indices.chemicalStages();
    }

    public Map<Identifier, Set<Identifier>> recipeStagesIndexView() {
        return indices.recipeStages();
    }

    public Map<Identifier, Set<Identifier>> dimensionStagesIndexView() {
        return indices.dimensionStages();
    }

    public Map<Identifier, Set<Identifier>> itemJeiStagesIndexView() {
        return indices.itemJeiStages();
    }

    public Map<Identifier, Set<Identifier>> fluidJeiStagesIndexView() {
        return indices.fluidJeiStages();
    }

    public Map<Identifier, Set<Identifier>> chemicalJeiStagesIndexView() {
        return indices.chemicalJeiStages();
    }

    public Map<Identifier, Set<Identifier>> recipeJeiStagesIndexView() {
        return indices.recipeJeiStages();
    }

    public Map<Identifier, Set<Identifier>> fluidTransferStagesIndexView() {
        return indices.fluidTransferStages();
    }

    public Map<Identifier, Set<Identifier>> chemicalTransferStagesIndexView() {
        return indices.chemicalTransferStages();
    }

    public synchronized Map<Identifier, StageDefinition> allDefinitions() {
        return allDefinitionsLocked();
    }

    private Map<Identifier, StageDefinition> allDefinitionsLocked() {
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

    public boolean isItemLocked(Identifier itemId, Set<Identifier> ownedStages) {
        if (itemId == null) {
            return false;
        }
        return LockRules.isLocked(indices.itemStages().get(itemId), ownedStages);
    }

    public boolean isFluidLocked(Identifier fluidKindId, Set<Identifier> ownedStages) {
        if (fluidKindId == null) {
            return false;
        }
        return LockRules.isLocked(indices.fluidStages().get(fluidKindId), ownedStages);
    }

    public boolean isChemicalLocked(Identifier chemicalId, Set<Identifier> ownedStages) {
        if (chemicalId == null) {
            return false;
        }
        return LockRules.isLocked(indices.chemicalStages().get(chemicalId), ownedStages);
    }

    public boolean isRecipeLocked(Identifier recipeId, Set<Identifier> ownedStages) {
        if (recipeId == null) {
            return false;
        }
        return LockRules.isLocked(indices.recipeStages().get(recipeId), ownedStages);
    }

    public boolean isDimensionLocked(Identifier dimensionId, Set<Identifier> ownedStages) {
        if (dimensionId == null) {
            return false;
        }
        return LockRules.isLocked(indices.dimensionStages().get(dimensionId), ownedStages);
    }

    public boolean isItemRestricted(
            Identifier itemId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(indices.itemEdges().get(itemId), ownedStages, restriction);
    }

    public boolean isFluidRestricted(
            Identifier fluidKindId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(indices.fluidEdges().get(fluidKindId), ownedStages, restriction);
    }

    public boolean isChemicalRestricted(
            Identifier chemicalId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(indices.chemicalEdges().get(chemicalId), ownedStages, restriction);
    }

    public boolean isRecipeRestricted(
            Identifier recipeId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(indices.recipeEdges().get(recipeId), ownedStages, restriction);
    }

    public boolean isDimensionRestricted(
            Identifier dimensionId, Set<Identifier> ownedStages, Restriction restriction) {
        return isRestricted(indices.dimensionEdges().get(dimensionId), ownedStages, restriction);
    }

    /**
     * Item restriction including the filled-bucket rule: a bucket counts as restricted while its fluid is.
     *
     * @param bucketFluidId fluid kind held by the item when it is a filled bucket, else {@code null}
     */
    public boolean isItemOrFluidBucketRestricted(
            Identifier itemId,
            @Nullable Identifier bucketFluidId,
            Set<Identifier> ownedStages,
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

    private static Map<Identifier, Set<Identifier>> flagIndexFrom(
            Map<Identifier, Map<Identifier, RestrictionFlags>> edges,
            Restriction restriction
    ) {
        if (edges.isEmpty() || restriction == null) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> out = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Map<Identifier, RestrictionFlags>> e : edges.entrySet()) {
            LinkedHashSet<Identifier> stages = new LinkedHashSet<>();
            for (Map.Entry<Identifier, RestrictionFlags> edge : e.getValue().entrySet()) {
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
