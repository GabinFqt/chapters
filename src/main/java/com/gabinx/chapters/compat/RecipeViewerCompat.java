package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.stage.ClientStageCache;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RecipeViewerCompat {
    private RecipeViewerCompat() {
    }

    public static void refresh() {
        Set<ResourceLocation> lockedItems = getLockedItemIds();
        Set<ResourceLocation> lockedFluids = getLockedFluidIds();
        Set<ResourceLocation> lockedChemicals = getLockedChemicalIds();
        Set<ResourceLocation> lockedRecipes = getLockedRecipeIds();
        Chapters.LOGGER.debug(
                "Recipe viewer refresh — locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}",
                lockedItems.size(),
                lockedFluids.size(),
                lockedChemicals.size(),
                lockedRecipes.size()
        );
        if (ModList.get().isLoaded("jei")) {
            ChaptersJeiPlugin.onLockedIngredientsChanged(lockedItems, lockedFluids, lockedChemicals, lockedRecipes);
        }
    }

    /**
     * Items the client-side player cannot use (mirrors {@link com.gabinx.chapters.stage.LockResolver}: need at least one
     * stage among every definition that mentions the stack).
     */
    private static Set<ResourceLocation> getLockedItemIds() {
        return computeLocked(ClientStageIndices.itemsView());
    }

    private static Set<ResourceLocation> getLockedFluidIds() {
        return computeLocked(ClientStageIndices.fluidsView());
    }

    private static Set<ResourceLocation> getLockedChemicalIds() {
        return computeLocked(ClientStageIndices.chemicalsView());
    }

    private static Set<ResourceLocation> getLockedRecipeIds() {
        return computeLocked(ClientStageIndices.recipesView());
    }

    private static Set<ResourceLocation> computeLocked(Map<ResourceLocation, Set<ResourceLocation>> index) {
        Set<ResourceLocation> activeStages = ClientStageCache.snapshot();
        Set<ResourceLocation> locked = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : index.entrySet()) {
            ResourceLocation id = entry.getKey();
            Set<ResourceLocation> definingStages = entry.getValue();
            boolean unlocked = false;
            for (ResourceLocation required : definingStages) {
                if (activeStages.contains(required)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(id);
            }
        }
        return locked;
    }
}
