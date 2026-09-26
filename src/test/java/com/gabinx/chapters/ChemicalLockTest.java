package com.gabinx.chapters;

import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.HYDROGEN;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChemicalLockTest {
    @Test
    void hydrogenLockedViaSameRule() {
        ResourceLocation stage = id("chapters:script_mek_hydrogen");
        JsonObject json = new JsonObject();
        JsonArray chemicals = new JsonArray();
        chemicals.add(HYDROGEN.toString());
        json.add("chemicals", chemicals);

        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.setRuntimeDefinitions(List.of(StageDefinition.fromJson(stage, json)));

        assertTrue(book.isChemicalLocked(HYDROGEN, Set.of()));
        assertFalse(book.isChemicalLocked(HYDROGEN, Set.of(stage)));
    }

    @Test
    void chemicalIndexEmptyWhenNoneDeclared() throws Exception {
        StageBook book = TutorialFixtures.loadTutorialBook();
        assertTrue(book.chemicalStagesIndexView().isEmpty());
        assertFalse(book.isChemicalLocked(HYDROGEN, Set.of()));
    }
}
