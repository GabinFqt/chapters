package com.gabinx.chapters;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config ({@code config/chapters-common.toml}).
 */
public final class ChaptersConfig {
    public static final ModConfigSpec.BooleanValue SYNC_WITH_FTB_TEAMS;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("ftb");
        SYNC_WITH_FTB_TEAMS = builder
                .comment(
                        "When true (default) and FTB Teams is installed, chapter unlocks are stored on the player's",
                        "team (TEAM_STAGES) and shared with party members. When false, Chapters always uses the",
                        "per-player attachment store even if FTB Teams is present (standalone mode).",
                        "FTB Library / Quests Stage Reward and Stage Task still work either way.")
                .define("syncWithFtbTeams", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ChaptersConfig() {
    }

    public static boolean syncWithFtbTeams() {
        return SYNC_WITH_FTB_TEAMS.get();
    }
}
