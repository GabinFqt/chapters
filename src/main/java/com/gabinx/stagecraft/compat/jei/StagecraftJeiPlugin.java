package com.gabinx.stagecraft.compat.jei;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class StagecraftJeiPlugin {
    private StagecraftJeiPlugin() {
    }

    public static void onLockedIngredientsChanged(
            Set<ResourceLocation> lockedItems,
            Set<ResourceLocation> lockedFluids,
            Set<ResourceLocation> lockedChemicals
    ) {
        StagecraftJeiModPlugin.applyLocked(lockedItems, lockedFluids, lockedChemicals);
    }
}
