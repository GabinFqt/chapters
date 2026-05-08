package com.gabinx.stagecraft.compat.emi;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class StagecraftEmiPlugin {
    private StagecraftEmiPlugin() {
    }

    /** Reserved for EMI parity; locking state is handled via {@link com.gabinx.stagecraft.compat.RecipeViewerCompat}. */
    public static void onLockedIngredientsChanged(Set<ResourceLocation> lockedItems, Set<ResourceLocation> lockedFluids) {
        // no-op
    }
}
