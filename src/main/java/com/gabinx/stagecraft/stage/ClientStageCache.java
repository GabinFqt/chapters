package com.gabinx.stagecraft.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientStageCache {
    private static final Set<ResourceLocation> STAGES = new LinkedHashSet<>();

    private ClientStageCache() {
    }

    public static synchronized void set(Set<ResourceLocation> stages) {
        STAGES.clear();
        STAGES.addAll(stages);
    }

    public static synchronized void add(ResourceLocation stage) {
        STAGES.add(stage);
    }

    public static synchronized void remove(ResourceLocation stage) {
        STAGES.remove(stage);
    }

    public static synchronized Set<ResourceLocation> snapshot() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(STAGES));
    }
}
