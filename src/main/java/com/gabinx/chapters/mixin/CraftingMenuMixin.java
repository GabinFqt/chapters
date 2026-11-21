package com.gabinx.chapters.mixin;

import com.gabinx.chapters.logic.CraftGate;
import com.gabinx.chapters.logic.Restriction;
import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * The 3x3 table and the 2x2 inventory grid both call this method
 * ({@code InventoryMenu.slotsChanged} delegates here).
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(
        method = "slotChangedCraftingGrid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        cancellable = true,
        // Do not capture Optional<RecipeHolder<?>> here: other mods’ transformers often drop that local, which breaks
        // CAPTURE_FAILHARD. We only need CraftingInput, ServerPlayer, and the computed result stack.
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void chapters$hideLockedResult(
        AbstractContainerMenu menu,
        Level level,
        net.minecraft.world.entity.player.Player player,
        CraftingContainer craftSlots,
        ResultContainer resultSlots,
        @Nullable RecipeHolder<CraftingRecipe> recipe,
        CallbackInfo ci,
        CraftingInput craftingInput,
        ServerPlayer serverPlayer,
        ItemStack result
    ) {
        if (level.isClientSide || result.isEmpty()) {
            return;
        }

        boolean outputLocked = LockResolver.isRestricted(serverPlayer, result, Restriction.CRAFT);
        // A non-empty result means vanilla just stored the matched recipe via setRecipeUsed.
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        boolean recipeLocked = !outputLocked
                && used != null
                && LockResolver.isRecipeRestricted(serverPlayer, used.id(), Restriction.CRAFT);
        if (!CraftGate.shouldBlock(outputLocked, recipeLocked)) {
            return;
        }

        resultSlots.setItem(0, ItemStack.EMPTY);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        ci.cancel();
    }
}
