package com.gabinx.chapters.mixin;

import com.gabinx.chapters.logic.Restriction;
import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla buckets do not go through {@code FluidUtil}. Gate the item itself on {@code preventTransfer}.
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;

    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"
            ),
            cancellable = true
    )
    private void chapters$blockFill(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BlockHitResult hit = net.minecraft.world.item.Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        FluidState fluidState = level.getFluidState(hit.getBlockPos());
        if (!fluidState.isSource()) {
            return;
        }
        if (LockResolver.isFluidRestricted(serverPlayer, new FluidStack(fluidState.getType(), 1000), Restriction.TRANSFER)) {
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }

    @Inject(
            method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void chapters$blockEmpty(
            Player player,
            Level level,
            BlockPos pos,
            BlockHitResult hitResult,
            ItemStack container,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player instanceof ServerPlayer serverPlayer
                && LockResolver.isFluidRestricted(serverPlayer, new FluidStack(this.content, 1000), Restriction.TRANSFER)) {
            cir.setReturnValue(false);
        }
    }
}
