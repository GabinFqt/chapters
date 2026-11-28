package com.gabinx.chapters;

import com.gabinx.chapters.logic.ContentCatalog;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageBookRebuildTest {
    private static final ResourceLocation THE_END = id("minecraft:the_end");

    @Test
    void namespaceEntriesScanTheRegistryOncePerRebuild() {
        CountingCatalog catalog = new CountingCatalog();
        StageBook book = new StageBook(catalog);
        Map<ResourceLocation, StageDefinition> defs = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            ResourceLocation stage = id("test:ns_" + i);
            defs.put(stage, StageDefinition.fromJson(stage, json("items", "@mymod")));
        }
        book.replaceDatapack(defs);
        assertEquals(1, catalog.namespaceScans, "five stages share one @mymod lookup");
        assertTrue(book.isItemLocked(id("mymod:gear"), Set.of()));

        book.replaceDatapack(defs);
        assertEquals(2, catalog.namespaceScans, "a new rebuild must see registry changes, so it scans again");
    }

    @Test
    void rebuildPicksUpDimensionsThatAppearedAfterTheFirstLoad() {
        CountingCatalog catalog = new CountingCatalog();
        StageBook book = new StageBook(catalog);
        ResourceLocation stage = id("test:end_tag");
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json("dimensions", "#test:ends"))));
        assertTrue(book.usesDimensionExpansion());
        assertFalse(book.isDimensionLocked(THE_END, Set.of()), "no server yet: the tag resolves to nothing");

        catalog.serverStarted = true;
        book.rebuild();
        assertTrue(book.isDimensionLocked(THE_END, Set.of()), "after the server starts the tag resolves");
        assertFalse(book.isDimensionLocked(THE_END, Set.of(stage)));
    }

    @Test
    void plainIdsDoNotNeedTheServer() {
        StageBook book = new StageBook(new CountingCatalog());
        ResourceLocation stage = id("test:end_id");
        book.replaceDatapack(Map.of(stage, StageDefinition.fromJson(stage, json("dimensions", THE_END.toString()))));
        assertFalse(book.usesDimensionExpansion());
    }

    private static JsonObject json(String key, String entry) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        array.add(entry);
        json.add(key, array);
        return json;
    }

    /** Counts namespace scans; resolves dimension tags only once {@code serverStarted} is set. */
    private static final class CountingCatalog implements ContentCatalog {
        int namespaceScans;
        boolean serverStarted;

        @Override
        public Set<ResourceLocation> itemsInTag(ResourceLocation tagId) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> itemsInNamespace(String namespace) {
            namespaceScans++;
            return new HashSet<>(Set.of(id(namespace + ":gear"), id(namespace + ":plate")));
        }

        @Override
        public Set<ResourceLocation> fluidsInTag(ResourceLocation tagId) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> fluidsInNamespace(String namespace) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> chemicalsInTag(ResourceLocation tagId) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> chemicalsInNamespace(String namespace) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> dimensionsInTag(ResourceLocation tagId) {
            return serverStarted ? Set.of(THE_END) : Set.of();
        }

        @Override
        public Set<ResourceLocation> dimensionsInNamespace(String namespace) {
            return Set.of();
        }

        @Override
        public Set<ResourceLocation> bucketItemsForFluid(ResourceLocation fluidKindId) {
            return Set.of();
        }
    }
}
