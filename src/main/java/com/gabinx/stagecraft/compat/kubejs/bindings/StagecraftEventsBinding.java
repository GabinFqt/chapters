package com.gabinx.stagecraft.compat.kubejs.bindings;

import com.gabinx.stagecraft.compat.kubejs.KubeJSStageBridge;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;

public final class StagecraftEventsBinding {
    /**
     * Entries match datapack-style item lines ({@code minecraft:apple}, {@code #tag}, {@code @mod_id}).
     * A leading {@code @} also applies to every fluid and every Mekanism chemical registered under that namespace (same as datapack {@code namespaces}).
     * Optional {@code fluid:} prefix for fluids only ({@code fluid:minecraft:lava}, {@code fluid:#c:water}, {@code fluid:@mod_id}).
     * Optional {@code chemical:} prefix for Mekanism chemicals only ({@code chemical:mekanism:hydrogen}, {@code chemical:#mekanism:gases}, {@code chemical:@mekanism}) when Mekanism is installed.
     * <p>
     * Breaking change: use a single collection-form call from KubeJS:
     * {@code defineStage('namespace:stage_id', ['minecraft:apple', '#minecraft:axes'])}.
     */
    public void defineStage(String id, Collection<?> locks) {
        ResourceLocation stageId = ResourceLocation.tryParse(id);
        if (stageId == null || locks == null) {
            return;
        }

        var entries = new ArrayList<String>(locks.size());
        for (Object lock : locks) {
            if (lock != null) {
                entries.add(lock.toString());
            }
        }

        KubeJSStageBridge.defineStage(stageId, entries);
    }

    public void clearStage(String id) {
        ResourceLocation stageId = ResourceLocation.tryParse(id);
        if (stageId != null) {
            KubeJSStageBridge.clearStage(stageId);
        }
    }
}
