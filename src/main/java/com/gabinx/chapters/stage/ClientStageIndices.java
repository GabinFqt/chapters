package com.gabinx.chapters.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshots of the four stage-locking indices, mirrored on the logical client.
 * Delegates storage to a shared {@link com.gabinx.chapters.logic.ClientStageView}.
 */
public final class ClientStageIndices {
    private ClientStageIndices() {
    }

    public static synchronized void replace(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes
    ) {
        ClientStageMirror.VIEW.replaceIndices(nextItems, nextFluids, nextChemicals, nextRecipes);
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> itemsView() {
        return ClientStageMirror.VIEW.itemsView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> fluidsView() {
        return ClientStageMirror.VIEW.fluidsView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> chemicalsView() {
        return ClientStageMirror.VIEW.chemicalsView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> recipesView() {
        return ClientStageMirror.VIEW.recipesView();
    }
}
