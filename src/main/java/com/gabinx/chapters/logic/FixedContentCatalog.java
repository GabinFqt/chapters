package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory catalog for unit tests (and any non-registry context).
 */
public final class FixedContentCatalog implements ContentCatalog {
    private final Map<ResourceLocation, Set<ResourceLocation>> itemTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> itemNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> fluidTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> fluidNamespaces = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> chemicalTags = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> chemicalNamespaces = new HashMap<>();
    private final Map<ResourceLocation, ResourceLocation> fluidToBucket = new HashMap<>();

    public FixedContentCatalog addItem(ResourceLocation itemId) {
        itemNamespaces.computeIfAbsent(itemId.getNamespace(), k -> new LinkedHashSet<>()).add(itemId);
        return this;
    }

    public FixedContentCatalog addItemTag(ResourceLocation tagId, ResourceLocation... itemIds) {
        Set<ResourceLocation> set = itemTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (ResourceLocation id : itemIds) {
            set.add(id);
            addItem(id);
        }
        return this;
    }

    public FixedContentCatalog addFluid(ResourceLocation fluidId) {
        fluidNamespaces.computeIfAbsent(fluidId.getNamespace(), k -> new LinkedHashSet<>()).add(fluidId);
        return this;
    }

    public FixedContentCatalog addFluidTag(ResourceLocation tagId, ResourceLocation... fluidIds) {
        Set<ResourceLocation> set = fluidTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (ResourceLocation id : fluidIds) {
            set.add(id);
            addFluid(id);
        }
        return this;
    }

    public FixedContentCatalog addChemical(ResourceLocation chemicalId) {
        chemicalNamespaces.computeIfAbsent(chemicalId.getNamespace(), k -> new LinkedHashSet<>()).add(chemicalId);
        return this;
    }

    public FixedContentCatalog addChemicalTag(ResourceLocation tagId, ResourceLocation... chemicalIds) {
        Set<ResourceLocation> set = chemicalTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (ResourceLocation id : chemicalIds) {
            set.add(id);
            addChemical(id);
        }
        return this;
    }

    public FixedContentCatalog setBucket(ResourceLocation fluidKindId, ResourceLocation bucketItemId) {
        fluidToBucket.put(fluidKindId, bucketItemId);
        addItem(bucketItemId);
        addFluid(fluidKindId);
        return this;
    }

    @Override
    public Set<ResourceLocation> itemsInTag(ResourceLocation tagId) {
        return copy(itemTags.get(tagId));
    }

    @Override
    public Set<ResourceLocation> itemsInNamespace(String namespace) {
        return copy(itemNamespaces.get(namespace));
    }

    @Override
    public Set<ResourceLocation> fluidsInTag(ResourceLocation tagId) {
        return copy(fluidTags.get(tagId));
    }

    @Override
    public Set<ResourceLocation> fluidsInNamespace(String namespace) {
        return copy(fluidNamespaces.get(namespace));
    }

    @Override
    public Set<ResourceLocation> chemicalsInTag(ResourceLocation tagId) {
        return copy(chemicalTags.get(tagId));
    }

    @Override
    public Set<ResourceLocation> chemicalsInNamespace(String namespace) {
        return copy(chemicalNamespaces.get(namespace));
    }

    @Override
    public Set<ResourceLocation> bucketItemsForFluid(ResourceLocation fluidKindId) {
        ResourceLocation bucket = fluidToBucket.get(fluidKindId);
        return bucket == null ? Set.of() : Set.of(bucket);
    }

    private static Set<ResourceLocation> copy(Set<ResourceLocation> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
