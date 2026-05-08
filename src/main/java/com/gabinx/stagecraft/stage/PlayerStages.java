package com.gabinx.stagecraft.stage;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PlayerStages {
    public static final Codec<PlayerStages> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(PlayerStages::new, stages -> List.copyOf(stages.stages));

    private final Set<ResourceLocation> stages;

    public PlayerStages(Collection<ResourceLocation> stages) {
        this.stages = new LinkedHashSet<>(stages);
    }

    public static PlayerStages empty() {
        return new PlayerStages(List.of());
    }

    public Set<ResourceLocation> view() {
        return Collections.unmodifiableSet(stages);
    }

    public boolean add(ResourceLocation stage) {
        return stages.add(stage);
    }

    public boolean remove(ResourceLocation stage) {
        return stages.remove(stage);
    }

    public boolean has(ResourceLocation stage) {
        return stages.contains(stage);
    }
}
