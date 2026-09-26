package com.gabinx.chapters.logic;

/**
 * Whether a crafting result slot should be cleared / a craft should be undone.
 */
public final class CraftGate {
    private CraftGate() {
    }

    public static boolean shouldBlock(boolean outputItemLocked, boolean recipeLocked) {
        return outputItemLocked || recipeLocked;
    }
}
