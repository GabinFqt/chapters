package com.gabinx.chapters.e2e;

import com.gabinx.chapters.ChaptersConfig;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shared setup and interactions for the flag GameTests. */
public final class E2eSupport {
    private E2eSupport() {
    }

    public static void prepare(GameTestHelper helper) {
        ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(false);
        helper.assertTrue(
                StageManager.get().get(E2eStages.SOFT_AMETHYST).isPresent(),
                "e2e stage datapack was not loaded (chapters:e2e_soft_amethyst)");
    }

    public static void grant(ServerPlayer player, Identifier stage) {
        ChaptersAPI.addStage(player, stage);
    }

    public static void revoke(ServerPlayer player, Identifier stage) {
        ChaptersAPI.removeStage(player, stage);
    }

    public static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    public static void give(ServerPlayer player, Item item, int amount) {
        player.getInventory().add(new ItemStack(item, amount));
    }

    /**
     * Inventory audits run from {@code Player.tick}, which the server only reaches through the
     * connection. A mock connection is never ticked, and {@code tickCount} only advances in
     * {@code ServerLevel}, so drive one audit tick directly.
     */
    public static void settle(ServerPlayer player) {
        player.tickCount = 20;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
    }

    /**
     * Spawns a stack at the player's feet and runs the pickup event. Pickup delay is cleared so an
     * allowed pickup is not rejected by the vanilla delay.
     */
    public static ItemEntity offerPickup(GameTestHelper helper, ServerPlayer player, Item item) {
        Vec3 feet = player.position();
        ItemEntity entity = new ItemEntity(helper.getLevel(), feet.x, feet.y, feet.z, new ItemStack(item));
        entity.setPickUpDelay(0);
        entity.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(entity);
        entity.playerTouch(player);
        return entity;
    }

    /**
     * Fills a 3x3 crafting grid (index 0 is the top-left slot) and returns the result after
     * {@code CraftingMenu} runs its lock mixin.
     */
    public static ItemStack craft(ServerPlayer player, ItemStack[] grid) {
        CraftingMenu menu = openGrid(player, grid);
        return menu.getSlot(0).getItem().copy();
    }

    /** Fires the crafted event the way a result-slot take does, against the still-filled grid. */
    public static void fireCrafted(ServerPlayer player, ItemStack crafted, ItemStack[] grid) {
        CraftingMenu menu = openGrid(player, grid);
        Container matrix = menu.getSlot(1).container;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, crafted, matrix));
    }

    private static CraftingMenu openGrid(ServerPlayer player, ItemStack[] grid) {
        CraftingMenu menu = new CraftingMenu(
                42,
                player.getInventory(),
                ContainerLevelAccess.create(player.level(), BlockPos.containing(player.position())));
        for (int i = 0; i < 9; i++) {
            ItemStack stack = grid[i] == null ? ItemStack.EMPTY : grid[i];
            if (!stack.isEmpty()) {
                menu.getSlot(1 + i).set(stack.copy());
            }
        }
        return menu;
    }

    public static ItemStack[] grid(ItemStack... stacks) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < stacks.length && i < 9; i++) {
            grid[i] = stacks[i];
        }
        return grid;
    }
}
