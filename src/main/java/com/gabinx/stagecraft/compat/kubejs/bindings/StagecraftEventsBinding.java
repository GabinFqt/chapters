package com.gabinx.stagecraft.compat.kubejs.bindings;

import com.gabinx.stagecraft.compat.kubejs.KubeJSStageBridge;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public final class StagecraftEventsBinding {
    /**
     * Entries match datapack-style item lines ({@code minecraft:apple}, {@code #tag}, {@code @mod_id}).
     * A leading {@code @} also applies to every fluid and every Mekanism chemical registered under that namespace (same as datapack {@code namespaces}).
     * Optional {@code fluid:} prefix for fluids only ({@code fluid:minecraft:lava}, {@code fluid:#c:water}, {@code fluid:@mod_id}).
     * Optional {@code chemical:} prefix for Mekanism chemicals only ({@code chemical:mekanism:hydrogen}, {@code chemical:#mekanism:gases}, {@code chemical:@mekanism}) when Mekanism is installed.
     * <p>
     * Use this varargs form only — a second overload {@code Collection} made Rhino choke on
     * {@code defineStage('id', '@minecraft')} (ambiguous with {@code String[]}).
     */
    public void defineStage(String id, String... itemsOrTags) {
        ResourceLocation stageId = ResourceLocation.tryParse(id);
        if (stageId != null) {
            KubeJSStageBridge.defineStage(stageId, Arrays.asList(itemsOrTags));
        }
    }

    public void clearStage(String id) {
        ResourceLocation stageId = ResourceLocation.tryParse(id);
        if (stageId != null) {
            KubeJSStageBridge.clearStage(stageId);
        }
    }
}
