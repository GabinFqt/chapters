package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.PlayerStages;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pure command semantics for {@code /chapters add|remove|list|check}.
 */
public final class StageCommandActions {
    private StageCommandActions() {
    }

    public sealed interface Outcome {
        record Success(boolean changed) implements Outcome {
        }

        record UnknownStage(ResourceLocation stageId) implements Outcome {
        }
    }

    public static Outcome requireKnown(Set<ResourceLocation> knownStageIds, ResourceLocation stageId) {
        if (!knownStageIds.contains(stageId)) {
            return new Outcome.UnknownStage(stageId);
        }
        return new Outcome.Success(false);
    }

    public static Outcome add(PlayerStages stages, Set<ResourceLocation> knownStageIds, ResourceLocation stageId) {
        Outcome known = requireKnown(knownStageIds, stageId);
        if (known instanceof Outcome.UnknownStage) {
            return known;
        }
        return new Outcome.Success(stages.add(stageId));
    }

    public static Outcome remove(PlayerStages stages, Set<ResourceLocation> knownStageIds, ResourceLocation stageId) {
        Outcome known = requireKnown(knownStageIds, stageId);
        if (known instanceof Outcome.UnknownStage) {
            return known;
        }
        return new Outcome.Success(stages.remove(stageId));
    }

    public static boolean check(PlayerStages stages, ResourceLocation stageId) {
        return stages.has(stageId);
    }

    public static Set<ResourceLocation> list(PlayerStages stages) {
        return new LinkedHashSet<>(stages.view());
    }
}
