package com.gabinx.chapters.compat.kubejs;

import com.gabinx.chapters.logic.RestrictionFlags;
import com.gabinx.chapters.stage.StageDefinition;
import com.gabinx.chapters.stage.StageManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChaptersKubeJSBridge {
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, List<Object>> RAW = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RestrictionFlags> STAGE_DEFAULTS = new ConcurrentHashMap<>();
    private static volatile boolean flushScheduled;

    private ChaptersKubeJSBridge() {
    }

    public static void defineStage(ResourceLocation id, Collection<?> entries) {
        defineStage(id, entries, RestrictionFlags.ALL);
    }

    public static void defineStage(ResourceLocation id, Collection<?> entries, RestrictionFlags defaults) {
        RAW.put(id, new ArrayList<>(entries));
        if (defaults == null || defaults.equals(RestrictionFlags.ALL)) {
            STAGE_DEFAULTS.remove(id);
        } else {
            STAGE_DEFAULTS.put(id, defaults);
        }
        flushScheduled = true;
    }

    public static void clearStage(ResourceLocation id) {
        RAW.remove(id);
        STAGE_DEFAULTS.remove(id);
        flushScheduled = true;
    }

    /**
     * Flushes deferred stage definitions once per server tick so scripts can call {@link #defineStage} many times in one
     * {@code ServerEvents.loaded} callback without rebuilding indices repeatedly.
     */
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (!flushScheduled) {
            return;
        }
        flushScheduled = false;
        flushToStageManager();
    }

    private static void flushToStageManager() {
        var defs = new ArrayList<StageDefinition>();
        for (var entry : RAW.entrySet()) {
            ResourceLocation id = entry.getKey();
            RestrictionFlags defaults = STAGE_DEFAULTS.getOrDefault(id, RestrictionFlags.ALL);
            defs.add(buildDefinition(id, entry.getValue(), defaults));
        }
        StageManager.get().setRuntimeDefinitions(defs);
    }

    private static StageDefinition buildDefinition(
            ResourceLocation id, Collection<?> entries, RestrictionFlags defaults) {
        var items = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var tags = new LinkedHashMap<TagKey<Item>, RestrictionFlags>();
        var namespaces = new LinkedHashMap<String, RestrictionFlags>();
        var fluids = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var fluidTags = new LinkedHashMap<TagKey<Fluid>, RestrictionFlags>();
        var fluidNamespaces = new LinkedHashMap<String, RestrictionFlags>();
        var chemicals = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var chemicalTags = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var chemicalNamespaces = new LinkedHashMap<String, RestrictionFlags>();
        var recipes = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var dimensions = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var dimensionTags = new LinkedHashMap<ResourceLocation, RestrictionFlags>();
        var dimensionNamespaces = new LinkedHashMap<String, RestrictionFlags>();

        for (Object lock : entries) {
            if (lock == null) {
                continue;
            }
            String raw;
            RestrictionFlags flags = defaults;
            if (lock instanceof Map<?, ?> map) {
                JsonObject obj = GSON.toJsonTree(map).getAsJsonObject();
                JsonElement idEl = obj.has("id") ? obj.get("id") : obj.get("item");
                if (idEl == null || !idEl.isJsonPrimitive()) {
                    continue;
                }
                raw = idEl.getAsString();
                flags = defaults.withOverrides(obj);
            } else {
                raw = lock.toString();
            }
            if (raw == null) {
                continue;
            }

            if (raw.regionMatches(true, 0, "fluid:", 0, 6)) {
                StageDefinition.accumulateFluidEntry(
                        raw.substring(6).trim(), flags, fluids, fluidTags, fluidNamespaces);
            } else if (raw.regionMatches(true, 0, "chemical:", 0, 9)) {
                StageDefinition.accumulateChemicalEntry(
                        raw.substring(9).trim(), flags, chemicals, chemicalTags, chemicalNamespaces);
            } else if (raw.regionMatches(true, 0, "recipe:", 0, 7)) {
                StageDefinition.accumulateRecipeEntry(raw.substring(7).trim(), flags, recipes);
            } else if (raw.regionMatches(true, 0, "dimension:", 0, 10)) {
                StageDefinition.accumulateDimensionEntry(
                        raw.substring(10).trim(), flags, dimensions, dimensionTags, dimensionNamespaces);
            } else {
                StageDefinition.accumulateEntry(raw, flags, items, tags, namespaces);
                if (raw.trim().startsWith("@")) {
                    StageDefinition.accumulateFluidEntry(raw.trim(), flags, fluids, fluidTags, fluidNamespaces);
                    StageDefinition.accumulateChemicalEntry(
                            raw.trim(), flags, chemicals, chemicalTags, chemicalNamespaces);
                }
            }
        }

        return new StageDefinition(
                id,
                items,
                tags,
                namespaces,
                fluids,
                fluidTags,
                fluidNamespaces,
                chemicals,
                chemicalTags,
                chemicalNamespaces,
                recipes,
                dimensions,
                dimensionTags,
                dimensionNamespaces);
    }
}
