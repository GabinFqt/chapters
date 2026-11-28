package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Team rules used by the FTB Teams listeners, kept free of FTB types so they can be unit tested.
 * <ul>
 *   <li>Party join: union of party + previous team + player attachment into the party</li>
 *   <li>Party leave: copy party stages onto the player's personal team</li>
 *   <li>Legacy migration: attachment → personal team only (never into a party)</li>
 * </ul>
 */
public final class StageAccounts {
    private StageAccounts() {
    }

    public static Set<ResourceLocation> mergeOnPartyJoin(
            Set<ResourceLocation> partyStages,
            Set<ResourceLocation> previousTeamStages,
            Set<ResourceLocation> attachmentStages
    ) {
        Set<ResourceLocation> merged = new LinkedHashSet<>();
        if (partyStages != null) {
            merged.addAll(partyStages);
        }
        if (previousTeamStages != null) {
            merged.addAll(previousTeamStages);
        }
        if (attachmentStages != null) {
            merged.addAll(attachmentStages);
        }
        return merged;
    }

    public static Set<ResourceLocation> stagesAfterPartyLeave(Set<ResourceLocation> partyStages) {
        if (partyStages == null || partyStages.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(partyStages);
    }

    public static boolean shouldMigrateLegacy(boolean isPartyTeam, boolean legacyEmpty) {
        return !isPartyTeam && !legacyEmpty;
    }
}
