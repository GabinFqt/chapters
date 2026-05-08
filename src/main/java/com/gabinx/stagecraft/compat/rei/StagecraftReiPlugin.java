package com.gabinx.stagecraft.compat.rei;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class StagecraftReiPlugin {
    private StagecraftReiPlugin() {
    }

    /** Reserved for REI parity; locking state is handled via {@link com.gabinx.stagecraft.compat.RecipeViewerCompat}. */
    public static void onLockedIngredientsChanged(Set<ResourceLocation> lockedItems, Set<ResourceLocation> lockedFluids) {
        // no-op
    }
}
