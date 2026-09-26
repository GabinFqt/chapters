package com.gabinx.chapters.logic;

/**
 * Whether dimension travel should be cancelled, or a player already inside a locked dimension should be
 * ejected to the Overworld.
 */
public final class DimensionGate {
    private DimensionGate() {
    }

    /**
     * Block travel when the destination is locked and differs from the player's current dimension.
     * Same-dimension teleports stay allowed even if that dimension is locked.
     */
    public static boolean shouldBlockTravel(boolean destinationLocked, boolean sameDimension) {
        return destinationLocked && !sameDimension;
    }

    /**
     * Eject when the current dimension is locked and the Overworld is not (avoids a teleport loop).
     */
    public static boolean shouldEject(boolean currentLocked, boolean overworldLocked) {
        return currentLocked && !overworldLocked;
    }
}
