package com.gabinx.chapters.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshot of recipe id → defining stages for recipe viewer lock math on the logical client.
 * Without this, indices built only on the integrated server thread (KubeJS/datapack merge) never exist on dedicated clients.
 */
public final class ClientRecipeStagesIndex {
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> index = Map.of();

    private ClientRecipeStagesIndex() {
    }

    public static synchronized void replace(Map<ResourceLocation, Set<ResourceLocation>> next) {
        if (next == null || next.isEmpty()) {
            index = Map.of();
            return;
        }
        Map<ResourceLocation, Set<ResourceLocation>> copy = new LinkedHashMap<>(next.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : next.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        index = Collections.unmodifiableMap(copy);
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> snapshot() {
        return index;
    }
}
