package com.gabinx.chapters;

import com.gabinx.chapters.logic.StageAccounts;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.RECIPE_PICKAXE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules the FTB Teams listeners apply; the listeners themselves run in {@code TeamGameTests}. */
class StageAccountsTest {
    @Test
    void mergeOnJoinUnionsAllSources() {
        Set<ResourceLocation> merged = StageAccounts.mergeOnPartyJoin(
                Set.of(INTRO_NETHER),
                Set.of(RECIPE_PICKAXE),
                Set.of(INTRO_NETHER)
        );
        assertEquals(Set.of(INTRO_NETHER, RECIPE_PICKAXE), merged);
    }

    @Test
    void mergeOnJoinToleratesMissingSources() {
        assertEquals(Set.of(INTRO_NETHER), StageAccounts.mergeOnPartyJoin(null, Set.of(INTRO_NETHER), null));
    }

    @Test
    void leaveKeepsACopyOfThePartyStages() {
        Set<ResourceLocation> party = new java.util.LinkedHashSet<>(Set.of(INTRO_NETHER));
        Set<ResourceLocation> copy = StageAccounts.stagesAfterPartyLeave(party);
        party.add(RECIPE_PICKAXE);
        assertEquals(Set.of(INTRO_NETHER), copy, "later party changes must not reach the copy");
        assertEquals(Set.of(), StageAccounts.stagesAfterPartyLeave(null));
    }

    @Test
    void migrateLegacyOnlyOntoAPersonalTeamWithStages() {
        assertTrue(StageAccounts.shouldMigrateLegacy(false, false));
        assertFalse(StageAccounts.shouldMigrateLegacy(true, false), "never leak personal stages into a party");
        assertFalse(StageAccounts.shouldMigrateLegacy(false, true), "nothing to migrate");
    }
}
