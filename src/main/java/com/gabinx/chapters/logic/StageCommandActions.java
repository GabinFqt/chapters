package com.gabinx.chapters.logic;

import com.gabinx.chapters.stage.PlayerStages;
import net.minecraft.resources.Identifier;

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

        record UnknownStage(Identifier stageId) implements Outcome {
        }
    }

    public static Outcome requireKnown(Set<Identifier> knownStageIds, Identifier stageId) {
        if (!knownStageIds.contains(stageId)) {
            return new Outcome.UnknownStage(stageId);
        }
        return new Outcome.Success(false);
    }

    public static Outcome add(PlayerStages stages, Set<Identifier> knownStageIds, Identifier stageId) {
        Outcome known = requireKnown(knownStageIds, stageId);
        if (known instanceof Outcome.UnknownStage) {
            return known;
        }
        return new Outcome.Success(stages.add(stageId));
    }

    public static Outcome remove(PlayerStages stages, Set<Identifier> knownStageIds, Identifier stageId) {
        Outcome known = requireKnown(knownStageIds, stageId);
        if (known instanceof Outcome.UnknownStage) {
            return known;
        }
        return new Outcome.Success(stages.remove(stageId));
    }

    public static boolean check(PlayerStages stages, Identifier stageId) {
        return stages.has(stageId);
    }

    public static Set<Identifier> list(PlayerStages stages) {
        return new LinkedHashSet<>(stages.view());
    }
}
