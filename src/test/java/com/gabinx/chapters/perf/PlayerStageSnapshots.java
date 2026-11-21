package com.gabinx.chapters.perf;

import com.gabinx.chapters.logic.ParsedStageCache;
import com.gabinx.chapters.stage.PlayerStages;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * What {@code EffectiveStages.view(player)} does per lock check, without needing a {@code ServerPlayer}.
 */
final class PlayerStageSnapshots {
    private static final ParsedStageCache<UUID> TEAM_CACHE = new ParsedStageCache<>();
    private static final UUID TEAM = new UUID(0, 1);

    private PlayerStageSnapshots() {
    }

    /** Player attachment path: read-only view of the owned set. */
    static Set<ResourceLocation> solo(PlayerStages stages) {
        return stages.view();
    }

    /** FTB Teams path ({@code FtbTeamsBridge.cachedStages}): parsed set reused while TEAM_STAGES is unchanged. */
    static Set<ResourceLocation> team(Set<String> teamStages) {
        return TEAM_CACHE.get(TEAM, teamStages);
    }
}
