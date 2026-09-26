package com.gabinx.chapters.compat.mekanism;

import com.gabinx.chapters.stage.StageDefinition;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the chemical → defining stages map when Mekanism is loaded.
 */
@SuppressWarnings("removal")
public final class MekanismChemicalIndex {
    private MekanismChemicalIndex() {
    }

    public static Set<Identifier> chemicalsInTag(Identifier tagId) {
        Set<Identifier> out = new LinkedHashSet<>();
        var registry = MekanismAPI.CHEMICAL_REGISTRY;
        TagKey<Chemical> tag = TagKey.create(registry.key(), tagId);
        registry.getTag(tag).ifPresent(holders -> {
            for (Holder<Chemical> holder : holders) {
                Identifier key = registry.getKey(holder.value());
                if (key != null && !MekanismAPI.EMPTY_CHEMICAL_NAME.equals(key)) {
                    out.add(key);
                }
            }
        });
        return out;
    }

    public static Set<Identifier> chemicalsInNamespace(String namespace) {
        Set<Identifier> out = new LinkedHashSet<>();
        var registry = MekanismAPI.CHEMICAL_REGISTRY;
        for (Chemical chemical : registry) {
            if (chemical == null) {
                continue;
            }
            Identifier key = registry.getKey(chemical);
            if (key != null
                    && !MekanismAPI.EMPTY_CHEMICAL_NAME.equals(key)
                    && namespace.equals(key.getNamespace())) {
                out.add(key);
            }
        }
        return out;
    }

    public static Map<Identifier, Set<Identifier>> buildIndex(List<StageDefinition> mergedDefinitions) {
        Map<Identifier, Set<Identifier>> chemicalMap = new HashMap<>();
        for (StageDefinition def : mergedDefinitions) {
            for (Identifier chemicalId : def.chemicals()) {
                chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
            }
            for (Identifier tagId : def.chemicalTags()) {
                for (Identifier key : chemicalsInTag(tagId)) {
                    chemicalMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
            for (String ns : def.chemicalNamespaces()) {
                for (Identifier key : chemicalsInNamespace(ns)) {
                    chemicalMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                }
            }
        }
        return chemicalMap;
    }
}
