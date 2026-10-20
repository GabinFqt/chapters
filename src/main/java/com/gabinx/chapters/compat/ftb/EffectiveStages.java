package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.ChaptersConfig;
import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.stage.PlayerStages;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Single source of truth for "what stages does this player effectively have?".
 * <p>
 * When FTB Teams is loaded <em>and</em> {@link ChaptersConfig#syncWithFtbTeams()} is true, the
 * effective set is the player's current team's {@code TEAM_STAGES} property — so all members of a
 * {@code PartyTeam} share unlocks. Otherwise the per-player {@code PLAYER_STAGES} attachment is used.
 * <p>
 * Writes follow the same routing. The class never touches FTB classes directly — it delegates to
 * {@link FtbTeamsBridge} which is only loaded when FTB Teams is present.
 */
public final class EffectiveStages {
    private EffectiveStages() {
    }

    public static boolean useTeamStages() {
        return ChaptersConfig.syncWithFtbTeams() && FtbCompat.isTeamsLoaded();
    }

    /**
     * Snapshot of effective stage ids for this player. Mutating the returned
     * set has no effect.
     */
    public static Set<Identifier> of(ServerPlayer player) {
        if (useTeamStages()) {
            Set<Identifier> teamView = FtbTeamsBridge.viewStages(player);
            if (teamView != null) {
                return teamView;
            }
        }
        return new LinkedHashSet<>(player.getData(ChaptersRegistries.PLAYER_STAGES.get()).view());
    }

    /** Same as {@link #of(ServerPlayer)} but wrapped as a {@link PlayerStages} for the lock APIs. */
    public static PlayerStages snapshot(ServerPlayer player) {
        return new PlayerStages(of(player));
    }

    /**
     * @return {@code true} if the stage was actually added (was not present before).
     */
    public static boolean add(ServerPlayer player, Identifier stage) {
        if (useTeamStages() && FtbTeamsBridge.hasTeam(player)) {
            return FtbTeamsBridge.addStage(player, stage);
        }
        PlayerStages stages = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        boolean changed = stages.add(stage);
        if (changed) {
            player.setData(ChaptersRegistries.PLAYER_STAGES.get(), stages);
        }
        return changed;
    }

    /**
     * @return {@code true} if the stage was actually removed (was present before).
     */
    public static boolean remove(ServerPlayer player, Identifier stage) {
        if (useTeamStages() && FtbTeamsBridge.hasTeam(player)) {
            return FtbTeamsBridge.removeStage(player, stage);
        }
        PlayerStages stages = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        boolean changed = stages.remove(stage);
        if (changed) {
            player.setData(ChaptersRegistries.PLAYER_STAGES.get(), stages);
        }
        return changed;
    }

    public static boolean has(ServerPlayer player, Identifier stage) {
        if (useTeamStages()) {
            Boolean teamHas = FtbTeamsBridge.hasStage(player, stage);
            if (teamHas != null) {
                return teamHas;
            }
        }
        return player.getData(ChaptersRegistries.PLAYER_STAGES.get()).has(stage);
    }

    /**
     * When FTB Teams handles broadcasting via {@code TeamEvent.PROPERTIES_CHANGED},
     * the per-player delta packet is redundant and would cause double client refreshes.
     */
    public static boolean shouldEmitPerPlayerDelta(ServerPlayer player) {
        return !(useTeamStages() && FtbTeamsBridge.hasTeam(player));
    }
}
