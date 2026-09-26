package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Resolves tags and namespaces into concrete registry ids so {@link StageBook} can build lock
 * indices without depending on live registries in unit tests.
 */
public interface ContentCatalog {
    Set<Identifier> itemsInTag(Identifier tagId);

    Set<Identifier> itemsInNamespace(String namespace);

    Set<Identifier> fluidsInTag(Identifier tagId);

    Set<Identifier> fluidsInNamespace(String namespace);

    Set<Identifier> chemicalsInTag(Identifier tagId);

    Set<Identifier> chemicalsInNamespace(String namespace);

    /** Item id of the filled bucket for this fluid kind, or empty if none. */
    Set<Identifier> bucketItemsForFluid(Identifier fluidKindId);
}
