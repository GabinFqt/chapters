package com.gabinx.chapters;

import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.APPLE;
import static com.gabinx.chapters.TutorialFixtures.DIAMOND;
import static com.gabinx.chapters.TutorialFixtures.EMERALD;
import static com.gabinx.chapters.TutorialFixtures.GOLDEN_APPLE;
import static com.gabinx.chapters.TutorialFixtures.GOLD_INGOT;
import static com.gabinx.chapters.TutorialFixtures.NETHERITE_INGOT;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeStagesTest {
    @Test
    void scriptPickupsLocksListedItems() {
        ResourceLocation stage = id("chapters:script_pickups");
        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, itemsJson(
                DIAMOND, EMERALD, GOLDEN_APPLE, GOLD_INGOT, APPLE
        ))));

        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
        assertTrue(book.isItemLocked(APPLE, Set.of()));
        assertFalse(book.isItemLocked(DIAMOND, Set.of(stage)));
    }

    @Test
    void scriptVanillaAllLocksNamespace() {
        ResourceLocation stage = id("chapters:script_vanilla_all");
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add("@minecraft");
        json.add("items", items);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
        assertFalse(book.isItemLocked(NETHERITE_INGOT, Set.of(stage)));
    }

    @Test
    void runtimeOverridesSameDatapackId() {
        ResourceLocation stage = id("tutorial:intro_nether");
        StageBook book = TutorialFixtures.loadTutorialBookUnchecked();
        assertTrue(book.isItemLocked(NETHERITE_INGOT, Set.of()));

        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        items.add(DIAMOND.toString());
        json.add("items", items);
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertFalse(book.isItemLocked(NETHERITE_INGOT, Set.of()));
        assertTrue(book.isItemLocked(DIAMOND, Set.of()));
    }

    private static JsonObject itemsJson(ResourceLocation... itemIds) {
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        for (ResourceLocation item : itemIds) {
            items.add(item.toString());
        }
        json.add("items", items);
        return json;
    }
}
