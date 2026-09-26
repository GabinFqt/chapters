package com.gabinx.chapters;

import com.gabinx.chapters.logic.CraftGate;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.DIAMOND;
import static com.gabinx.chapters.TutorialFixtures.DIAMOND_PICKAXE;
import static com.gabinx.chapters.TutorialFixtures.RECIPE_PICKAXE;
import static com.gabinx.chapters.TutorialFixtures.TIER_EARLY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeLockTest {
    private StageBook book;

    @BeforeEach
    void setUp() throws IOException {
        // Only the recipe stage: diamants are unlocked so we can assert recipe-only gating.
        book = new StageBook(TutorialFixtures.tutorialCatalog());
        Map<net.minecraft.resources.ResourceLocation, StageDefinition> all = TutorialFixtures.loadTutorialDefinitions();
        book.replaceDatapack(Map.of(RECIPE_PICKAXE, all.get(RECIPE_PICKAXE)));
    }

    @Test
    void diamondPickaxeRecipeLockedWhileDiamondsAreNot() {
        assertFalse(book.isItemLocked(DIAMOND, Set.of()));
        assertTrue(book.isRecipeLocked(DIAMOND_PICKAXE, Set.of()));
        assertTrue(CraftGate.shouldBlock(false, book.isRecipeLocked(DIAMOND_PICKAXE, Set.of())));
    }

    @Test
    void unlockedWithRecipeStage() {
        assertFalse(book.isRecipeLocked(DIAMOND_PICKAXE, Set.of(RECIPE_PICKAXE)));
        assertFalse(CraftGate.shouldBlock(false, book.isRecipeLocked(DIAMOND_PICKAXE, Set.of(RECIPE_PICKAXE))));
    }

    @Test
    void recipeStaysLockedEvenWhenTierEarlyUnlocksDiamonds() throws IOException {
        StageBook full = TutorialFixtures.loadTutorialBook();
        assertFalse(full.isItemLocked(DIAMOND, Set.of(TIER_EARLY)));
        assertTrue(full.isRecipeLocked(DIAMOND_PICKAXE, Set.of(TIER_EARLY)));
    }
}
