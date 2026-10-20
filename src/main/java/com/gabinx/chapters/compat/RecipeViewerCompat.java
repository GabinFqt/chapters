package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.stage.ClientStageCache;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeViewerCompat {
    private RecipeViewerCompat() {
    }

    public static void refresh() {
        Set<Identifier> active = ClientStageCache.snapshot();
        Set<Identifier> lockedFluids = ClientStageView.computeFlagRestricted(
                ClientStageIndices.fluidsView(), ClientStageIndices.fluidJeiView(), active);
        Set<Identifier> lockedItems = ClientStageView.computeFlagRestricted(
                ClientStageIndices.itemsView(), ClientStageIndices.itemJeiView(), active);
        for (Identifier fluidId : lockedFluids) {
            lockedItems.addAll(RegistryContentCatalog.INSTANCE.bucketItemsForFluid(fluidId));
        }
        Set<Identifier> lockedChemicals = ClientStageView.computeFlagRestricted(
                ClientStageIndices.chemicalsView(), ClientStageIndices.chemicalJeiView(), active);
        Set<Identifier> lockedRecipes = ClientStageView.computeFlagRestricted(
                ClientStageIndices.recipesView(), ClientStageIndices.recipeJeiView(), active);
        Chapters.LOGGER.debug(
                "Recipe viewer refresh: locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}",
                lockedItems.size(),
                lockedFluids.size(),
                lockedChemicals.size(),
                lockedRecipes.size()
        );
        if (ModList.get().isLoaded("jei")) {
            ChaptersJeiPlugin.onLockedIngredientsChanged(
                    new LinkedHashSet<>(lockedItems),
                    lockedFluids,
                    lockedChemicals,
                    lockedRecipes
            );
        }
    }
}
