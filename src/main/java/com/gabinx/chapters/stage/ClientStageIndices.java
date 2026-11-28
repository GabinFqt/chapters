package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.ContentCatalog;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshots of the stage-locking indices, mirrored on the logical client.
 * Delegates storage to a shared {@link ClientStageView} used also by {@link ClientStageCache}.
 */
public final class ClientStageIndices {
    private ClientStageIndices() {
    }

    public static synchronized void replace(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes,
            Map<ResourceLocation, Set<ResourceLocation>> nextItemJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluidJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicalJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipeJei,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluidTransfer,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicalTransfer
    ) {
        ClientStageMirror.VIEW.replaceIndices(
                nextItems,
                nextFluids,
                nextChemicals,
                nextRecipes,
                nextItemJei,
                nextFluidJei,
                nextChemicalJei,
                nextRecipeJei,
                nextFluidTransfer,
                nextChemicalTransfer);
    }

    /** What the recipe viewer hides for the mirrored stages; see {@link ClientStageView#hidden}. */
    public static ClientStageView.Hidden hidden(ContentCatalog catalog) {
        return ClientStageMirror.VIEW.hidden(catalog);
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

    public static Map<ResourceLocation, Set<ResourceLocation>> itemJeiView() {
        return ClientStageMirror.VIEW.itemJeiView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> fluidJeiView() {
        return ClientStageMirror.VIEW.fluidJeiView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiView() {
        return ClientStageMirror.VIEW.chemicalJeiView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> recipeJeiView() {
        return ClientStageMirror.VIEW.recipeJeiView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> fluidTransferView() {
        return ClientStageMirror.VIEW.fluidTransferView();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> chemicalTransferView() {
        return ClientStageMirror.VIEW.chemicalTransferView();
    }
}
