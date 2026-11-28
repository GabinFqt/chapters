package com.gabinx.chapters;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.DIAMOND_PICKAXE;
import static com.gabinx.chapters.TutorialFixtures.FLUIDES_BASE;
import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.RECIPE_PICKAXE;
import static com.gabinx.chapters.TutorialFixtures.WATER;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientVisibilityTest {
    private StageBook book;
    private ClientStageView view;

    @BeforeEach
    void setUp() throws IOException {
        book = TutorialFixtures.loadTutorialBook();
        view = TutorialFixtures.mirror(book);
    }

    @Test
    void withoutStagesEverythingLockedIsHidden() {
        assertTrue(view.hidden(book.catalog()).items().contains(NETHERITE_INGOT));
        assertTrue(view.hidden(null).fluids().contains(WATER));
        assertTrue(view.hidden(null).recipes().contains(DIAMOND_PICKAXE));
    }

    @Test
    void addRevealsWithoutRestart() {
        view.addStage(INTRO_NETHER);
        assertFalse(view.hidden(book.catalog()).items().contains(NETHERITE_INGOT));

        view.addStage(RECIPE_PICKAXE);
        assertFalse(view.hidden(null).recipes().contains(DIAMOND_PICKAXE));

        view.addStage(FLUIDES_BASE);
        assertFalse(view.hidden(null).fluids().contains(WATER));
    }

    @Test
    void removeRecaches() {
        view.setStages(Set.of(INTRO_NETHER, RECIPE_PICKAXE, FLUIDES_BASE));
        view.removeStage(INTRO_NETHER);
        assertTrue(view.hidden(book.catalog()).items().contains(NETHERITE_INGOT));
    }

    @Test
    void multiReasonStaysVisibleWhileOneUnlockRemains() {
        ResourceLocation stageA = id("tutorial:vis_a");
        ResourceLocation stageB = id("tutorial:vis_b");
        StageBook multi = new StageBook(TutorialFixtures.tutorialCatalog());
        multi.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, itemsJson(NETHERITE_INGOT)),
                stageB, StageDefinition.fromJson(stageB, itemsJson(NETHERITE_INGOT))
        ));
        ClientStageView multiView = TutorialFixtures.mirror(multi);
        multiView.setStages(Set.of(stageA, stageB));
        assertFalse(multiView.hidden(multi.catalog()).items().contains(NETHERITE_INGOT));
        multiView.removeStage(stageA);
        assertFalse(multiView.hidden(multi.catalog()).items().contains(NETHERITE_INGOT));
        multiView.removeStage(stageB);
        assertTrue(multiView.hidden(multi.catalog()).items().contains(NETHERITE_INGOT));
    }

    private static JsonObject itemsJson(ResourceLocation item) {
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add(item.toString());
        json.add("items", items);
        return json;
    }
}
