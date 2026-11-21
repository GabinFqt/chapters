package com.gabinx.chapters.e2e.gametest;

import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eStages;
import com.gabinx.chapters.e2e.E2eSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Sections 5 to 7, server side. Bucket fill goes through {@link FluidUtil#tryPickUpFluid}, which is the
 * path the fluid mixin gates. Vanilla {@code BucketItem.use} picks up through {@code BucketPickup} and
 * does not consult that mixin.
 */
public class CraftAndFluidGameTests {
    private static final BlockPos FLUID_POS = new BlockPos(4, 1, 4);

    public static void spyglassRecipe_blockedUntilStage(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        ItemStack[] grid = spyglassGrid();
        helper.assertTrue(E2eSupport.craft(player, grid).isEmpty(), "spyglass recipe should be blocked");
        E2eSupport.give(player, Items.SPYGLASS, 1);
        E2eSupport.fireCrafted(player, new ItemStack(Items.SPYGLASS), grid);
        helper.assertTrue(E2eSupport.count(player, Items.SPYGLASS) == 0, "CraftingHandler should remove a crafted spyglass");
        E2eSupport.grant(player, E2eStages.RECIPE_SPYGLASS);
        helper.assertTrue(E2eSupport.craft(player, grid).is(Items.SPYGLASS), "unlocked spyglass recipe should craft");
        E2eSupport.give(player, Items.SPYGLASS, 1);
        E2eSupport.fireCrafted(player, new ItemStack(Items.SPYGLASS), grid);
        helper.assertTrue(E2eSupport.count(player, Items.SPYGLASS) >= 1, "unlocked craft should keep the spyglass");
        helper.succeed();
    }

    public static void lava_transferAllowedAndBucketDrops(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        helper.setBlock(FLUID_POS, Blocks.LAVA.defaultBlockState());
        FluidActionResult picked = FluidUtil.tryPickUpFluid(
                new ItemStack(Items.BUCKET), player, helper.getLevel(), helper.absolutePos(FLUID_POS), Direction.UP);
        helper.assertTrue(picked.isSuccess(), "lava pickup should be allowed when preventTransfer is off");
        BlockPos placeAt = helper.absolutePos(FLUID_POS.offset(1, 0, 0));
        helper.getLevel().setBlock(placeAt, Blocks.AIR.defaultBlockState(), 3);
        ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, lavaBucket.copy());
        FluidStack lava = FluidUtil.getFluidContained(lavaBucket).orElse(FluidStack.EMPTY);
        FluidActionResult placed = FluidUtil.tryPlaceFluid(
                player,
                helper.getLevel(),
                InteractionHand.MAIN_HAND,
                placeAt,
                lavaBucket,
                lava);
        helper.assertTrue(
                placed.isSuccess(),
                "lava place should be allowed when preventTransfer is off (fluid " + lava + ", block "
                        + helper.getLevel().getBlockState(placeAt) + ")");
        E2eSupport.give(player, Items.LAVA_BUCKET, 1);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.LAVA_BUCKET) == 0, "lava bucket should still drop");
            helper.succeed();
        });
    }

    public static void water_transferBlockedUntilStage(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        helper.setBlock(FLUID_POS, Blocks.WATER.defaultBlockState());
        FluidActionResult picked = FluidUtil.tryPickUpFluid(
                new ItemStack(Items.BUCKET), player, helper.getLevel(), helper.absolutePos(FLUID_POS), Direction.UP);
        helper.assertFalse(picked.isSuccess(), "water pickup should be blocked");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(FLUID_POS)).is(Blocks.WATER),
                "blocked pickup should leave the source");
        FluidActionResult placed = FluidUtil.tryPlaceFluid(
                player,
                helper.getLevel(),
                InteractionHand.MAIN_HAND,
                helper.absolutePos(FLUID_POS.offset(1, 0, 0)),
                new ItemStack(Items.WATER_BUCKET),
                new FluidStack(Fluids.WATER, 1000));
        helper.assertFalse(placed.isSuccess(), "water place should be blocked");
        E2eSupport.give(player, Items.WATER_BUCKET, 1);
        helper.runAfterDelay(1, () -> {
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.WATER_BUCKET) == 0, "water bucket should drop");
            E2eSupport.grant(player, E2eStages.WATER_TRANSFER);
            FluidActionResult unlocked = FluidUtil.tryPickUpFluid(
                    new ItemStack(Items.BUCKET), player, helper.getLevel(), helper.absolutePos(FLUID_POS), Direction.UP);
            helper.assertTrue(unlocked.isSuccess(), "unlocked water pickup should succeed");
            helper.succeed();
        });
    }

    public static void vanillaWater_blockedUntilStage(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        BlockPos target = new BlockPos(2, 2, 4);
        BlockPos placedAt = new BlockPos(2, 2, 3);
        helper.setBlock(target, Blocks.STONE.defaultBlockState());
        InteractionResult placeBlocked = useBucket(player, Items.WATER_BUCKET);
        helper.assertTrue(placeBlocked == InteractionResult.FAIL, "a vanilla water bucket should refuse to place");
        helper.assertFalse(
                helper.getLevel().getBlockState(helper.absolutePos(placedAt)).is(Blocks.WATER),
                "locked water should not be placed");
        helper.setBlock(target, Blocks.WATER.defaultBlockState());
        InteractionResult blocked = useBucket(player, Items.BUCKET);
        helper.assertTrue(blocked == InteractionResult.FAIL, "a vanilla bucket should refuse locked water");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(target)).is(Blocks.WATER), "locked water should stay");
        E2eSupport.grant(player, E2eStages.WATER_TRANSFER);
        InteractionResult unlocked = useBucket(player, Items.BUCKET);
        helper.assertTrue(unlocked.consumesAction(), "an unlocked vanilla bucket should pick up water");
        helper.assertFalse(
                helper.getLevel().getBlockState(helper.absolutePos(target)).is(Blocks.WATER),
                "unlocked pickup should remove the source");
        helper.setBlock(target, Blocks.STONE.defaultBlockState());
        InteractionResult placed = useBucket(player, Items.WATER_BUCKET);
        helper.assertTrue(placed.consumesAction(), "an unlocked water bucket should place");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(placedAt)).is(Blocks.WATER),
                "unlocked place should leave a source");
        helper.succeed();
    }

    public static void vanillaLava_placeAndPickupAllowed(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        BlockPos lava = new BlockPos(2, 2, 4);
        BlockPos placedAt = new BlockPos(2, 2, 3);
        helper.setBlock(lava, Blocks.LAVA.defaultBlockState());
        InteractionResult picked = useBucket(player, Items.BUCKET);
        helper.assertTrue(picked.consumesAction(), "lava pickup stays allowed when preventTransfer is off");
        helper.assertFalse(
                helper.getLevel().getBlockState(helper.absolutePos(lava)).is(Blocks.LAVA),
                "lava pickup should remove the source");
        helper.setBlock(lava, Blocks.STONE.defaultBlockState());
        InteractionResult placed = useBucket(player, Items.LAVA_BUCKET);
        helper.assertTrue(placed.consumesAction(), "lava place stays allowed when preventTransfer is off");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(placedAt)).is(Blocks.LAVA),
                "lava place should leave a source in front of the clicked block");
        helper.succeed();
    }

    /** The joined player stands at relative (2, 1, 2) looking +Z, so a source at (2, 2, 4) is at eye height. */
    private static InteractionResult useBucket(ServerPlayer player, Item item) {
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setYHeadRot(0.0F);
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static ItemStack[] spyglassGrid() {
        ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack[] grid = new ItemStack[9];
        grid[0] = shard;
        grid[3] = copper;
        grid[6] = copper;
        return grid;
    }
}
