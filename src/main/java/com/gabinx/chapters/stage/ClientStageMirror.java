package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Shared client-side stage + index mirror used by {@link ClientStageCache} and {@link ClientStageIndices}.
 */
final class ClientStageMirror {
    static final ClientStageView VIEW = new ClientStageView();

    private ClientStageMirror() {
    }
}
