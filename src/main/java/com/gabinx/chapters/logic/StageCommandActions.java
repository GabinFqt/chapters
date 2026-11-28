package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Which stage ids {@code /chapters add|remove} accept.
 */
public final class StageCommandActions {
    private StageCommandActions() {
    }

    /** {@code add} only grants stages a datapack or KubeJS defines, so typos fail loudly. */
    public static boolean canAdd(Set<Identifier> knownStageIds, Identifier stageId) {
        return knownStageIds.contains(stageId);
    }

    /**
     * {@code remove} also accepts a stage the player still has after it left the datapack or scripts, so it can be
     * cleaned up.
     */
    public static boolean canRemove(
            Set<Identifier> knownStageIds, Set<Identifier> ownedStageIds, Identifier stageId) {
        return knownStageIds.contains(stageId) || ownedStageIds.contains(stageId);
    }
}
