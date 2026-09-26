package com.gabinx.chapters;

import com.gabinx.chapters.logic.CraftGate;
import com.gabinx.chapters.logic.InventoryPlan;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.logic.StageCommandActions;
import com.gabinx.chapters.stage.PlayerStages;
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

import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void addCheckListRemoveCycle() {
        assertInstanceOf(
                StageCommandActions.Outcome.Success.class,
                StageCommandActions.add(stages, book.stageIds(), INTRO_NETHER)
        );
        assertTrue(StageCommandActions.check(stages, INTRO_NETHER));
        assertTrue(StageCommandActions.list(stages).contains(INTRO_NETHER));

        assertInstanceOf(
                StageCommandActions.Outcome.Success.class,
                StageCommandActions.remove(stages, book.stageIds(), INTRO_NETHER)
        );
        assertFalse(StageCommandActions.check(stages, INTRO_NETHER));
        assertFalse(StageCommandActions.list(stages).contains(INTRO_NETHER));
    }

    @Test
    void unknownStageRejected() {
        ResourceLocation unknown = id("foo:bar");
        assertInstanceOf(
                StageCommandActions.Outcome.UnknownStage.class,
                StageCommandActions.requireKnown(book.stageIds(), unknown)
        );
        assertInstanceOf(
                StageCommandActions.Outcome.UnknownStage.class,
                StageCommandActions.add(stages, book.stageIds(), unknown)
        );
    }

    @Test
    void reloadUpdatesLocks() {
        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));

        JsonObject empty = new JsonObject();
        empty.add("items", new JsonArray());
        ResourceLocation other = id("tutorial:empty_reload");
        book.replaceDatapack(Map.of(other, StageDefinition.fromJson(other, empty)));

        assertFalse(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertTrue(book.stageIds().contains(other));
        assertFalse(book.stageIds().contains(INTRO_NETHER));
    }

    @Test
    void craftGateMatchesCommandLockedState() {
        assertTrue(CraftGate.shouldBlock(book.isItemLocked(NETHERITE_INGOT, stages.view()), false));
        StageCommandActions.add(stages, book.stageIds(), INTRO_NETHER);
        assertFalse(CraftGate.shouldBlock(book.isItemLocked(NETHERITE_INGOT, stages.view()), false));
    }

    @Test
    void removePlansInventoryDrop() {
        StageCommandActions.add(stages, book.stageIds(), INTRO_NETHER);
        StageCommandActions.remove(stages, book.stageIds(), INTRO_NETHER);
        List<Integer> slots = InventoryPlan.slotsToDrop(
                java.util.Arrays.asList(NETHERITE_INGOT, null, id("minecraft:stick")),
                book,
                stages.view()
        );
        assertEquals(List.of(0), slots);
    }
}
