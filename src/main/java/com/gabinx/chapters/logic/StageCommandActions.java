package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Which stage ids {@code /chapters add|remove} accept.
 */
public final class StageCommandActions {
    private StageCommandActions() {
    }

    /** {@code add} only grants stages a datapack or KubeJS defines, so typos fail loudly. */
    public static boolean canAdd(Set<ResourceLocation> knownStageIds, ResourceLocation stageId) {
        return knownStageIds.contains(stageId);
    }

    /**
     * {@code remove} also accepts a stage the player still has after it left the datapack or scripts, so it can be
     * cleaned up.
     */
    public static boolean canRemove(
            Set<ResourceLocation> knownStageIds, Set<ResourceLocation> ownedStageIds, ResourceLocation stageId) {
        return knownStageIds.contains(stageId) || ownedStageIds.contains(stageId);
    }
}
