package com.gabinx.chapters;

import com.gabinx.chapters.logic.FixedContentCatalog;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared fixtures: tutorial datapack JSON + a fixed catalog covering those examples.
 */
public final class TutorialFixtures {
    public static final Identifier INTRO_NETHER = id("tutorial:intro_nether");
    public static final Identifier RECIPE_PICKAXE = id("tutorial:recipe_pickaxe");
    public static final Identifier FLUIDES_BASE = id("tutorial:fluides_base");
    public static final Identifier TIER_EARLY = id("tutorial:tier_early");
    public static final Identifier TIER_MID = id("tutorial:tier_mid");
    public static final Identifier TIER_LATE = id("tutorial:tier_late");

    public static final Identifier NETHERITE_INGOT = id("minecraft:netherite_ingot");
    public static final Identifier DIAMOND = id("minecraft:diamond");
    public static final Identifier EMERALD = id("minecraft:emerald");
    public static final Identifier GOLDEN_APPLE = id("minecraft:golden_apple");
    public static final Identifier DIAMOND_PICKAXE = id("minecraft:diamond_pickaxe");
    public static final Identifier WATER = id("minecraft:water");
    public static final Identifier LAVA = id("minecraft:lava");
    public static final Identifier WATER_BUCKET = id("minecraft:water_bucket");
    public static final Identifier LAVA_BUCKET = id("minecraft:lava_bucket");
    public static final Identifier APPLE = id("minecraft:apple");
    public static final Identifier GOLD_INGOT = id("minecraft:gold_ingot");
    public static final Identifier HYDROGEN = id("mekanism:hydrogen");

    private TutorialFixtures() {
    }

    public static Identifier id(String value) {
        return Identifier.parse(value);
    }

    public static FixedContentCatalog tutorialCatalog() {
        return new FixedContentCatalog()
                .addItem(NETHERITE_INGOT)
                .addItem(DIAMOND)
                .addItem(EMERALD)
                .addItem(GOLDEN_APPLE)
                .addItem(DIAMOND_PICKAXE)
                .addItem(APPLE)
                .addItem(GOLD_INGOT)
                .addItem(id("minecraft:stick"))
                .setBucket(WATER, WATER_BUCKET)
                .setBucket(LAVA, LAVA_BUCKET)
                .addChemical(HYDROGEN)
                .addItemTag(id("minecraft:planks"), id("minecraft:oak_planks"), id("minecraft:birch_planks"));
    }

    public static StageBook loadTutorialBook() throws IOException {
        StageBook book = new StageBook(tutorialCatalog());
        book.replaceDatapack(loadTutorialDefinitions());
        return book;
    }

    public static StageBook loadTutorialBookUnchecked() {
        try {
            return loadTutorialBook();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<Identifier, StageDefinition> loadTutorialDefinitions() throws IOException {
        Path dir = tutorialStagesDir();
        Map<Identifier, StageDefinition> out = new LinkedHashMap<>();
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                String name = file.getFileName().toString().replace(".json", "");
                Identifier id = Identifier.fromNamespaceAndPath("tutorial", name);
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    out.put(id, StageDefinition.fromJson(id, json));
                }
            }
        }
        return out;
    }

    public static Path tutorialStagesDir() {
        Path relative = Path.of("examples/datapack/tutorial/data/tutorial/chapters/stages");
        Path cursor = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 8; i++) {
            Path candidate = cursor.resolve(relative);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            Path parent = cursor.getParent();
            if (parent == null || parent.equals(cursor)) {
                break;
            }
            cursor = parent;
        }
        // Classpath fallback: copy under src/test/resources if present
        try {
            var url = TutorialFixtures.class.getResource("/tutorial-stages");
            if (url != null && "file".equals(url.getProtocol())) {
                return Path.of(url.toURI());
            }
        } catch (Exception ignored) {
        }
        throw new IllegalStateException(
                "Cannot find tutorial stages (searched from " + Path.of("").toAbsolutePath() + ")"
        );
    }
}
