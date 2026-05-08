package com.gabinx.stagecraft.event;

import com.gabinx.stagecraft.stage.LockResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CraftingHandler {
    private CraftingHandler() {
    }

    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !LockResolver.isLocked(player, crafted)) {
            return;
        }

        int remaining = crafted.getCount();
        remaining = removeFromCarriedStack(player, crafted, remaining);
        removeFromInventory(player, crafted, remaining);
        player.containerMenu.broadcastChanges();

        player.displayClientMessage(
                Component.translatable("commands.stagecraft.craft.blocked", crafted.getHoverName()),
                true
        );
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
