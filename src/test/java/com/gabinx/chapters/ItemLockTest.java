package com.gabinx.chapters;

import com.gabinx.chapters.logic.CraftGate;
import com.gabinx.chapters.logic.InventoryPlan;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.DIAMOND;
import static com.gabinx.chapters.TutorialFixtures.EMERALD;
import static com.gabinx.chapters.TutorialFixtures.GOLDEN_APPLE;
import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.TIER_EARLY;
import static com.gabinx.chapters.TutorialFixtures.TIER_LATE;
import static com.gabinx.chapters.TutorialFixtures.TIER_MID;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLockTest {
    private StageBook book;

    @BeforeEach
    void setUp() throws IOException {
        book = TutorialFixtures.loadTutorialBook();
    }

    @Test
    void netheriteLockedWithoutStage() {
        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertFalse(book.isItemLocked(id("minecraft:stick"), Set.of()));
    }

    @Test
    void givePickupCraftShareSamePredicate() {
        Set<ResourceLocation> none = Set.of();
        assertTrue(book.isItemLocked(NETHERITE_INGOT, none));
        assertTrue(CraftGate.shouldBlock(book.isItemLocked(NETHERITE_INGOT, none), false));
    }

    @Test
    void unlockedWithStage() {
        assertFalse(book.isItemLocked(NETHERITE_INGOT, Set.of(INTRO_NETHER)));
    }

    @Test
    void removePlansDropOfLockedSlots() {
        List<Integer> slots = InventoryPlan.slotsToDrop(
                List.of(NETHERITE_INGOT, id("minecraft:stick"), NETHERITE_INGOT),
                book,
                Set.of()
        );
        assertEquals(List.of(0, 2), slots);
    }

    @Test
    void tierStagesLockDistinctItems() {
        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
        assertTrue(book.isItemLocked(EMERALD, Set.of()));
        assertTrue(book.isItemLocked(GOLDEN_APPLE, Set.of()));

        assertFalse(book.isItemLocked(DIAMOND, Set.of(TIER_EARLY)));
        assertFalse(book.isItemLocked(EMERALD, Set.of(TIER_MID)));
        assertFalse(book.isItemLocked(GOLDEN_APPLE, Set.of(TIER_LATE)));
    }

    @Test
    void multiStageItemUnlocksWithEitherReason() {
        ResourceLocation stageA = id("tutorial:a");
        ResourceLocation stageB = id("tutorial:b");
        StageBook multi = new StageBook(TutorialFixtures.tutorialCatalog());
        multi.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, itemsJson(NETHERITE_INGOT)),
                stageB, StageDefinition.fromJson(stageB, itemsJson(NETHERITE_INGOT))
        ));
        assertTrue(multi.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertFalse(multi.isItemLocked(NETHERITE_INGOT, Set.of(stageA)));
        assertFalse(multi.isItemLocked(NETHERITE_INGOT, Set.of(stageB)));
    }

    private static JsonObject itemsJson(ResourceLocation item) {
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add(item.toString());
        json.add("items", items);
        return json;
    }
}
