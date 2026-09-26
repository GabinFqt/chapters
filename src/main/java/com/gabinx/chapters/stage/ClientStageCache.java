package com.gabinx.chapters.stage;

import com.gabinx.chapters.logic.ClientStageView;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class ClientStageCache {
    private ClientStageCache() {
    }

    public static synchronized void set(Set<ResourceLocation> stages) {
        ClientStageMirror.VIEW.setStages(stages);
    }

    public static synchronized void add(ResourceLocation stage) {
        ClientStageMirror.VIEW.addStage(stage);
    }

    public static synchronized void remove(ResourceLocation stage) {
        ClientStageMirror.VIEW.removeStage(stage);
    }

    public static synchronized Set<ResourceLocation> snapshot() {
        return ClientStageMirror.VIEW.snapshotStages();
    }
}
