package com.gabinx.chapters.event;

import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Cauldrons fill and empty buckets without going through {@code FluidUtil} or {@code BucketItem#use}. Gate both on
 * {@code preventTransfer}, like the bucket and fluid-handler paths.
 */
public final class CauldronHandler {
    private CauldronHandler() {
    }

    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AbstractCauldronBlock)) {
            return;
        }
        Fluid fluid = movedFluid(event.getItemStack(), state);
        if (fluid != null && LockResolver.isFluidLocked(event.getEntity(), fluid)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Fluid that using {@code held} on this cauldron would move into or out of a bucket, else {@code null}. */
    @Nullable
    static Fluid movedFluid(ItemStack held, BlockState cauldron) {
        if (held.is(Items.BUCKET)) {
            if (cauldron.is(Blocks.WATER_CAULDRON)) {
                return Fluids.WATER;
            }
            if (cauldron.is(Blocks.LAVA_CAULDRON)) {
                return Fluids.LAVA;
            }
            return null;
        }
        if (held.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
            return bucket.content;
        }
        return null;
    }
}
