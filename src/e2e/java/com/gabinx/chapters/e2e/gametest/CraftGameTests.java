package com.gabinx.chapters.e2e.gametest;

import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eStages;
import com.gabinx.chapters.e2e.E2eSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Real menu clicks, the 2x2 inventory grid, a second recipe for a locked recipe id, and a tag-locked output.
 */
@GameTestHolder("chapters")
@PrefixGameTestTemplate(false)
public class CraftGameTests {
    @GameTest(template = "empty", batch = "chapters", timeoutTicks = 40)
    public static void lockedResult_pickupClearsCursor(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper, false).player();
        E2eSupport.grant(player, E2eStages.PLANKS);
        CraftingMenu menu = E2eSupport.openCraftingTable(player, E2eSupport.grid(new ItemStack(Items.BIRCH_LOG)));
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.BIRCH_PLANKS), "birch planks should show while unlocked");
        E2eSupport.revoke(player, E2eStages.PLANKS);
        E2eSupport.takeResult(menu, player, ClickType.PICKUP);
        helper.assertTrue(menu.getCarried().isEmpty(), "a locked result must not stay on the cursor");
        helper.assertTrue(E2eSupport.count(player, Items.BIRCH_PLANKS) == 0, "a locked result must not enter the inventory");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "chapters", timeoutTicks = 40)
    public static void lockedResult_quickMoveDoesNotEnterInventory(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper, false).player();
        E2eSupport.grant(player, E2eStages.PLANKS);
        CraftingMenu menu = E2eSupport.openCraftingTable(player, E2eSupport.grid(new ItemStack(Items.BIRCH_LOG)));
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.BIRCH_PLANKS), "birch planks should show while unlocked");
        E2eSupport.revoke(player, E2eStages.PLANKS);
        E2eSupport.takeResult(menu, player, ClickType.QUICK_MOVE);
        helper.assertTrue(E2eSupport.count(player, Items.BIRCH_PLANKS) == 0, "shift-click must not keep locked planks");
        helper.assertTrue(menu.getCarried().isEmpty(), "shift-click must not leave locked planks on the cursor");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "chapters", timeoutTicks = 40)
    public static void inventoryGrid_hidesLockedPlanks(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper, false).player();
        var menu = E2eSupport.fillInventoryGrid(player, new ItemStack(Items.BIRCH_LOG));
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "the 2x2 grid should hide locked birch planks");
        E2eSupport.grant(player, E2eStages.PLANKS);
        menu.getSlot(1).set(new ItemStack(Items.BIRCH_LOG));
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.BIRCH_PLANKS), "unlocked birch planks should show in the 2x2 grid");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "chapters", timeoutTicks = 40)
    public static void altSpyglass_craftsWhileVanillaRecipeLocked(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper, false).player();
        ItemStack[] vanilla = spyglassGrid();
        helper.assertTrue(E2eSupport.craft(player, vanilla).isEmpty(), "the vanilla spyglass recipe should be blocked");
        ItemStack[] alt = E2eSupport.grid(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.AMETHYST_SHARD));
        helper.assertTrue(
                E2eSupport.craft(player, alt).is(Items.SPYGLASS),
                "a different spyglass recipe should still craft");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "chapters", timeoutTicks = 40)
    public static void planksTag_blocksOakAndBirchOutputs(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper, false).player();
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new ItemStack(Items.OAK_LOG))).isEmpty(),
                "oak planks stay craft-locked: the oak override is pickup only");
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new ItemStack(Items.BIRCH_LOG))).isEmpty(),
                "birch planks should be craft-locked by the planks tag");
        E2eSupport.grant(player, E2eStages.PLANKS);
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new ItemStack(Items.OAK_LOG))).is(Items.OAK_PLANKS),
                "oak planks should craft once the stage is owned");
        helper.assertTrue(
                E2eSupport.craft(player, E2eSupport.grid(new ItemStack(Items.BIRCH_LOG))).is(Items.BIRCH_PLANKS),
                "birch planks should craft once the stage is owned");
        helper.succeed();
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
