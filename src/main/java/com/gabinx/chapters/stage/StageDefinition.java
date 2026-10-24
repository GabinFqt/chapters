package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.RestrictionFlags;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StageDefinition(
        Identifier id,
        Map<Identifier, RestrictionFlags> items,
        Map<TagKey<Item>, RestrictionFlags> tags,
        Map<String, RestrictionFlags> namespaces,
        Map<Identifier, RestrictionFlags> fluids,
        Map<TagKey<Fluid>, RestrictionFlags> fluidTags,
        Map<String, RestrictionFlags> fluidNamespaces,
        Map<Identifier, RestrictionFlags> chemicals,
        Map<Identifier, RestrictionFlags> chemicalTags,
        Map<String, RestrictionFlags> chemicalNamespaces,
        /** Crafting (and other) recipe holder ids to lock until the player has this stage. */
        Map<Identifier, RestrictionFlags> recipes,
        /** Dimension ids (e.g. {@code minecraft:the_nether}) gated by this stage. */
        Map<Identifier, RestrictionFlags> dimensions,
        Map<Identifier, RestrictionFlags> dimensionTags,
        Map<String, RestrictionFlags> dimensionNamespaces) {

    public static StageDefinition fromJson(Identifier id, JsonObject json) {
        RestrictionFlags stageDefaults = RestrictionFlags.ALL;
        if (json.has("restrictions") && json.get("restrictions").isJsonObject()) {
            stageDefaults = RestrictionFlags.fromJson(json.getAsJsonObject("restrictions"));
        }

        Map<Identifier, RestrictionFlags> items = new LinkedHashMap<>();
        Map<TagKey<Item>, RestrictionFlags> tags = new LinkedHashMap<>();
        Map<String, RestrictionFlags> namespaces = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> fluids = new LinkedHashMap<>();
        Map<TagKey<Fluid>, RestrictionFlags> fluidTags = new LinkedHashMap<>();
        Map<String, RestrictionFlags> fluidNamespaces = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> chemicals = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> chemicalTags = new LinkedHashMap<>();
        Map<String, RestrictionFlags> chemicalNamespaces = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> recipes = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> dimensions = new LinkedHashMap<>();
        Map<Identifier, RestrictionFlags> dimensionTags = new LinkedHashMap<>();
        Map<String, RestrictionFlags> dimensionNamespaces = new LinkedHashMap<>();

        JsonArray namespacesJson = json.getAsJsonArray("namespaces");
        if (namespacesJson != null) {
            for (JsonElement element : namespacesJson) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry == null) {
                    continue;
                }
                putNamespace(namespaces, entry.raw(), entry.flags());
                putNamespace(fluidNamespaces, entry.raw(), entry.flags());
                putNamespace(chemicalNamespaces, entry.raw(), entry.flags());
            }
        }

        JsonArray fluidNamespacesJson = json.getAsJsonArray("fluid_namespaces");
        if (fluidNamespacesJson != null) {
            for (JsonElement element : fluidNamespacesJson) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    putNamespace(fluidNamespaces, entry.raw(), entry.flags());
                }
            }
        }

        JsonArray chemicalNamespacesJson = json.getAsJsonArray("chemical_namespaces");
        if (chemicalNamespacesJson != null) {
            for (JsonElement element : chemicalNamespacesJson) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    putNamespace(chemicalNamespaces, entry.raw(), entry.flags());
                }
            }
        }

        JsonArray values = json.getAsJsonArray("items");
        if (values != null) {
            for (JsonElement element : values) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry == null) {
                    continue;
                }
                accumulateEntry(entry.raw(), entry.flags(), items, tags, namespaces);
                if (entry.raw().trim().startsWith("@")) {
                    accumulateFluidEntry(entry.raw().trim(), entry.flags(), fluids, fluidTags, fluidNamespaces);
                    accumulateChemicalEntry(
                            entry.raw().trim(), entry.flags(), chemicals, chemicalTags, chemicalNamespaces);
                }
            }
        }

        JsonArray fluidValues = json.getAsJsonArray("fluids");
        if (fluidValues != null) {
            for (JsonElement element : fluidValues) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    accumulateFluidEntry(entry.raw(), entry.flags(), fluids, fluidTags, fluidNamespaces);
                }
            }
        }

        JsonArray chemicalValues = json.getAsJsonArray("chemicals");
        if (chemicalValues != null) {
            for (JsonElement element : chemicalValues) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    accumulateChemicalEntry(entry.raw(), entry.flags(), chemicals, chemicalTags, chemicalNamespaces);
                }
            }
        }

        JsonArray recipeValues = json.getAsJsonArray("recipes");
        if (recipeValues != null) {
            for (JsonElement element : recipeValues) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    accumulateRecipeEntry(entry.raw(), entry.flags(), recipes);
                }
            }
        }

        JsonArray dimensionValues = json.getAsJsonArray("dimensions");
        if (dimensionValues != null) {
            for (JsonElement element : dimensionValues) {
                ParsedEntry entry = parseListEntry(element, stageDefaults);
                if (entry != null) {
                    accumulateDimensionEntry(entry.raw(), entry.flags(), dimensions, dimensionTags, dimensionNamespaces);
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

    /**
     * Accepts a legacy string or an object {@code {"id":"…", "hideInJei":false, …}}.
     */
    static ParsedEntry parseListEntry(JsonElement element, RestrictionFlags stageDefaults) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            String raw = element.getAsString();
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            return new ParsedEntry(raw, stageDefaults);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        String raw = null;
        if (obj.has("id") && obj.get("id").isJsonPrimitive()) {
            raw = obj.get("id").getAsString();
        } else if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
            raw = obj.get("item").getAsString();
        }
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return new ParsedEntry(raw, stageDefaults.withOverrides(obj));
    }

    record ParsedEntry(String raw, RestrictionFlags flags) {
    }

    /**
     * One stage-list entry from a datapack or KubeJS: item id (namespaced path), {@code #item_tag}, or
     * {@code @mod_id} meaning every item registered under that namespace.
     */
    public static void accumulateEntry(
            String raw,
            RestrictionFlags flags,
            Map<Identifier, RestrictionFlags> itemsOut,
            Map<TagKey<Item>, RestrictionFlags> tagsOut,
            Map<String, RestrictionFlags> namespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                mergeFlags(tagsOut, TagKey.create(BuiltInRegistries.ITEM.key(), tagId), f);
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            putNamespace(namespacesOut, trimmed.substring(1), f);
            return;
        }
        Identifier itemId = Identifier.tryParse(trimmed);
        if (itemId != null) {
            mergeFlags(itemsOut, itemId, f);
        }
    }

    /** Convenience overload: all restrictions enabled. */
    public static void accumulateEntry(
            String raw,
            Map<Identifier, RestrictionFlags> itemsOut,
            Map<TagKey<Item>, RestrictionFlags> tagsOut,
            Map<String, RestrictionFlags> namespacesOut) {
        accumulateEntry(raw, RestrictionFlags.ALL, itemsOut, tagsOut, namespacesOut);
    }

    static void putNamespace(Map<String, RestrictionFlags> namespacesOut, String raw, RestrictionFlags flags) {
        if (raw == null) {
            return;
        }
        String s = raw.trim();
        if (s.startsWith("@")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return;
        }
        try {
            Identifier.fromNamespaceAndPath(s, "x");
            mergeFlags(namespacesOut, s, flags == null ? RestrictionFlags.ALL : flags);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void accumulateFluidEntry(
            String raw,
            RestrictionFlags flags,
            Map<Identifier, RestrictionFlags> fluidsOut,
            Map<TagKey<Fluid>, RestrictionFlags> fluidTagsOut,
            Map<String, RestrictionFlags> fluidNamespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                mergeFlags(fluidTagsOut, TagKey.create(BuiltInRegistries.FLUID.key(), tagId), f);
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            putNamespace(fluidNamespacesOut, trimmed.substring(1), f);
            return;
        }
        Identifier fluidId = Identifier.tryParse(trimmed);
        if (fluidId != null) {
            mergeFlags(fluidsOut, fluidId, f);
        }
    }

    public static void accumulateFluidEntry(
            String raw,
            Map<Identifier, RestrictionFlags> fluidsOut,
            Map<TagKey<Fluid>, RestrictionFlags> fluidTagsOut,
            Map<String, RestrictionFlags> fluidNamespacesOut) {
        accumulateFluidEntry(raw, RestrictionFlags.ALL, fluidsOut, fluidTagsOut, fluidNamespacesOut);
    }

    public static void accumulateChemicalEntry(
            String raw,
            RestrictionFlags flags,
            Map<Identifier, RestrictionFlags> chemicalsOut,
            Map<Identifier, RestrictionFlags> chemicalTagIdsOut,
            Map<String, RestrictionFlags> chemicalNamespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                mergeFlags(chemicalTagIdsOut, tagId, f);
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            putNamespace(chemicalNamespacesOut, trimmed.substring(1), f);
            return;
        }
        Identifier chemicalId = Identifier.tryParse(trimmed);
        if (chemicalId != null) {
            mergeFlags(chemicalsOut, chemicalId, f);
        }
    }

    public static void accumulateChemicalEntry(
            String raw,
            Map<Identifier, RestrictionFlags> chemicalsOut,
            Map<Identifier, RestrictionFlags> chemicalTagIdsOut,
            Map<String, RestrictionFlags> chemicalNamespacesOut) {
        accumulateChemicalEntry(raw, RestrictionFlags.ALL, chemicalsOut, chemicalTagIdsOut, chemicalNamespacesOut);
    }

    public static void accumulateRecipeEntry(
            String raw, RestrictionFlags flags, Map<Identifier, RestrictionFlags> recipesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        Identifier recipeId = Identifier.tryParse(trimmed);
        if (recipeId != null) {
            mergeFlags(recipesOut, recipeId, flags == null ? RestrictionFlags.ALL : flags);
        }
    }

    public static void accumulateRecipeEntry(String raw, Map<Identifier, RestrictionFlags> recipesOut) {
        accumulateRecipeEntry(raw, RestrictionFlags.ALL, recipesOut);
    }

    public static void accumulateDimensionEntry(
            String raw,
            RestrictionFlags flags,
            Map<Identifier, RestrictionFlags> dimensionsOut,
            Map<Identifier, RestrictionFlags> dimensionTagIdsOut,
            Map<String, RestrictionFlags> dimensionNamespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        RestrictionFlags f = flags == null ? RestrictionFlags.ALL : flags;
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                mergeFlags(dimensionTagIdsOut, tagId, f);
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            putNamespace(dimensionNamespacesOut, trimmed.substring(1), f);
            return;
        }
        Identifier dimensionId = Identifier.tryParse(trimmed);
        if (dimensionId != null) {
            mergeFlags(dimensionsOut, dimensionId, f);
        }
    }

    public static void accumulateDimensionEntry(
            String raw,
            Map<Identifier, RestrictionFlags> dimensionsOut,
            Map<Identifier, RestrictionFlags> dimensionTagIdsOut,
            Map<String, RestrictionFlags> dimensionNamespacesOut) {
        accumulateDimensionEntry(raw, RestrictionFlags.ALL, dimensionsOut, dimensionTagIdsOut, dimensionNamespacesOut);
    }

    public boolean matches(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (items.containsKey(key)) {
            return true;
        }
        if (!namespaces.isEmpty() && namespaces.containsKey(key.getNamespace())) {
            return true;
        }
        for (TagKey<Item> tag : tags.keySet()) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Still and flowing variants ({@code minecraft:water} vs {@code minecraft:flowing_water}, NeoForge
     * {@code minecraft:milk} vs {@code minecraft:flowing_milk}) share one logical fluid for stages; match on the
     * source fluid id like filled buckets do.
     */
    public static Identifier fluidKindRegistryKey(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowing) {
            return BuiltInRegistries.FLUID.getKey(flowing.getSource());
        }
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    public boolean matchesFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Identifier kindKey = fluidKindRegistryKey(stack.getFluid());
        if (kindKey != null && fluids.containsKey(kindKey)) {
            return true;
        }
        if (kindKey != null && !fluidNamespaces.isEmpty() && fluidNamespaces.containsKey(kindKey.getNamespace())) {
            return true;
        }
        for (TagKey<Fluid> tag : fluidTags.keySet()) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /** Item ids gated by this stage (flags ignored). */
    public Set<Identifier> itemIds() {
        return items.keySet();
    }

    public Set<Identifier> fluidIds() {
        return fluids.keySet();
    }

    public Set<Identifier> recipeIds() {
        return recipes.keySet();
    }

    public Set<Identifier> dimensionIds() {
        return dimensions.keySet();
    }

    public StageDefinition {
        Objects.requireNonNull(id, "id");
        items = copyFlags(items);
        tags = copyFlags(tags);
        namespaces = copyFlags(namespaces);
        fluids = copyFlags(fluids);
        fluidTags = copyFlags(fluidTags);
        fluidNamespaces = copyFlags(fluidNamespaces);
        chemicals = copyFlags(chemicals);
        chemicalTags = copyFlags(chemicalTags);
        chemicalNamespaces = copyFlags(chemicalNamespaces);
        recipes = copyFlags(recipes);
        dimensions = copyFlags(dimensions);
        dimensionTags = copyFlags(dimensionTags);
        dimensionNamespaces = copyFlags(dimensionNamespaces);
    }

    private static <K> Map<K, RestrictionFlags> copyFlags(Map<K, RestrictionFlags> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<K, RestrictionFlags> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<K, RestrictionFlags> e : source.entrySet()) {
            copy.put(e.getKey(), e.getValue() == null ? RestrictionFlags.ALL : e.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }

    private static <K> void mergeFlags(Map<K, RestrictionFlags> map, K key, RestrictionFlags flags) {
        map.merge(key, flags, RestrictionFlags::or);
    }
}
