package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.stage.ClientStageCache;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeViewerCompat {
    private RecipeViewerCompat() {
    }

    public static void refresh() {
        Set<ResourceLocation> active = ClientStageCache.snapshot();
        Set<ResourceLocation> lockedFluids = ClientStageView.computeLocked(ClientStageIndices.fluidJeiView(), active);
        Set<ResourceLocation> lockedItems = ClientStageView.computeLocked(ClientStageIndices.itemJeiView(), active);
        for (ResourceLocation fluidId : lockedFluids) {
            lockedItems.addAll(RegistryContentCatalog.INSTANCE.bucketItemsForFluid(fluidId));
        }
        Set<ResourceLocation> lockedChemicals = ClientStageView.computeLocked(ClientStageIndices.chemicalJeiView(), active);
        Set<ResourceLocation> lockedRecipes = ClientStageView.computeLocked(ClientStageIndices.recipeJeiView(), active);
        Chapters.LOGGER.debug(
                "Recipe viewer refresh — locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}",
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
