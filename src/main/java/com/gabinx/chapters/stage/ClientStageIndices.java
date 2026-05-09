package com.gabinx.chapters.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshots of the four stage-locking indices, mirrored on the logical client so that
 * {@link com.gabinx.chapters.compat.RecipeViewerCompat} can compute locked sets without depending on
 * {@link StageManager}'s own indices (which are only populated on the server thread by datapack /
 * KubeJS merge).
 * <p>
 * Without this, dedicated-server clients see empty indices and JEI hides nothing; only integrated
 * servers (singleplayer / open-to-LAN) work because they share the JVM with the server-side
 * {@link StageManager} singleton.
 */
public final class ClientStageIndices {
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> items = Map.of();
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> fluids = Map.of();
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> chemicals = Map.of();
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> recipes = Map.of();

    private ClientStageIndices() {
    }

    public static synchronized void replace(
            Map<ResourceLocation, Set<ResourceLocation>> nextItems,
            Map<ResourceLocation, Set<ResourceLocation>> nextFluids,
            Map<ResourceLocation, Set<ResourceLocation>> nextChemicals,
            Map<ResourceLocation, Set<ResourceLocation>> nextRecipes
    ) {
        items = freeze(nextItems);
        fluids = freeze(nextFluids);
        chemicals = freeze(nextChemicals);
        recipes = freeze(nextRecipes);
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> itemsView() {
        return items;
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> fluidsView() {
        return fluids;
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> chemicalsView() {
        return chemicals;
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> recipesView() {
        return recipes;
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> freeze(
            Map<ResourceLocation, Set<ResourceLocation>> next
    ) {
        if (next == null || next.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Set<ResourceLocation>> copy = new LinkedHashMap<>(next.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : next.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
