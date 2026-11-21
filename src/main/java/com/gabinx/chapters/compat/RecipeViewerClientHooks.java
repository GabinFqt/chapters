package com.gabinx.chapters.compat;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only registration so dedicated servers never classload {@link ClientTickEvent}.
 */
public final class RecipeViewerClientHooks {
    private RecipeViewerClientHooks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RecipeViewerClientHooks::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        RecipeViewerCompat.flushScheduled();
    }
}
