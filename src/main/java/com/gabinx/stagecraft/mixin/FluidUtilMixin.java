package com.gabinx.stagecraft.mixin;

import com.gabinx.stagecraft.stage.LockResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = FluidUtil.class, remap = false)
public abstract class FluidUtilMixin {
    private static final ThreadLocal<Player> STAGECRAFT_FLUID_ACTOR = new ThreadLocal<>();

    @Inject(
        method = "tryFillContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;",
        at = @At("HEAD")
    )
    private static void stagecraft$beginTryFill(ItemStack container, IFluidHandler fluidSource, int maxAmount, @Nullable Player player, boolean doFill) {
        STAGECRAFT_FLUID_ACTOR.set(player);
    }

    @Inject(
        method = "tryFillContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;",
        at = @At("RETURN")
    )
    private static void stagecraft$endTryFill(ItemStack container, IFluidHandler fluidSource, int maxAmount, @Nullable Player player, boolean doFill) {
        STAGECRAFT_FLUID_ACTOR.remove();
    }

    @Inject(
        method = "tryEmptyContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;",
        at = @At("HEAD")
    )
    private static void stagecraft$beginTryEmpty(ItemStack container, IFluidHandler fluidDestination, int maxAmount, @Nullable Player player, boolean doDrain) {
        STAGECRAFT_FLUID_ACTOR.set(player);
    }

    @Inject(
        method = "tryEmptyContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;",
        at = @At("RETURN")
    )
    private static void stagecraft$endTryEmpty(ItemStack container, IFluidHandler fluidDestination, int maxAmount, @Nullable Player player, boolean doDrain) {
        STAGECRAFT_FLUID_ACTOR.remove();
    }

    @Inject(
        method = "tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void stagecraft$gatePlaceFluid(
        @Nullable Player player,
        Level level,
        InteractionHand hand,
        BlockPos pos,
        IFluidHandler fluidSource,
        FluidStack resource,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!resource.isEmpty() && player instanceof ServerPlayer sp && LockResolver.isFluidLocked(sp, resource)) {
            cir.setReturnValue(false);
            return;
        }
        STAGECRAFT_FLUID_ACTOR.set(player);
    }

    @Inject(
        method = "tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;)Z",
        at = @At("RETURN")
    )
    private static void stagecraft$releasePlaceFluid(
        @Nullable Player player,
        Level level,
        InteractionHand hand,
        BlockPos pos,
        IFluidHandler fluidSource,
        FluidStack resource,
        CallbackInfoReturnable<Boolean> cir
    ) {
        STAGECRAFT_FLUID_ACTOR.remove();
    }

    @Inject(
        method = "tryFluidTransfer(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;IZ)Lnet/neoforged/neoforge/fluids/FluidStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;tryFluidTransfer_Internal(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Lnet/neoforged/neoforge/fluids/FluidStack;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void stagecraft$blockTransferByAmount(
        IFluidHandler fluidDestination,
        IFluidHandler fluidSource,
        int maxAmount,
        boolean doTransfer,
        CallbackInfoReturnable<FluidStack> cir,
        FluidStack drainable
    ) {
        Player actor = STAGECRAFT_FLUID_ACTOR.get();
        if (actor instanceof ServerPlayer sp && !drainable.isEmpty() && LockResolver.isFluidLocked(sp, drainable)) {
            cir.setReturnValue(FluidStack.EMPTY);
        }
    }

    @Inject(
        method = "tryFluidTransfer(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Lnet/neoforged/neoforge/fluids/FluidStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;tryFluidTransfer_Internal(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Lnet/neoforged/neoforge/fluids/FluidStack;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void stagecraft$blockTransferByStack(
        IFluidHandler fluidDestination,
        IFluidHandler fluidSource,
        FluidStack resource,
        boolean doTransfer,
        CallbackInfoReturnable<FluidStack> cir,
        FluidStack drainable
    ) {
        Player actor = STAGECRAFT_FLUID_ACTOR.get();
        if (actor instanceof ServerPlayer sp && !drainable.isEmpty() && LockResolver.isFluidLocked(sp, drainable)) {
            cir.setReturnValue(FluidStack.EMPTY);
        }
    }
}
