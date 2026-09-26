package com.gabinx.chapters;

import com.gabinx.chapters.logic.DimensionGate;
import com.gabinx.chapters.logic.FixedContentCatalog;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionLockTest {
    private static final Identifier THE_NETHER = id("minecraft:the_nether");
    private static final Identifier THE_END = id("minecraft:the_end");
    private static final Identifier OVERWORLD = id("minecraft:overworld");
    private static final Identifier MOD_DIM = id("othermod:custom");
    private static final Identifier DIM_TAG = id("chapters:gated_worlds");

    @Test
    void netherLockedWithoutStage() {
        Identifier stage = id("tutorial:intro_dims");
        StageBook book = bookWithDimensions(stage, THE_NETHER, THE_END);

        assertTrue(book.isDimensionLocked(THE_NETHER, Set.of()));
        assertTrue(book.isDimensionLocked(THE_END, Set.of()));
        assertFalse(book.isDimensionLocked(OVERWORLD, Set.of()));
        assertFalse(book.isDimensionLocked(THE_NETHER, Set.of(stage)));
    }

    @Test
    void dimensionLockDoesNotLockItemsOrRecipes() {
        Identifier stage = id("tutorial:intro_dims");
        JsonObject json = new JsonObject();
        JsonArray dimensions = new JsonArray();
        dimensions.add(THE_NETHER.toString());
        json.add("dimensions", dimensions);
        JsonArray items = new JsonArray();
        items.add(NETHERITE_INGOT.toString());
        json.add("items", items);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isDimensionLocked(THE_NETHER, Set.of()));
        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertFalse(book.isRecipeLocked(id("minecraft:diamond_pickaxe"), Set.of()));
    }

    @Test
    void dimensionTagExpandsViaCatalog() {
        Identifier stage = id("tutorial:tag_dims");
        FixedContentCatalog catalog = TutorialFixtures.tutorialCatalog()
                .addDimensionTag(DIM_TAG, THE_NETHER, THE_END);

        JsonObject json = new JsonObject();
        JsonArray dimensions = new JsonArray();
        dimensions.add("#" + DIM_TAG);
        json.add("dimensions", dimensions);

        StageBook book = new StageBook(catalog);
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isDimensionLocked(THE_NETHER, Set.of()));
        assertTrue(book.isDimensionLocked(THE_END, Set.of()));
        assertFalse(book.isDimensionLocked(OVERWORLD, Set.of()));
        assertFalse(book.isDimensionLocked(THE_NETHER, Set.of(stage)));
    }

    @Test
    void dimensionNamespaceExpandsViaCatalog() {
        Identifier stage = id("tutorial:ns_dims");
        FixedContentCatalog catalog = TutorialFixtures.tutorialCatalog()
                .addDimension(MOD_DIM)
                .addDimension(id("othermod:void"));

        JsonObject json = new JsonObject();
        JsonArray dimensions = new JsonArray();
        dimensions.add("@othermod");
        json.add("dimensions", dimensions);

        StageBook book = new StageBook(catalog);
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isDimensionLocked(MOD_DIM, Set.of()));
        assertTrue(book.isDimensionLocked(id("othermod:void"), Set.of()));
        assertFalse(book.isDimensionLocked(THE_NETHER, Set.of()));
    }

    @Test
    void itemAtModDoesNotIndexDimensions() {
        Identifier stage = id("tutorial:item_mod");
        FixedContentCatalog catalog = TutorialFixtures.tutorialCatalog()
                .addDimension(MOD_DIM)
                .addItem(id("othermod:widget"));

        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add("@othermod");
        json.add("items", items);

        StageBook book = new StageBook(catalog);
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isItemLocked(id("othermod:widget"), Set.of()));
        assertFalse(book.isDimensionLocked(MOD_DIM, Set.of()));
    }

    @Test
    void multiStageDimensionUnlocksWithEitherReason() {
        Identifier stageA = id("tutorial:dim_a");
        Identifier stageB = id("tutorial:dim_b");
        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(Map.of(
                stageA, StageDefinition.fromJson(stageA, dimensionsJson(THE_NETHER)),
                stageB, StageDefinition.fromJson(stageB, dimensionsJson(THE_NETHER))
        ));

        assertTrue(book.isDimensionLocked(THE_NETHER, Set.of()));
        assertFalse(book.isDimensionLocked(THE_NETHER, Set.of(stageA)));
        assertFalse(book.isDimensionLocked(THE_NETHER, Set.of(stageB)));
    }

    @Test
    void travelBlockedOnlyWhenDestinationLockedAndDifferent() {
        assertTrue(DimensionGate.shouldBlockTravel(true, false));
        assertFalse(DimensionGate.shouldBlockTravel(true, true));
        assertFalse(DimensionGate.shouldBlockTravel(false, false));
        assertFalse(DimensionGate.shouldBlockTravel(false, true));
    }

    @Test
    void ejectOnlyWhenCurrentLockedAndOverworldFree() {
        assertTrue(DimensionGate.shouldEject(true, false));
        assertFalse(DimensionGate.shouldEject(true, true));
        assertFalse(DimensionGate.shouldEject(false, false));
        assertFalse(DimensionGate.shouldEject(false, true));
    }

    private static StageBook bookWithDimensions(Identifier stage, Identifier... dimensionIds) {
        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, dimensionsJson(dimensionIds))));
        return book;
    }

    private static JsonObject dimensionsJson(Identifier... dimensionIds) {
        JsonObject json = new JsonObject();
        JsonArray dimensions = new JsonArray();
        for (Identifier dimensionId : dimensionIds) {
            dimensions.add(dimensionId.toString());
        }
        json.add("dimensions", dimensions);
        return json;
    }
}
