package com.gabinx.chapters;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.logic.Restriction;
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
        ClientStageView view = TutorialFixtures.mirror(book);
        Set<?> lockedItems = view.hidden(book.catalog()).items();
        assertTrue(lockedItems.contains(WATER_BUCKET));
        assertTrue(lockedItems.contains(LAVA_BUCKET));

        view.addStage(FLUIDES_BASE);
        lockedItems = view.hidden(book.catalog()).items();
        assertFalse(lockedItems.contains(WATER_BUCKET));
        assertFalse(lockedItems.contains(LAVA_BUCKET));
    }

    @Test
    void filledBucketGatingViaBook() {
        assertTrue(book.isItemOrFluidBucketRestricted(WATER_BUCKET, WATER, Set.of(), Restriction.INVENTORY));
        assertFalse(book.isItemOrFluidBucketRestricted(WATER_BUCKET, WATER, Set.of(FLUIDES_BASE), Restriction.INVENTORY));
        assertFalse(book.isItemOrFluidBucketRestricted(WATER_BUCKET, null, Set.of(), Restriction.INVENTORY),
                "the bucket item itself is not listed, only its fluid");
    }
}
