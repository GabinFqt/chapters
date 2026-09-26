package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshots of the four stage-locking indices, mirrored on the logical client.
 * Delegates storage to a shared {@link ClientStageView} used also by {@link ClientStageCache}.
 */
public final class ClientStageIndices {
    private ClientStageIndices() {
    }

    public static synchronized void replace(
            Map<Identifier, Set<Identifier>> nextItems,
            Map<Identifier, Set<Identifier>> nextFluids,
            Map<Identifier, Set<Identifier>> nextChemicals,
            Map<Identifier, Set<Identifier>> nextRecipes
    ) {
        ClientStageMirror.VIEW.replaceIndices(nextItems, nextFluids, nextChemicals, nextRecipes);
    }

    public static Map<Identifier, Set<Identifier>> itemsView() {
        return ClientStageMirror.VIEW.itemsView();
    }

    public static Map<Identifier, Set<Identifier>> fluidsView() {
        return ClientStageMirror.VIEW.fluidsView();
    }

    public static Map<Identifier, Set<Identifier>> chemicalsView() {
        return ClientStageMirror.VIEW.chemicalsView();
    }

    public static Map<Identifier, Set<Identifier>> recipesView() {
        return ClientStageMirror.VIEW.recipesView();
    }
}
