package com.gabinx.chapters.perf;

import com.gabinx.chapters.logic.FixedContentCatalog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic synthetic "big modpack": 250 mods, 40k items, 2k cross-mod tags, 400 fluids (with buckets),
 * 100 chemicals, 60k recipe ids and 120 stages mixing namespace, tag and explicit-id locks.
 */
final class PerfScenario {
    static final int NAMESPACES = 250;
    static final int ITEMS_PER_NAMESPACE = 160;
    static final int ITEM_TAGS = 2_000;
    static final int TAG_SIZE = 25;
    static final int FLUIDS = 400;
    static final int CHEMICALS = 100;
    static final int RECIPES = 60_000;

    static final int NAMESPACE_STAGES = 20;
    static final int TAG_STAGES = 40;
    static final int EXPLICIT_STAGES = 60;
    static final int ITEMS_PER_EXPLICIT_STAGE = 250;
    static final int RECIPES_PER_EXPLICIT_STAGE = 100;

    final FixedContentCatalog catalog = new FixedContentCatalog();
    final List<ResourceLocation> items = new ArrayList<>();
    final List<ResourceLocation> fluids = new ArrayList<>();
    final List<ResourceLocation> chemicals = new ArrayList<>();
    final List<ResourceLocation> recipes = new ArrayList<>();
    final List<ResourceLocation> stageIds = new ArrayList<>();
    final Map<ResourceLocation, JsonObject> stageJson = new LinkedHashMap<>();
    /** Every other stage: a mid-game player. */
    final Set<ResourceLocation> ownedStages = new LinkedHashSet<>();
    /** Recipe id → output item id (for recipe-viewer simulations). */
    final Map<ResourceLocation, ResourceLocation> recipeOutputs = new LinkedHashMap<>();

    PerfScenario() {
        Random random = new Random(42);

        for (int ns = 0; ns < NAMESPACES; ns++) {
            for (int i = 0; i < ITEMS_PER_NAMESPACE; i++) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(mod(ns), "item_" + i);
                items.add(id);
                catalog.addItem(id);
            }
        }
        for (int f = 0; f < FLUIDS; f++) {
            String ns = mod(f % NAMESPACES);
            ResourceLocation fluid = ResourceLocation.fromNamespaceAndPath(ns, "fluid_" + f);
            ResourceLocation bucket = ResourceLocation.fromNamespaceAndPath(ns, "fluid_" + f + "_bucket");
            fluids.add(fluid);
            items.add(bucket);
            catalog.setBucket(fluid, bucket);
        }
        for (int c = 0; c < CHEMICALS; c++) {
            ResourceLocation chemical = ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_" + c);
            chemicals.add(chemical);
            catalog.addChemical(chemical);
        }
        for (int t = 0; t < ITEM_TAGS; t++) {
            ResourceLocation[] members = new ResourceLocation[TAG_SIZE];
            for (int i = 0; i < TAG_SIZE; i++) {
                members[i] = items.get(random.nextInt(items.size()));
            }
            catalog.addItemTag(tagId(t), members);
        }
        for (int r = 0; r < RECIPES; r++) {
            ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath(mod(r % NAMESPACES), "recipe_" + r);
            recipes.add(recipe);
            recipeOutputs.put(recipe, items.get(random.nextInt(items.size())));
        }

        int stageIndex = 0;
        for (int s = 0; s < NAMESPACE_STAGES; s++, stageIndex++) {
            JsonObject json = new JsonObject();
            JsonArray namespaces = new JsonArray();
            namespaces.add(mod(2 * s));
            namespaces.add(mod(2 * s + 1));
            json.add("namespaces", namespaces);
            addStage("ns_" + s, json, stageIndex);
        }
        for (int s = 0; s < TAG_STAGES; s++, stageIndex++) {
            JsonObject json = new JsonObject();
            JsonArray entries = new JsonArray();
            for (int j = 0; j < 10; j++) {
                entries.add("#" + tagId(10 * s + j));
            }
            json.add("items", entries);
            addStage("tags_" + s, json, stageIndex);
        }
        for (int s = 0; s < EXPLICIT_STAGES; s++, stageIndex++) {
            JsonObject json = new JsonObject();
            JsonArray itemEntries = new JsonArray();
            for (int i = 0; i < ITEMS_PER_EXPLICIT_STAGE; i++) {
                String id = items.get(random.nextInt(items.size())).toString();
                if (i % 10 == 0) {
                    JsonObject soft = new JsonObject();
                    soft.addProperty("id", id);
                    soft.addProperty("hideInJei", false);
                    itemEntries.add(soft);
                } else {
                    itemEntries.add(id);
                }
            }
            json.add("items", itemEntries);
            JsonArray recipeEntries = new JsonArray();
            for (int i = 0; i < RECIPES_PER_EXPLICIT_STAGE; i++) {
                recipeEntries.add(recipes.get(random.nextInt(recipes.size())).toString());
            }
            json.add("recipes", recipeEntries);
            JsonArray fluidEntries = new JsonArray();
            for (int i = 0; i < 4; i++) {
                fluidEntries.add(fluids.get(random.nextInt(fluids.size())).toString());
            }
            json.add("fluids", fluidEntries);
            JsonArray chemicalEntries = new JsonArray();
            for (int i = 0; i < 2; i++) {
                chemicalEntries.add(chemicals.get(random.nextInt(chemicals.size())).toString());
            }
            json.add("chemicals", chemicalEntries);
            addStage("explicit_" + s, json, stageIndex);
        }
    }

    private void addStage(String name, JsonObject json, int index) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("perfpack", name);
        stageIds.add(id);
        stageJson.put(id, json);
        if (index % 2 == 0) {
            ownedStages.add(id);
        }
    }

    private static String mod(int ns) {
        return "examplemod" + ns;
    }

    private static ResourceLocation tagId(int t) {
        return ResourceLocation.fromNamespaceAndPath("c", "group_" + t);
    }
}
