package com.gabinx.stagecraft.event;

import com.gabinx.stagecraft.stage.LockResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

public final class CraftingHandler {
    private CraftingHandler() {
    }

    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        boolean itemLocked = !crafted.isEmpty() && LockResolver.isLocked(player, crafted);
        boolean recipeLocked =
                crafted.isEmpty() ? false : isCraftingRecipeLocked(player, event.getInventory(), crafted);

        if (!itemLocked && !recipeLocked) {
            return;
        }

        int remaining = crafted.getCount();
        remaining = removeFromCarriedStack(player, crafted, remaining);
        removeFromInventory(player, crafted, remaining);
        player.containerMenu.broadcastChanges();

        if (recipeLocked && !itemLocked) {
            player.displayClientMessage(
                    Component.translatable("commands.stagecraft.craft.blocked_recipe", crafted.getHoverName()),
                    true
            );
            return;
        }

        player.displayClientMessage(
                Component.translatable("commands.stagecraft.craft.blocked", crafted.getHoverName()),
                true
        );
    }

    private static boolean isCraftingRecipeLocked(ServerPlayer player, Container craftMatrix, ItemStack crafted) {
        if (crafted.isEmpty() || !(craftMatrix instanceof CraftingContainer crafting)) {
            return false;
        }
        if (player.level() == null || player.level().isClientSide) {
            return false;
        }
        Optional<ResourceLocation> recipeId =
                player.level().getRecipeManager().getRecipeFor(
                        RecipeType.CRAFTING,
                        crafting.asCraftInput(),
                        player.level()).map(RecipeHolder<CraftingRecipe>::id);
        return recipeId.map(id -> LockResolver.isRecipeLocked(player, id)).orElse(false);
    }

    private static int removeFromCarriedStack(ServerPlayer player, ItemStack target, int amount) {
        if (amount <= 0) {
            return 0;
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (!ItemStack.isSameItemSameComponents(carried, target)) {
            return amount;
        }

        int removed = Math.min(amount, carried.getCount());
        carried.shrink(removed);
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        return amount - removed;
    }

    private static void removeFromInventory(ServerPlayer player, ItemStack target, int amount) {
        if (amount <= 0) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!ItemStack.isSameItemSameComponents(stack, target)) {
                continue;
            }

            int removed = Math.min(amount, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            amount -= removed;
        }
    }
}
