package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory catalog for unit tests (and any non-registry context).
 */
public final class FixedContentCatalog implements ContentCatalog {
    private final Map<Identifier, Set<Identifier>> itemTags = new HashMap<>();
    private final Map<String, Set<Identifier>> itemNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> fluidTags = new HashMap<>();
    private final Map<String, Set<Identifier>> fluidNamespaces = new HashMap<>();
    private final Map<Identifier, Set<Identifier>> chemicalTags = new HashMap<>();
    private final Map<String, Set<Identifier>> chemicalNamespaces = new HashMap<>();
    private final Map<Identifier, Identifier> fluidToBucket = new HashMap<>();

    public FixedContentCatalog addItem(Identifier itemId) {
        itemNamespaces.computeIfAbsent(itemId.getNamespace(), k -> new LinkedHashSet<>()).add(itemId);
        return this;
    }

    public FixedContentCatalog addItemTag(Identifier tagId, Identifier... itemIds) {
        Set<Identifier> set = itemTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (Identifier id : itemIds) {
            set.add(id);
            addItem(id);
        }
        return this;
    }

    public FixedContentCatalog addFluid(Identifier fluidId) {
        fluidNamespaces.computeIfAbsent(fluidId.getNamespace(), k -> new LinkedHashSet<>()).add(fluidId);
        return this;
    }

    public FixedContentCatalog addFluidTag(Identifier tagId, Identifier... fluidIds) {
        Set<Identifier> set = fluidTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (Identifier id : fluidIds) {
            set.add(id);
            addFluid(id);
        }
        return this;
    }

    public FixedContentCatalog addChemical(Identifier chemicalId) {
        chemicalNamespaces.computeIfAbsent(chemicalId.getNamespace(), k -> new LinkedHashSet<>()).add(chemicalId);
        return this;
    }

    public FixedContentCatalog addChemicalTag(Identifier tagId, Identifier... chemicalIds) {
        Set<Identifier> set = chemicalTags.computeIfAbsent(tagId, k -> new LinkedHashSet<>());
        for (Identifier id : chemicalIds) {
            set.add(id);
            addChemical(id);
        }
        return this;
    }

    public FixedContentCatalog setBucket(Identifier fluidKindId, Identifier bucketItemId) {
        fluidToBucket.put(fluidKindId, bucketItemId);
        addItem(bucketItemId);
        addFluid(fluidKindId);
        return this;
    }

    @Override
    public Set<Identifier> itemsInTag(Identifier tagId) {
        return copy(itemTags.get(tagId));
    }

    @Override
    public Set<Identifier> itemsInNamespace(String namespace) {
        return copy(itemNamespaces.get(namespace));
    }

    @Override
    public Set<Identifier> fluidsInTag(Identifier tagId) {
        return copy(fluidTags.get(tagId));
    }

    @Override
    public Set<Identifier> fluidsInNamespace(String namespace) {
        return copy(fluidNamespaces.get(namespace));
    }

    @Override
    public Set<Identifier> chemicalsInTag(Identifier tagId) {
        return copy(chemicalTags.get(tagId));
    }

    @Override
    public Set<Identifier> chemicalsInNamespace(String namespace) {
        return copy(chemicalNamespaces.get(namespace));
    }

    @Override
    public Set<Identifier> bucketItemsForFluid(Identifier fluidKindId) {
        Identifier bucket = fluidToBucket.get(fluidKindId);
        return bucket == null ? Set.of() : Set.of(bucket);
    }

    private static Set<Identifier> copy(Set<Identifier> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
