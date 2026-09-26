package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Resolves tags and namespaces into concrete registry ids so {@link StageBook} can build lock
 * indices without depending on live registries in unit tests.
 */
public interface ContentCatalog {
    Set<ResourceLocation> itemsInTag(ResourceLocation tagId);

    Set<ResourceLocation> itemsInNamespace(String namespace);

    Set<ResourceLocation> fluidsInTag(ResourceLocation tagId);

    Set<ResourceLocation> fluidsInNamespace(String namespace);

    Set<ResourceLocation> chemicalsInTag(ResourceLocation tagId);

    Set<ResourceLocation> chemicalsInNamespace(String namespace);

    Set<ResourceLocation> dimensionsInTag(ResourceLocation tagId);

    Set<ResourceLocation> dimensionsInNamespace(String namespace);

    /** Item id of the filled bucket for this fluid kind, or empty if none. */
    Set<ResourceLocation> bucketItemsForFluid(ResourceLocation fluidKindId);
}
