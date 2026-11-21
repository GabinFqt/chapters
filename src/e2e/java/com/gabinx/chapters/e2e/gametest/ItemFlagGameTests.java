package com.gabinx.chapters.e2e.gametest;

import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eStages;
import com.gabinx.chapters.e2e.E2eSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Sections 1 to 4 and 9 of the flags protocol, minus JEI (that needs the client harness).
 * {@code preventCraft} locks the recipe output, so the amethyst check crafts shards out of a block.
 */
public class ItemFlagGameTests {
    public static void softAmethyst_keepsAndBlocksPickup(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.give(player, Items.AMETHYST_SHARD, 8);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.AMETHYST_SHARD) == 8, "soft amethyst must stay in the inventory");
            int before = E2eSupport.count(player, Items.AMETHYST_SHARD);
            ItemEntity offered = E2eSupport.offerPickup(helper, player, Items.AMETHYST_SHARD);
            helper.assertTrue(offered.isAlive(), "locked shard on the ground should stay");
            helper.assertTrue(E2eSupport.count(player, Items.AMETHYST_SHARD) == before, "pickup of a locked shard should fail");
            E2eSupport.grant(player, E2eStages.SOFT_AMETHYST);
            ItemEntity unlocked = E2eSupport.offerPickup(helper, player, Items.AMETHYST_SHARD);
            helper.assertFalse(unlocked.isAlive(), "unlocked shard should be picked up");
            helper.succeed();
        });
    }

    public static void softAmethyst_craftOutput(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new net.minecraft.world.item.ItemStack(Items.AMETHYST_BLOCK))).isEmpty(),
                "crafting shards from a block should be blocked");
        helper.assertFalse(
                E2eSupport.craft(player, four(Items.AMETHYST_SHARD)).isEmpty(),
                "crafting the block from shards is not gated: the lock is on the shard output");
        E2eSupport.grant(player, E2eStages.SOFT_AMETHYST);
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new net.minecraft.world.item.ItemStack(Items.AMETHYST_BLOCK)))
                        .is(Items.AMETHYST_SHARD),
                "unlocked shard recipe should craft");
        helper.succeed();
    }

    public static void teaseQuartz_dropsAndBlocksPickup(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.give(player, Items.QUARTZ, 16);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.QUARTZ) == 0, "quartz should be dropped");
            ItemEntity offered = E2eSupport.offerPickup(helper, player, Items.QUARTZ);
            helper.assertTrue(offered.isAlive(), "quartz pickup should be blocked");
            E2eSupport.grant(player, E2eStages.TEASE_QUARTZ);
            E2eSupport.give(player, Items.QUARTZ, 16);
            helper.runAfterDelay(1, () -> {
                E2eSupport.settle(player);
                helper.assertTrue(E2eSupport.count(player, Items.QUARTZ) >= 16, "unlocked quartz should stay");
                ItemEntity unlocked = E2eSupport.offerPickup(helper, player, Items.QUARTZ);
                helper.assertFalse(unlocked.isAlive(), "unlocked quartz should be picked up");
                helper.succeed();
            });
        });
    }

    public static void fullPrismarine_dropsAndBlocksPickup(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.give(player, Items.PRISMARINE_SHARD, 8);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.PRISMARINE_SHARD) == 0, "prismarine should be dropped");
            ItemEntity offered = E2eSupport.offerPickup(helper, player, Items.PRISMARINE_SHARD);
            helper.assertTrue(offered.isAlive(), "prismarine pickup should be blocked");
            E2eSupport.grant(player, E2eStages.FULL_PRISMARINE);
            E2eSupport.give(player, Items.PRISMARINE_SHARD, 4);
            ItemEntity unlocked = E2eSupport.offerPickup(helper, player, Items.PRISMARINE_SHARD);
            helper.assertFalse(unlocked.isAlive(), "unlocked prismarine should be picked up");
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.PRISMARINE_SHARD) >= 5, "unlocked give and pickup should stay after an audit");
            helper.succeed();
        });
    }

    public static void planks_oakOverride(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        ItemEntity oak = E2eSupport.offerPickup(helper, player, Items.OAK_PLANKS);
        helper.assertFalse(oak.isAlive(), "oak pickup is overridden on");
        helper.assertTrue(E2eSupport.count(player, Items.OAK_PLANKS) == 1, "oak should enter the inventory");
        ItemEntity birch = E2eSupport.offerPickup(helper, player, Items.BIRCH_PLANKS);
        helper.assertTrue(birch.isAlive(), "birch pickup should be blocked");
        helper.assertTrue(E2eSupport.count(player, Items.BIRCH_PLANKS) == 0, "birch should not enter the inventory");
        E2eSupport.grant(player, E2eStages.PLANKS);
        ItemEntity birchUnlocked = E2eSupport.offerPickup(helper, player, Items.BIRCH_PLANKS);
        helper.assertFalse(birchUnlocked.isAlive(), "unlocked birch should be picked up");
        helper.succeed();
    }

    public static void orStages_oneUnlocksDiamond(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.give(player, Items.DIAMOND, 1);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.DIAMOND) == 0, "diamond should drop with neither stage");
            E2eSupport.grant(player, E2eStages.OR_A);
            E2eSupport.give(player, Items.DIAMOND, 1);
            helper.runAfterDelay(1, () -> {
                E2eSupport.settle(player);
                helper.assertTrue(E2eSupport.count(player, Items.DIAMOND) >= 1, "owning only or_a should keep diamond");
                E2eSupport.revoke(player, E2eStages.OR_A);
                helper.runAfterDelay(1, () -> {
                    E2eSupport.settle(player);
                    helper.assertTrue(E2eSupport.count(player, Items.DIAMOND) == 0, "removing or_a should lock diamond again");
                    E2eSupport.grant(player, E2eStages.OR_B);
                    E2eSupport.give(player, Items.DIAMOND, 1);
                    helper.assertTrue(ChaptersAPI.hasStage(player, E2eStages.OR_B), "or_b should be recorded");
                    helper.runAfterDelay(1, () -> {
                        E2eSupport.settle(player);
                        helper.assertTrue(E2eSupport.count(player, Items.DIAMOND) >= 1, "owning only or_b should keep diamond");
                        helper.succeed();
                    });
                });
            });
        });
    }

    private static net.minecraft.world.item.ItemStack[] four(Item item) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        return E2eSupport.grid(stack, stack, null, stack, stack);
    }
}
