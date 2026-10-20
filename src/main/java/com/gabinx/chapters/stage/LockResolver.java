package com.gabinx.chapters.stage;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import com.gabinx.chapters.logic.LockRules;
import com.gabinx.chapters.logic.Restriction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Set;

public final class LockResolver {
    private LockResolver() {
    }

    /** True if the stack is gated by any stage (flags ignored). */
    public static boolean isLocked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (StageManager.get().isItemLocked(EffectiveStages.snapshot(player), stack)) {
            return true;
        }
        return isFluidBucketLocked(player, stack);
    }

    public static boolean isRestricted(ServerPlayer player, ItemStack stack, Restriction restriction) {
        if (stack.isEmpty() || restriction == null) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Set<ResourceLocation> owned = EffectiveStages.of(player);
        if (StageManager.get().book().isItemRestricted(itemId, owned, restriction)) {
            return true;
        }
        return isFluidBucketRestricted(player, stack, restriction);
    }

    public static boolean isFluidLocked(ServerPlayer player, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return StageManager.get().isFluidLocked(EffectiveStages.snapshot(player), stack);
    }

    public static boolean isFluidRestricted(ServerPlayer player, FluidStack stack, Restriction restriction) {
        if (stack == null || stack.isEmpty() || restriction == null) {
            return false;
        }
        ResourceLocation kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        return StageManager.get().book().isFluidRestricted(kind, EffectiveStages.of(player), restriction);
    }

    /**
     * Server uses {@link EffectiveStages} + {@link StageManager}; client prediction uses the mirrored
     * {@link ClientStageIndices} / {@link ClientStageCache} so empty-bucket fill does not visually succeed.
     */
    public static boolean isFluidLocked(Player player, Fluid fluid) {
        if (player == null || fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        FluidStack stack = new FluidStack(fluid, 1000);
        if (stack.isEmpty()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide()) {
            return isFluidRestricted(serverPlayer, stack, Restriction.TRANSFER);
        }
        if (player.level().isClientSide()) {
            return isFluidLockedClient(stack);
        }
        return false;
    }

    public static boolean isFluidBucketLocked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BucketItem bucket)) {
            return false;
        }
        Fluid filled = bucket.content;
        if (filled == null || filled == Fluids.EMPTY) {
            return false;
        }
        return isFluidLocked(player, filled);
    }

    public static boolean isFluidBucketRestricted(
            ServerPlayer player, ItemStack stack, Restriction restriction) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BucketItem bucket) || restriction == null) {
            return false;
        }
        Fluid filled = bucket.content;
        if (filled == null || filled == Fluids.EMPTY) {
            return false;
        }
        return isFluidRestricted(player, new FluidStack(filled, 1000), restriction);
    }

    private static boolean isFluidLockedClient(FluidStack stack) {
        ResourceLocation kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        if (kind == null) {
            return false;
        }
        Set<ResourceLocation> defining = ClientStageIndices.fluidsView().get(kind);
        if (!LockRules.isLocked(defining, ClientStageCache.snapshot())) {
            return false;
        }
        return ClientStageIndices.fluidTransferView().containsKey(kind);
    }

    public static boolean isChemicalLocked(ServerPlayer player, ResourceLocation chemicalRegistryKey) {
        if (chemicalRegistryKey == null) {
            return false;
        }
        return StageManager.get().isChemicalLocked(EffectiveStages.snapshot(player), chemicalRegistryKey);
    }

    public static boolean isChemicalRestricted(
            ServerPlayer player, ResourceLocation chemicalRegistryKey, Restriction restriction) {
        if (chemicalRegistryKey == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isChemicalRestricted(chemicalRegistryKey, EffectiveStages.of(player), restriction);
    }

    public static boolean isRecipeLocked(ServerPlayer player, ResourceLocation recipeHolderId) {
        if (recipeHolderId == null) {
            return false;
        }
        return StageManager.get().isRecipeLocked(EffectiveStages.snapshot(player), recipeHolderId);
    }

    public static boolean isRecipeRestricted(
            ServerPlayer player, ResourceLocation recipeHolderId, Restriction restriction) {
        if (recipeHolderId == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isRecipeRestricted(recipeHolderId, EffectiveStages.of(player), restriction);
    }

    public static boolean isDimensionLocked(ServerPlayer player, ResourceLocation dimensionId) {
        if (dimensionId == null) {
            return false;
        }
        return StageManager.get().isDimensionLocked(EffectiveStages.snapshot(player), dimensionId);
    }

    public static boolean isDimensionRestricted(
            ServerPlayer player, ResourceLocation dimensionId, Restriction restriction) {
        if (dimensionId == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isDimensionRestricted(dimensionId, EffectiveStages.of(player), restriction);
    }
}
