package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

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
    private final Map<Identifier, Set<Identifier>> itemTags = new HashMap<>();
    private final Map<String, Set<Identifier>> itemNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> fluidTags = new HashMap<>();
    private final Map<String, Set<Identifier>> fluidNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> chemicalTags = new HashMap<>();
    private final Map<String, Set<Identifier>> chemicalNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> dimensionTags = new HashMap<>();
    private final Map<String, Set<Identifier>> dimensionNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> buckets = new HashMap<>();

    MemoizedCatalog(ContentCatalog source) {
        this.source = source;
    }

    private static <K> Set<Identifier> memo(
            Map<K, Set<Identifier>> cache, K key, Function<K, Set<Identifier>> lookup) {
        Set<Identifier> hit = cache.get(key);
        if (hit == null) {
            hit = Set.copyOf(lookup.apply(key));
            cache.put(key, hit);
        }
        return hit;
    }

    @Override
    public Set<Identifier> itemsInTag(Identifier tagId) {
        return memo(itemTags, tagId, source::itemsInTag);
    }

    @Override
    public Set<Identifier> itemsInNamespace(String namespace) {
        return memo(itemNamespaces, namespace, source::itemsInNamespace);
    }

    @Override
    public Set<Identifier> fluidsInTag(Identifier tagId) {
        return memo(fluidTags, tagId, source::fluidsInTag);
    }

    @Override
    public Set<Identifier> fluidsInNamespace(String namespace) {
        return memo(fluidNamespaces, namespace, source::fluidsInNamespace);
    }

    @Override
    public Set<Identifier> chemicalsInTag(Identifier tagId) {
        return memo(chemicalTags, tagId, source::chemicalsInTag);
    }

    @Override
    public Set<Identifier> chemicalsInNamespace(String namespace) {
        return memo(chemicalNamespaces, namespace, source::chemicalsInNamespace);
    }

    @Override
    public Set<Identifier> dimensionsInTag(Identifier tagId) {
        return memo(dimensionTags, tagId, source::dimensionsInTag);
    }

    @Override
    public Set<Identifier> dimensionsInNamespace(String namespace) {
        return memo(dimensionNamespaces, namespace, source::dimensionsInNamespace);
    }

    @Override
    public Set<Identifier> bucketItemsForFluid(Identifier fluidKindId) {
        return memo(buckets, fluidKindId, source::bucketItemsForFluid);
    }
}
