package com.gabinx.chapters;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.InventoryPlan;
import com.gabinx.chapters.logic.Restriction;
import com.gabinx.chapters.logic.RestrictionFlags;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.DIAMOND;
import static com.gabinx.chapters.TutorialFixtures.EMERALD;
import static com.gabinx.chapters.TutorialFixtures.WATER;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictionFlagsTest {
    private static final ResourceLocation OAK_PLANKS = id("minecraft:oak_planks");
    private static final ResourceLocation BIRCH_PLANKS = id("minecraft:birch_planks");

    @Test
    void stringOnlyJsonKeepsAllFlags() {
        ResourceLocation stage = id("tutorial:flags_default");
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add(DIAMOND.toString());
        json.add("items", items);

        StageDefinition def = StageDefinition.fromJson(stage, json);
        assertEquals(RestrictionFlags.ALL, def.items().get(DIAMOND));
    }

    @Test
    void entryOverrideDisablesJeiButKeepsLock() {
        ResourceLocation stage = id("tutorial:flags_jei");
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("id", EMERALD.toString());
        entry.addProperty("hideInJei", false);
        items.add(entry);
        json.add("items", items);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json)));

        assertTrue(book.isItemLocked(EMERALD, Set.of()));
        assertFalse(book.isItemRestricted(EMERALD, Set.of(), Restriction.JEI));
        assertTrue(book.isItemRestricted(EMERALD, Set.of(), Restriction.PICKUP));
        assertFalse(book.itemJeiStagesIndexView().containsKey(EMERALD));
        assertTrue(book.itemStagesIndexView().containsKey(EMERALD));
    }

    @Test
    void dropFromInventoryFalseSkipsSlotsToDrop() {
        ResourceLocation stage = id("tutorial:flags_inv");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("dropFromInventory", false);
        json.add("restrictions", restrictions);
        JsonArray items = new JsonArray();
        items.add(DIAMOND.toString());
        json.add("items", items);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json)));

        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
        List<Integer> slots = InventoryPlan.slotsToDrop(List.of(DIAMOND), book, Set.of());
        assertEquals(List.of(), slots);
    }

    @Test
    void multiStageJeiOrKeepsHideWhileLocked() {
        ResourceLocation stageA = id("tutorial:a_jei");
        ResourceLocation stageB = id("tutorial:b_nojei");

        JsonObject withJei = new JsonObject();
        JsonArray itemsA = new JsonArray();
        itemsA.add(DIAMOND.toString());
        withJei.add("items", itemsA);

        JsonObject noJei = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("hideInJei", false);
        noJei.add("restrictions", restrictions);
        JsonArray itemsB = new JsonArray();
        itemsB.add(DIAMOND.toString());
        noJei.add("items", itemsB);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, withJei),
                stageB, StageDefinition.fromJson(stageB, noJei)
        ));

        assertTrue(book.isItemRestricted(DIAMOND, Set.of(), Restriction.JEI));
        assertTrue(book.itemJeiStagesIndexView().get(DIAMOND).contains(stageA));
        assertFalse(book.itemJeiStagesIndexView().get(DIAMOND).contains(stageB));
    }

    @Test
    void owningHideInJeiFalseStageUnlocksGameplayAndJeiHide() {
        ResourceLocation stageA = id("tutorial:a_jei");
        ResourceLocation stageB = id("tutorial:b_nojei");

        JsonObject withJei = new JsonObject();
        JsonArray itemsA = new JsonArray();
        itemsA.add(DIAMOND.toString());
        withJei.add("items", itemsA);

        JsonObject noJei = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("hideInJei", false);
        noJei.add("restrictions", restrictions);
        JsonArray itemsB = new JsonArray();
        itemsB.add(DIAMOND.toString());
        noJei.add("items", itemsB);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, withJei),
                stageB, StageDefinition.fromJson(stageB, noJei)
        ));

        Set<ResourceLocation> ownedB = Set.of(stageB);
        assertFalse(book.isItemLocked(DIAMOND, ownedB));
        assertFalse(book.isItemRestricted(DIAMOND, ownedB, Restriction.JEI));
        assertFalse(book.isItemRestricted(DIAMOND, ownedB, Restriction.PICKUP));

        Set<ResourceLocation> jeiHidden = ClientStageView.computeFlagRestricted(
                book.itemStagesIndexView(), book.itemJeiStagesIndexView(), ownedB);
        assertFalse(jeiHidden.contains(DIAMOND));
    }

    @Test
    void multiStageFlagOrWhileLockedUnlocksAllWhenOwningOne() {
        ResourceLocation stageA = id("tutorial:pickup_only");
        ResourceLocation stageB = id("tutorial:inventory_only");

        JsonObject pickupOnly = stageRestrictions(true, false, false, false, false, false);
        JsonArray itemsA = new JsonArray();
        itemsA.add(DIAMOND.toString());
        pickupOnly.add("items", itemsA);

        JsonObject inventoryOnly = stageRestrictions(false, true, false, false, false, false);
        JsonArray itemsB = new JsonArray();
        itemsB.add(DIAMOND.toString());
        inventoryOnly.add("items", itemsB);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, pickupOnly),
                stageB, StageDefinition.fromJson(stageB, inventoryOnly)
        ));

        assertTrue(book.isItemRestricted(DIAMOND, Set.of(), Restriction.PICKUP));
        assertTrue(book.isItemRestricted(DIAMOND, Set.of(), Restriction.INVENTORY));
        assertFalse(book.isItemRestricted(DIAMOND, Set.of(stageA), Restriction.PICKUP));
        assertFalse(book.isItemRestricted(DIAMOND, Set.of(stageA), Restriction.INVENTORY));
    }

    @Test
    void explicitEntryOverridesTagFlagsInSameStage() {
        ResourceLocation stage = id("tutorial:tag_override");
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add("#minecraft:planks");
        JsonObject override = new JsonObject();
        override.addProperty("id", OAK_PLANKS.toString());
        override.addProperty("preventPickup", false);
        items.add(override);
        json.add("items", items);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json)));

        assertFalse(book.isItemRestricted(OAK_PLANKS, Set.of(), Restriction.PICKUP));
        assertTrue(book.isItemRestricted(BIRCH_PLANKS, Set.of(), Restriction.PICKUP));
        assertTrue(book.isItemLocked(OAK_PLANKS, Set.of()));
    }

    @Test
    void preventTransferFalseKeepsFluidInLockIndexButNotTransferIndex() {
        ResourceLocation stage = id("tutorial:no_transfer");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("preventTransfer", false);
        json.add("restrictions", restrictions);
        JsonArray fluids = new JsonArray();
        fluids.add(WATER.toString());
        json.add("fluids", fluids);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json)));

        assertTrue(book.isFluidLocked(WATER, Set.of()));
        assertFalse(book.isFluidRestricted(WATER, Set.of(), Restriction.TRANSFER));
        assertFalse(book.fluidTransferStagesIndexView().containsKey(WATER));
    }

    @Test
    void preventTravelFalseAllowsDimensionTravelWhileStillLocked() {
        ResourceLocation stage = id("tutorial:no_travel");
        ResourceLocation nether = id("minecraft:the_nether");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("preventTravel", false);
        json.add("restrictions", restrictions);
        JsonArray dimensions = new JsonArray();
        dimensions.add(nether.toString());
        json.add("dimensions", dimensions);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json)));

        assertTrue(book.isDimensionLocked(nether, Set.of()));
        assertFalse(book.isDimensionRestricted(nether, Set.of(), Restriction.TRAVEL));
    }

    @Test
    void allFlagsFalseStillDefinesUnlockKey() {
        ResourceLocation soft = id("tutorial:soft");
        ResourceLocation hard = id("tutorial:hard");

        JsonObject softJson = stageRestrictions(false, false, false, false, false, false);
        JsonArray softItems = new JsonArray();
        softItems.add(DIAMOND.toString());
        softJson.add("items", softItems);

        JsonObject hardJson = new JsonObject();
        JsonArray hardItems = new JsonArray();
        hardItems.add(DIAMOND.toString());
        hardJson.add("items", hardItems);

        StageBook softOnly = new StageBook(TutorialFixtures.tutorialCatalog());
        softOnly.replaceDatapack(Map.of(soft, StageDefinition.fromJson(soft, softJson)));
        assertTrue(softOnly.isItemLocked(DIAMOND, Set.of()));
        assertFalse(softOnly.isItemRestricted(DIAMOND, Set.of(), Restriction.PICKUP));
        assertFalse(softOnly.itemJeiStagesIndexView().containsKey(DIAMOND));

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(
                soft, StageDefinition.fromJson(soft, softJson),
                hard, StageDefinition.fromJson(hard, hardJson)
        ));

        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
        assertTrue(book.isItemRestricted(DIAMOND, Set.of(), Restriction.PICKUP));
        assertTrue(book.itemStagesIndexView().get(DIAMOND).contains(soft));
        assertTrue(book.itemJeiStagesIndexView().get(DIAMOND).contains(hard));
        assertFalse(book.itemJeiStagesIndexView().get(DIAMOND).contains(soft));

        assertFalse(book.isItemLocked(DIAMOND, Set.of(soft)));
        assertFalse(book.isItemRestricted(DIAMOND, Set.of(soft), Restriction.PICKUP));
    }

    @Test
    void stageDefaultsApplyToStringEntries() {
        ResourceLocation stage = id("tutorial:defaults");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("preventPickup", false);
        restrictions.addProperty("preventCraft", false);
        json.add("restrictions", restrictions);
        JsonArray items = new JsonArray();
        items.add(DIAMOND.toString());
        json.add("items", items);

        StageDefinition def = StageDefinition.fromJson(stage, json);
        RestrictionFlags flags = def.items().get(DIAMOND);
        assertFalse(flags.preventPickup());
        assertFalse(flags.preventCraft());
        assertTrue(flags.dropFromInventory());
        assertTrue(flags.hideInJei());
    }



    private static JsonObject stageRestrictions(
            boolean preventPickup,
            boolean dropFromInventory,
            boolean preventCraft,
            boolean hideInJei,
            boolean preventTransfer,
            boolean preventTravel
    ) {
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("preventPickup", preventPickup);
        restrictions.addProperty("dropFromInventory", dropFromInventory);
        restrictions.addProperty("preventCraft", preventCraft);
        restrictions.addProperty("hideInJei", hideInJei);
        restrictions.addProperty("preventTransfer", preventTransfer);
        restrictions.addProperty("preventTravel", preventTravel);
        json.add("restrictions", restrictions);
        return json;
    }
}
