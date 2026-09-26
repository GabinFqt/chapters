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
        view = new ClientStageView();
        view.replaceIndices(
                book.itemStagesIndexView(),
                book.fluidStagesIndexView(),
                book.chemicalStagesIndexView(),
                book.recipeStagesIndexView()
        );
    }

    @Test
    void withoutStagesEverythingLockedIsHidden() {
        assertTrue(view.lockedItemIds(book.catalog()).contains(NETHERITE_INGOT));
        assertTrue(view.lockedFluidIds().contains(WATER));
        assertTrue(view.lockedRecipeIds().contains(DIAMOND_PICKAXE));
    }

    @Test
    void addRevealsWithoutRestart() {
        view.addStage(INTRO_NETHER);
        assertFalse(view.lockedItemIds(book.catalog()).contains(NETHERITE_INGOT));

        view.addStage(RECIPE_PICKAXE);
        assertFalse(view.lockedRecipeIds().contains(DIAMOND_PICKAXE));

        view.addStage(FLUIDES_BASE);
        assertFalse(view.lockedFluidIds().contains(WATER));
    }

    @Test
    void removeRecaches() {
        view.setStages(Set.of(INTRO_NETHER, RECIPE_PICKAXE, FLUIDES_BASE));
        view.removeStage(INTRO_NETHER);
        assertTrue(view.lockedItemIds(book.catalog()).contains(NETHERITE_INGOT));
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
        ClientStageView multiView = new ClientStageView();
        multiView.replaceIndices(
                multi.itemStagesIndexView(),
                multi.fluidStagesIndexView(),
                multi.chemicalStagesIndexView(),
                multi.recipeStagesIndexView()
        );
        multiView.setStages(Set.of(stageA, stageB));
        assertFalse(multiView.lockedItemIds(multi.catalog()).contains(NETHERITE_INGOT));
        multiView.removeStage(stageA);
        assertFalse(multiView.lockedItemIds(multi.catalog()).contains(NETHERITE_INGOT));
        multiView.removeStage(stageB);
        assertTrue(multiView.lockedItemIds(multi.catalog()).contains(NETHERITE_INGOT));
    }

    private static JsonObject itemsJson(ResourceLocation item) {
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add(item.toString());
        json.add("items", items);
        return json;
    }
}
