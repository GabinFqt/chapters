package com.gabinx.stagecraft.compat;

import com.gabinx.stagecraft.Stagecraft;
import net.neoforged.fml.ModList;

public final class RecipeViewerBootstrap {
    private RecipeViewerBootstrap() {
    }

    public static void logDetectedRecipeViewers() {
        if (ModList.get().isLoaded("jei")) {
            Stagecraft.LOGGER.info("Recipe viewer compat detected: JEI");
        }
    }
}
