package com.gabinx.stagecraft.compat;

import com.gabinx.stagecraft.Stagecraft;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class RecipeViewerBootstrap {
    private RecipeViewerBootstrap() {
    }

    public static void logDetectedRecipeViewers() {
        List<String> loaded = new ArrayList<>(3);
        if (ModList.get().isLoaded("jei")) {
            loaded.add("JEI");
        }
        if (ModList.get().isLoaded("roughlyenoughitems")) {
            loaded.add("REI");
        }
        if (ModList.get().isLoaded("emi")) {
            loaded.add("EMI");
        }
        if (!loaded.isEmpty()) {
            Stagecraft.LOGGER.info("Recipe viewer compat detected: {}", String.join(", ", loaded));
        }
    }
}
