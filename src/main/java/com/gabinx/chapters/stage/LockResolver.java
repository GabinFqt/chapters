package com.gabinx.chapters.stage;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class LockResolver {
    private LockResolver() {
    }

    public static boolean isLocked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return StageManager.get().isItemLocked(EffectiveStages.snapshot(player), stack);
    }

    public static boolean isFluidLocked(ServerPlayer player, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return StageManager.get().isFluidLocked(EffectiveStages.snapshot(player), stack);
    }

    /**
     * Mekanism chemical registry key (e.g. {@code mekanism:hydrogen}). Only meaningful when Mekanism is installed.
     */
    public static boolean isChemicalLocked(ServerPlayer player, ResourceLocation chemicalRegistryKey) {
        if (chemicalRegistryKey == null) {
            return false;
        }

        return StageManager.get().isChemicalLocked(EffectiveStages.snapshot(player), chemicalRegistryKey);
    }

    public static boolean isRecipeLocked(ServerPlayer player, ResourceLocation recipeHolderId) {
        if (recipeHolderId == null) {
            return false;
        }

        return StageManager.get().isRecipeLocked(EffectiveStages.snapshot(player), recipeHolderId);
    }
}
