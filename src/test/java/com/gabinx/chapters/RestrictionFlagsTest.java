package com.gabinx.chapters;

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
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictionFlagsTest {
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
        entry.addProperty("jei", false);
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
    void inventoryFalseSkipsSlotsToDrop() {
        ResourceLocation stage = id("tutorial:flags_inv");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("inventory", false);
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
        restrictions.addProperty("jei", false);
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
    void stageDefaultsApplyToStringEntries() {
        ResourceLocation stage = id("tutorial:defaults");
        JsonObject json = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("pickup", false);
        restrictions.addProperty("craft", false);
        json.add("restrictions", restrictions);
        JsonArray items = new JsonArray();
        items.add(DIAMOND.toString());
        json.add("items", items);

        StageDefinition def = StageDefinition.fromJson(stage, json);
        RestrictionFlags flags = def.items().get(DIAMOND);
        assertFalse(flags.pickup());
        assertFalse(flags.craft());
        assertTrue(flags.inventory());
        assertTrue(flags.jei());
    }
}
