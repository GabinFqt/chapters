package com.gabinx.stagecraft.compat;

import com.gabinx.stagecraft.Stagecraft;
import com.gabinx.stagecraft.compat.emi.StagecraftEmiPlugin;
import com.gabinx.stagecraft.compat.jei.StagecraftJeiPlugin;
import com.gabinx.stagecraft.compat.rei.StagecraftReiPlugin;
import com.gabinx.stagecraft.stage.ClientStageCache;
import com.gabinx.stagecraft.stage.StageManager;
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
        Stagecraft.LOGGER.debug(
                "Recipe viewer refresh — locked items: {}, locked fluids: {}",
                lockedItems.size(),
                lockedFluids.size()
        );
        if (ModList.get().isLoaded("jei")) {
            StagecraftJeiPlugin.onLockedIngredientsChanged(lockedItems, lockedFluids);
        }
        if (ModList.get().isLoaded("roughlyenoughitems")) {
            StagecraftReiPlugin.onLockedIngredientsChanged(lockedItems, lockedFluids);
        }
        if (ModList.get().isLoaded("emi")) {
            StagecraftEmiPlugin.onLockedIngredientsChanged(lockedItems, lockedFluids);
        }
    }

    /**
     * Items the client-side player cannot use (mirrors {@link com.gabinx.stagecraft.stage.LockResolver}: need at least one
     * stage among every definition that mentions the stack).
     */
    private static Set<ResourceLocation> getLockedItemIds() {
        Set<ResourceLocation> activeStages = ClientStageCache.snapshot();
        Set<ResourceLocation> locked = new LinkedHashSet<>();

        Map<ResourceLocation, Set<ResourceLocation>> index = StageManager.get().itemStagesIndexView();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : index.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            Set<ResourceLocation> definingStages = entry.getValue();
            boolean unlocked = false;
            for (ResourceLocation required : definingStages) {
                if (activeStages.contains(required)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(itemId);
            }
        }
        return locked;
    }

    private static Set<ResourceLocation> getLockedFluidIds() {
        Set<ResourceLocation> activeStages = ClientStageCache.snapshot();
        Set<ResourceLocation> locked = new LinkedHashSet<>();

        Map<ResourceLocation, Set<ResourceLocation>> index = StageManager.get().fluidStagesIndexView();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : index.entrySet()) {
            ResourceLocation fluidId = entry.getKey();
            Set<ResourceLocation> definingStages = entry.getValue();
            boolean unlocked = false;
            for (ResourceLocation required : definingStages) {
                if (activeStages.contains(required)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(fluidId);
            }
        }
        return locked;
    }
}
