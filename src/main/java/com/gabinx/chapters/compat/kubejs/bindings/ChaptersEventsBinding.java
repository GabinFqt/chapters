package com.gabinx.chapters.compat.kubejs.bindings;

import com.gabinx.chapters.compat.kubejs.ChaptersKubeJSBridge;
import com.gabinx.chapters.logic.RestrictionFlags;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public final class ChaptersEventsBinding {
    private static final Gson GSON = new Gson();

    /**
     * Entries match datapack-style item lines ({@code minecraft:apple}, {@code #tag}, {@code @mod_id}).
     * A leading {@code @} also applies to every fluid and every Mekanism chemical registered under that namespace (same as datapack {@code namespaces}).
     * Optional {@code fluid:} prefix for fluids only ({@code fluid:minecraft:lava}, {@code fluid:#c:water}, {@code fluid:@mod_id}).
     * Optional {@code chemical:} prefix for Mekanism chemicals only ({@code chemical:mekanism:hydrogen}, {@code chemical:#mekanism:gases}, {@code chemical:@mekanism}) when Mekanism is installed.
     * Optional {@code recipe:} prefix locks a recipe by id only ({@code recipe:minecraft:diamond_pickaxe}); the output item can stay visible if not otherwise locked.
     * Optional {@code dimension:} prefix locks a dimension by id ({@code dimension:minecraft:the_nether}, {@code dimension:#ns:tag}, {@code dimension:@modid}).
     * <p>
     * Object entries are also accepted: {@code { id: 'minecraft:diamond', jei: false }}.
     * <p>
     * Optional third argument: stage-wide restriction defaults, e.g.
     * {@code defineStage('mypack:tier1', ['minecraft:diamond'], { jei: false })}.
     */
    public void defineStage(String id, Collection<?> locks) {
        defineStage(id, locks, null);
    }

    public void defineStage(String id, Collection<?> locks, Object restrictions) {
        Identifier stageId = Identifier.tryParse(id);
        if (stageId == null || locks == null) {
            return;
        }

        var entries = new ArrayList<>(locks.size());
        for (Object lock : locks) {
            if (lock != null) {
                entries.add(lock);
            }
        }

        RestrictionFlags defaults = RestrictionFlags.ALL;
        if (restrictions instanceof Map<?, ?> map) {
            defaults = RestrictionFlags.fromJson(GSON.toJsonTree(map).getAsJsonObject());
        } else if (restrictions instanceof JsonObject obj) {
            defaults = RestrictionFlags.fromJson(obj);
        } else if (restrictions != null) {
            JsonElement el = GSON.toJsonTree(restrictions);
            if (el.isJsonObject()) {
                defaults = RestrictionFlags.fromJson(el.getAsJsonObject());
            }
        }

        ChaptersKubeJSBridge.defineStage(stageId, entries, defaults);
    }

    public void clearStage(String id) {
        Identifier stageId = Identifier.tryParse(id);
        if (stageId != null) {
            ChaptersKubeJSBridge.clearStage(stageId);
        }
    }
}
