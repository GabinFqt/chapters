package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class ClientStageCache {
    private ClientStageCache() {
    }

    public static synchronized void set(Set<Identifier> stages) {
        ClientStageMirror.VIEW.setStages(stages);
    }

    public static synchronized void add(Identifier stage) {
        ClientStageMirror.VIEW.addStage(stage);
    }

    public static synchronized void remove(Identifier stage) {
        ClientStageMirror.VIEW.removeStage(stage);
    }

    public static synchronized Set<Identifier> snapshot() {
        return ClientStageMirror.VIEW.snapshotStages();
    }
}
