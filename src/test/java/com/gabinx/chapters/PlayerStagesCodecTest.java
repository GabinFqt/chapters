package com.gabinx.chapters;

import com.gabinx.chapters.stage.PlayerStages;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.gabinx.chapters.TutorialFixtures.INTRO_NETHER;
import static com.gabinx.chapters.TutorialFixtures.RECIPE_PICKAXE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStagesCodecTest {
    @Test
    void roundTripPreservesStages() {
        PlayerStages original = new PlayerStages(List.of(INTRO_NETHER, RECIPE_PICKAXE));
        var encoded = PlayerStages.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        PlayerStages decoded = PlayerStages.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original.view(), decoded.view());
        assertTrue(decoded.has(INTRO_NETHER));
        assertTrue(decoded.has(RECIPE_PICKAXE));
    }

    @Test
    void emptyRoundTrip() {
        PlayerStages empty = PlayerStages.empty();
        var encoded = PlayerStages.CODEC.encodeStart(JsonOps.INSTANCE, empty).getOrThrow();
        PlayerStages decoded = PlayerStages.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertTrue(decoded.view().isEmpty());
    }
}
