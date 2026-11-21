package com.gabinx.chapters;

import com.gabinx.chapters.logic.ParsedStageCache;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.TIER_EARLY;
import static com.gabinx.chapters.TutorialFixtures.TIER_MID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParsedStageCacheTest {
    @Test
    void reusesParsedSetWhileStringsUnchanged() {
        ParsedStageCache<String> cache = new ParsedStageCache<>();
        Set<String> raw = new HashSet<>(Set.of(TIER_EARLY.toString()));

        Set<?> first = cache.get("team", raw);
        assertEquals(Set.of(TIER_EARLY), first);
        assertSame(first, cache.get("team", new HashSet<>(raw)));
    }

    @Test
    void inPlaceMutationIsSeen() {
        ParsedStageCache<String> cache = new ParsedStageCache<>();
        Set<String> raw = new HashSet<>(Set.of(TIER_EARLY.toString()));
        cache.get("team", raw);

        raw.add(TIER_MID.toString());
        raw.add("Not A Valid Id");
        assertEquals(Set.of(TIER_EARLY, TIER_MID), cache.get("team", raw));

        raw.clear();
        assertEquals(Set.of(), cache.get("team", raw));
    }
}
