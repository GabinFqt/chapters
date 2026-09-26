package com.gabinx.chapters;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.StageBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.FLUIDES_BASE;
import static com.gabinx.chapters.TutorialFixtures.LAVA;
import static com.gabinx.chapters.TutorialFixtures.LAVA_BUCKET;
import static com.gabinx.chapters.TutorialFixtures.WATER;
import static com.gabinx.chapters.TutorialFixtures.WATER_BUCKET;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidLockTest {
    private StageBook book;

    @BeforeEach
    void setUp() throws IOException {
        book = TutorialFixtures.loadTutorialBook();
    }

    @Test
    void waterAndLavaLockedWithoutStage() {
        assertTrue(book.isFluidLocked(WATER, Set.of()));
        assertTrue(book.isFluidLocked(LAVA, Set.of()));
    }

    @Test
    void transferBlockedMeansFluidLocked() {
        assertTrue(book.isFluidLocked(WATER, Set.of()));
        assertFalse(book.isFluidLocked(WATER, Set.of(FLUIDES_BASE)));
    }

    @Test
    void bucketsCountAsLockedItemsInViewer() {
        ClientStageView view = new ClientStageView();
        view.replaceIndices(
                book.itemStagesIndexView(),
                book.fluidStagesIndexView(),
                book.chemicalStagesIndexView(),
                book.recipeStagesIndexView()
        );
        Set<?> lockedItems = view.lockedItemIds(book.catalog());
        assertTrue(lockedItems.contains(WATER_BUCKET));
        assertTrue(lockedItems.contains(LAVA_BUCKET));

        view.addStage(FLUIDES_BASE);
        lockedItems = view.lockedItemIds(book.catalog());
        assertFalse(lockedItems.contains(WATER_BUCKET));
        assertFalse(lockedItems.contains(LAVA_BUCKET));
    }

    @Test
    void filledBucketGatingViaBook() {
        assertTrue(book.isItemOrFluidBucketLocked(WATER_BUCKET, Set.of()));
        assertFalse(book.isItemOrFluidBucketLocked(WATER_BUCKET, Set.of(FLUIDES_BASE)));
    }
}
