package com.gabinx.chapters;

import com.gabinx.chapters.logic.StageAccounts;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.RECIPE_PICKAXE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageAccountsTest {
    @Test
    void aliceAndBobIsolatedWithoutTeam() {
        StageAccounts accounts = new StageAccounts();
        accounts.add("alice", INTRO_NETHER);
        assertTrue(accounts.has("alice", INTRO_NETHER));
        assertFalse(accounts.has("bob", INTRO_NETHER));
        assertTrue(accounts.shouldEmitPerPlayerDelta("alice"));
    }

    @Test
    void partySharesUnlocks() {
        StageAccounts accounts = new StageAccounts();
        accounts.ensurePersonalTeam("alice", "alice-personal");
        accounts.ensurePersonalTeam("bob", "bob-personal");
        accounts.createParty("party");
        accounts.joinParty("alice", "party", "alice-personal");
        accounts.joinParty("bob", "party", "bob-personal");

        accounts.add("alice", INTRO_NETHER);
        assertTrue(accounts.has("bob", INTRO_NETHER));
        assertFalse(accounts.shouldEmitPerPlayerDelta("alice"));
    }

    @Test
    void leaveCopiesPartyOntoPersonal() {
        StageAccounts accounts = new StageAccounts();
        accounts.ensurePersonalTeam("bob", "bob-personal");
        accounts.createParty("party");
        accounts.joinParty("bob", "party", "bob-personal");
        accounts.add("bob", INTRO_NETHER);

        accounts.leaveParty("bob", "party", "bob-personal");
        assertTrue(accounts.has("bob", INTRO_NETHER));
        assertEquals(Set.of(INTRO_NETHER), accounts.of("bob"));
    }

    @Test
    void migrateLegacyOntoPersonalNotParty() {
        StageAccounts accounts = new StageAccounts();
        Set<Identifier> legacy = Set.of(INTRO_NETHER, RECIPE_PICKAXE);

        assertEquals(0, accounts.migrateLegacy("alice", "party", true, legacy));
        assertEquals(2, accounts.migrateLegacy("alice", "alice-personal", false, legacy));
        assertTrue(accounts.has("alice", INTRO_NETHER));
        assertTrue(accounts.has("alice", RECIPE_PICKAXE));
    }

    @Test
    void mergeOnJoinUnionsAllSources() {
        Set<Identifier> merged = StageAccounts.mergeOnPartyJoin(
                Set.of(INTRO_NETHER),
                Set.of(RECIPE_PICKAXE),
                Set.of(INTRO_NETHER)
        );
        assertEquals(Set.of(INTRO_NETHER, RECIPE_PICKAXE), merged);
    }
}
