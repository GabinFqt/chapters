package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import net.neoforged.fml.ModList;

public final class RecipeViewerBootstrap {
    private RecipeViewerBootstrap() {
    }

    public static void logDetectedRecipeViewers() {
        if (ModList.get().isLoaded("jei")) {
            Chapters.LOGGER.info("Recipe viewer compat detected: JEI");
        }
    }
}
