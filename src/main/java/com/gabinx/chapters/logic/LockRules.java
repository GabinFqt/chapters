package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Shared lock predicate: a subject is locked when its defining stages are non-empty and the
 * player owns none of them. Owning any one defining stage unlocks.
 */
public final class LockRules {
    private LockRules() {
    }

    public static boolean isLocked(Set<Identifier> definingStages, Set<Identifier> ownedStages) {
        if (definingStages == null || definingStages.isEmpty()) {
            return false;
        }
        if (ownedStages == null || ownedStages.isEmpty()) {
            return true;
        }
        for (Identifier stageId : definingStages) {
            if (ownedStages.contains(stageId)) {
                return false;
            }
        }
        return true;
    }
}
