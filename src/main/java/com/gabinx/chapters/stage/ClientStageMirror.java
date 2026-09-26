package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;

/**
 * Shared client-side stage + index mirror used by {@link ClientStageCache} and {@link ClientStageIndices}.
 */
final class ClientStageMirror {
    static final ClientStageView VIEW = new ClientStageView();

    private ClientStageMirror() {
    }
}
