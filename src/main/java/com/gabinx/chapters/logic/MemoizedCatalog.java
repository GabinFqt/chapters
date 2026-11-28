package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Remembers each tag and namespace lookup for the length of one index rebuild. Namespace lookups scan a whole
 * registry, so stages that repeat {@code @mod} entries would otherwise scan it once per entry.
 */
final class MemoizedCatalog implements ContentCatalog {
    private final ContentCatalog source;
    private final Map<ResourceLocation, Set<ResourceLocation>> itemTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> itemNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> fluidTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> fluidNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> chemicalTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> chemicalNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> dimensionTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> dimensionNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> buckets = new HashMap<>();

    MemoizedCatalog(ContentCatalog source) {
        this.source = source;
    }

    private static <K> Set<ResourceLocation> memo(
            Map<K, Set<ResourceLocation>> cache, K key, Function<K, Set<ResourceLocation>> lookup) {
        Set<ResourceLocation> hit = cache.get(key);
        if (hit == null) {
            hit = Set.copyOf(lookup.apply(key));
            cache.put(key, hit);
        }
        return hit;
    }

    @Override
    public Set<ResourceLocation> itemsInTag(ResourceLocation tagId) {
        return memo(itemTags, tagId, source::itemsInTag);
    }

    @Override
    public Set<ResourceLocation> itemsInNamespace(String namespace) {
        return memo(itemNamespaces, namespace, source::itemsInNamespace);
    }

    @Override
    public Set<ResourceLocation> fluidsInTag(ResourceLocation tagId) {
        return memo(fluidTags, tagId, source::fluidsInTag);
    }

    @Override
    public Set<ResourceLocation> fluidsInNamespace(String namespace) {
        return memo(fluidNamespaces, namespace, source::fluidsInNamespace);
    }

    @Override
    public Set<ResourceLocation> chemicalsInTag(ResourceLocation tagId) {
        return memo(chemicalTags, tagId, source::chemicalsInTag);
    }

    @Override
    public Set<ResourceLocation> chemicalsInNamespace(String namespace) {
        return memo(chemicalNamespaces, namespace, source::chemicalsInNamespace);
    }

    @Override
    public Set<ResourceLocation> dimensionsInTag(ResourceLocation tagId) {
        return memo(dimensionTags, tagId, source::dimensionsInTag);
    }

    @Override
    public Set<ResourceLocation> dimensionsInNamespace(String namespace) {
        return memo(dimensionNamespaces, namespace, source::dimensionsInNamespace);
    }

    @Override
    public Set<ResourceLocation> bucketItemsForFluid(ResourceLocation fluidKindId) {
        return memo(buckets, fluidKindId, source::bucketItemsForFluid);
    }
}
