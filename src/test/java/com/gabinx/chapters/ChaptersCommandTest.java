package com.gabinx.chapters;

import com.gabinx.chapters.logic.CraftGate;
import com.gabinx.chapters.logic.InventoryPlan;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.logic.StageCommandActions;
import com.gabinx.chapters.stage.PlayerStages;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaptersCommandTest {
    private StageBook book;
    private PlayerStages stages;

    @BeforeEach
    void setUp() throws IOException {
        book = TutorialFixtures.loadTutorialBook();
        stages = PlayerStages.empty();
    }

    @Test
    void addAcceptsOnlyDefinedStages() {
        assertTrue(StageCommandActions.canAdd(book.stageIds(), INTRO_NETHER));
        assertFalse(StageCommandActions.canAdd(book.stageIds(), id("foo:bar")));
    }

    @Test
    void removeAcceptsAStageThePlayerKeptAfterItWasDeleted() {
        Identifier deleted = id("tutorial:deleted_stage");
        assertFalse(StageCommandActions.canRemove(book.stageIds(), Set.of(), deleted), "unknown and not owned");
        assertTrue(StageCommandActions.canRemove(book.stageIds(), Set.of(deleted), deleted), "still owned");
        assertTrue(StageCommandActions.canRemove(book.stageIds(), Set.of(), INTRO_NETHER), "defined");
    }

    @Test
    void reloadUpdatesLocks() {
        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));

        JsonObject empty = new JsonObject();
        empty.add("items", new JsonArray());
        Identifier other = id("tutorial:empty_reload");
        book.replaceDatapack(Map.of(other, StageDefinition.fromJson(other, empty)));

        assertFalse(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertTrue(book.stageIds().contains(other));
        assertFalse(book.stageIds().contains(INTRO_NETHER));
    }

    @Test
    void craftGateMatchesCommandLockedState() {
        assertTrue(CraftGate.shouldBlock(book.isItemLocked(NETHERITE_INGOT, stages.view()), false));
        stages.add(INTRO_NETHER);
        assertFalse(CraftGate.shouldBlock(book.isItemLocked(NETHERITE_INGOT, stages.view()), false));
    }

    @Test
    void removePlansInventoryDrop() {
        stages.add(INTRO_NETHER);
        stages.remove(INTRO_NETHER);
        List<Integer> slots = InventoryPlan.slotsToDrop(
                java.util.Arrays.asList(NETHERITE_INGOT, null, id("minecraft:stick")),
                book,
                stages.view(),
                TutorialFixtures::bucketFluid
        );
        assertEquals(List.of(0), slots);
    }
}
