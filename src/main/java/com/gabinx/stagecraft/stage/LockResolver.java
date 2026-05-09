package com.gabinx.stagecraft.stage;

import com.gabinx.stagecraft.StagecraftRegistries;
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

        PlayerStages stages = player.getData(StagecraftRegistries.PLAYER_STAGES.get());
        return StageManager.get().isItemLocked(stages, stack);
    }

    public static boolean isFluidLocked(ServerPlayer player, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        PlayerStages stages = player.getData(StagecraftRegistries.PLAYER_STAGES.get());
        return StageManager.get().isFluidLocked(stages, stack);
    }

    /**
     * Mekanism chemical registry key (e.g. {@code mekanism:hydrogen}). Only meaningful when Mekanism is installed.
     */
    public static boolean isChemicalLocked(ServerPlayer player, ResourceLocation chemicalRegistryKey) {
        if (chemicalRegistryKey == null) {
            return false;
        }

        PlayerStages stages = player.getData(StagecraftRegistries.PLAYER_STAGES.get());
        return StageManager.get().isChemicalLocked(stages, chemicalRegistryKey);
    }
}
