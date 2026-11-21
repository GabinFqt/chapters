package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.logic.ClientRefreshCoalescer;
import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.stage.ClientStageCache;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeViewerCompat {
    private static final ClientRefreshCoalescer COALESCER = new ClientRefreshCoalescer(RecipeViewerCompat::applyNow);

    private RecipeViewerCompat() {
    }

    /** Mark the recipe viewer dirty; applied once at the end of the client tick. */
    public static void refresh() {
        COALESCER.schedule();
    }

    /** Apply any scheduled refresh. Called from the client tick hook. */
    public static void flushScheduled() {
        COALESCER.flush();
    }

    private static void applyNow() {
        Set<ResourceLocation> active = ClientStageCache.snapshot();
        Set<ResourceLocation> lockedFluids = ClientStageView.computeFlagRestricted(
                ClientStageIndices.fluidsView(), ClientStageIndices.fluidJeiView(), active);
        Set<ResourceLocation> lockedItems = ClientStageView.computeFlagRestricted(
                ClientStageIndices.itemsView(), ClientStageIndices.itemJeiView(), active);
        for (ResourceLocation fluidId : lockedFluids) {
            lockedItems.addAll(RegistryContentCatalog.INSTANCE.bucketItemsForFluid(fluidId));
        }
        Set<ResourceLocation> lockedChemicals = ClientStageView.computeFlagRestricted(
                ClientStageIndices.chemicalsView(), ClientStageIndices.chemicalJeiView(), active);
        Set<ResourceLocation> lockedRecipes = ClientStageView.computeFlagRestricted(
                ClientStageIndices.recipesView(), ClientStageIndices.recipeJeiView(), active);
        long started = System.nanoTime();
        if (ModList.get().isLoaded("jei")) {
            ChaptersJeiPlugin.onLockedIngredientsChanged(
                    new LinkedHashSet<>(lockedItems),
                    lockedFluids,
                    lockedChemicals,
                    lockedRecipes
            );
        }
        Chapters.LOGGER.debug(
                "Recipe viewer refresh: locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}, {} ms",
                lockedItems.size(),
                lockedFluids.size(),
                lockedChemicals.size(),
                lockedRecipes.size(),
                (System.nanoTime() - started) / 1_000_000L
        );
    }
}
