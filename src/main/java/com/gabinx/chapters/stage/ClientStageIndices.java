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

    public static synchronized void replace(
            Map<Identifier, Set<Identifier>> nextItems,
            Map<Identifier, Set<Identifier>> nextFluids,
            Map<Identifier, Set<Identifier>> nextChemicals,
            Map<Identifier, Set<Identifier>> nextRecipes,
            Map<Identifier, Set<Identifier>> nextItemJei,
            Map<Identifier, Set<Identifier>> nextFluidJei,
            Map<Identifier, Set<Identifier>> nextChemicalJei,
            Map<Identifier, Set<Identifier>> nextRecipeJei
    ) {
        ClientStageMirror.VIEW.replaceIndices(
                nextItems,
                nextFluids,
                nextChemicals,
                nextRecipes,
                nextItemJei,
                nextFluidJei,
                nextChemicalJei,
                nextRecipeJei);
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

    public static Map<Identifier, Set<Identifier>> itemJeiView() {
        return ClientStageMirror.VIEW.itemJeiView();
    }

    public static Map<Identifier, Set<Identifier>> fluidJeiView() {
        return ClientStageMirror.VIEW.fluidJeiView();
    }

    public static Map<Identifier, Set<Identifier>> chemicalJeiView() {
        return ClientStageMirror.VIEW.chemicalJeiView();
    }

    public static Map<Identifier, Set<Identifier>> recipeJeiView() {
        return ClientStageMirror.VIEW.recipeJeiView();
    }
}
