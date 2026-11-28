package com.gabinx.chapters.stage;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import com.gabinx.chapters.logic.LockRules;
import com.gabinx.chapters.logic.Restriction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Server-side lock checks for a player, reading {@link EffectiveStages} and the live {@link StageManager} book.
 */
public final class LockResolver {
    private LockResolver() {
    }

    /** Item restriction, counting a filled bucket as restricted while its fluid is. */
    public static boolean isRestricted(ServerPlayer player, ItemStack stack, Restriction restriction) {
        if (stack.isEmpty() || restriction == null) {
            return false;
        }
        return isItemRestricted(stack.getItem(), EffectiveStages.view(player), restriction);
    }

    /** Same rule as {@link #isRestricted(ServerPlayer, ItemStack, Restriction)} with the player's stages already read. */
    public static boolean isItemRestricted(Item item, Set<ResourceLocation> owned, Restriction restriction) {
        return StageManager.get().book().isItemOrFluidBucketRestricted(
                BuiltInRegistries.ITEM.getKey(item), bucketFluidId(item), owned, restriction);
    }

    /** Fluid kind held by a filled bucket item, else {@code null}. */
    @Nullable
    public static ResourceLocation bucketFluidId(Item item) {
        if (!(item instanceof BucketItem bucket)) {
            return null;
        }
        Fluid filled = bucket.content;
        if (filled == null || filled == Fluids.EMPTY) {
            return null;
        }
        return StageDefinition.fluidKindRegistryKey(filled);
    }

    public static boolean isFluidRestricted(ServerPlayer player, FluidStack stack, Restriction restriction) {
        if (stack == null || stack.isEmpty() || restriction == null) {
            return false;
        }
        ResourceLocation kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        return StageManager.get().book().isFluidRestricted(kind, EffectiveStages.view(player), restriction);
    }

    /**
     * Whether {@code player} may not move {@code fluid} in or out of a container ({@code preventTransfer}). The server
     * reads {@link EffectiveStages}; the client predicts from the mirrored {@link ClientStageIndices} /
     * {@link ClientStageCache} so a blocked bucket use does not visually succeed.
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

    public static boolean isChemicalRestricted(
            ServerPlayer player, ResourceLocation chemicalRegistryKey, Restriction restriction) {
        if (chemicalRegistryKey == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isChemicalRestricted(chemicalRegistryKey, EffectiveStages.view(player), restriction);
    }

    public static boolean isRecipeRestricted(
            ServerPlayer player, ResourceLocation recipeHolderId, Restriction restriction) {
        if (recipeHolderId == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isRecipeRestricted(recipeHolderId, EffectiveStages.view(player), restriction);
    }

    public static boolean isDimensionRestricted(
            ServerPlayer player, ResourceLocation dimensionId, Restriction restriction) {
        if (dimensionId == null || restriction == null) {
            return false;
        }
        return StageManager.get()
                .book()
                .isDimensionRestricted(dimensionId, EffectiveStages.view(player), restriction);
    }
}
