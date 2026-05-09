package com.gabinx.chapters.stage;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Scope for Mekanism chemical insert/extract checks (set during container slot handling, like fluid transfers).
 */
public final class ChemicalInteractionActor {
    private static final ThreadLocal<Player> ACTOR = new ThreadLocal<>();

    private ChemicalInteractionActor() {
    }

    public static void set(@Nullable Player player) {
        if (player == null) {
            ACTOR.remove();
        } else {
            ACTOR.set(player);
        }
    }

    public static void clear() {
        ACTOR.remove();
    }

    @Nullable
    public static Player get() {
        return ACTOR.get();
    }
}
