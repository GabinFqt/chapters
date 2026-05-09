package com.gabinx.chapters.compat.jei;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class ChaptersJeiPlugin {
    private ChaptersJeiPlugin() {
    }

    public static void onLockedIngredientsChanged(
            Set<ResourceLocation> lockedItems,
            Set<ResourceLocation> lockedFluids,
            Set<ResourceLocation> lockedChemicals,
            Set<ResourceLocation> lockedRecipes
    ) {
        ChaptersJeiModPlugin.applyLocked(lockedItems, lockedFluids, lockedChemicals, lockedRecipes);
    }
}
