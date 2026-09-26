package com.gabinx.chapters.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * In-memory model of effective stages with optional team sharing (FTB Teams rules without FTB types).
 * <ul>
 *   <li>No team: personal attachment store</li>
 *   <li>Team: reads/writes go to the team store; all members share</li>
 *   <li>Party join: union of party + previous team + attachment into the party</li>
 *   <li>Party leave: copy party stages onto the player's personal team</li>
 *   <li>Legacy migration: attachment → personal team only (never into a party)</li>
 * </ul>
 */
public final class StageAccounts {
    private final Map<String, Set<ResourceLocation>> personal = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> teams = new HashMap<>();
    /** player id → team id when the player is in a team. */
    private final Map<String, String> membership = new HashMap<>();
    private final Set<String> partyTeams = new HashSet<>();

    public Set<ResourceLocation> of(String playerId) {
        String teamId = membership.get(playerId);
        if (teamId != null) {
            return view(teams.computeIfAbsent(teamId, k -> new LinkedHashSet<>()));
        }
        return view(personal.computeIfAbsent(playerId, k -> new LinkedHashSet<>()));
    }

    public boolean has(String playerId, ResourceLocation stage) {
        return of(playerId).contains(stage);
    }

    public boolean add(String playerId, ResourceLocation stage) {
        return mutableStore(playerId).add(stage);
    }

    public boolean remove(String playerId, ResourceLocation stage) {
        return mutableStore(playerId).remove(stage);
    }

    /**
     * When stages live on a team, FTB broadcasts via property change; skip per-player delta packets.
     */
    public boolean shouldEmitPerPlayerDelta(String playerId) {
        return !membership.containsKey(playerId);
    }

    public void putPersonal(String playerId, Set<ResourceLocation> stages) {
        personal.put(playerId, new LinkedHashSet<>(stages));
    }

    public void ensurePersonalTeam(String playerId, String personalTeamId) {
        teams.computeIfAbsent(personalTeamId, k -> new LinkedHashSet<>());
        membership.put(playerId, personalTeamId);
    }

    public void createParty(String partyId) {
        teams.computeIfAbsent(partyId, k -> new LinkedHashSet<>());
        partyTeams.add(partyId);
    }

    /**
     * Merge previous team + attachment into the party, then move the player onto the party.
     */
    public void joinParty(String playerId, String partyId, String previousTeamId) {
        createParty(partyId);
        Set<ResourceLocation> merged = mergeOnPartyJoin(
                teams.getOrDefault(partyId, Set.of()),
                previousTeamId == null ? Set.of() : teams.getOrDefault(previousTeamId, Set.of()),
                personal.getOrDefault(playerId, Set.of())
        );
        teams.put(partyId, merged);
        membership.put(playerId, partyId);
    }

    /**
     * Copy party stages onto the personal team and move the player there.
     */
    public void leaveParty(String playerId, String partyId, String personalTeamId) {
        Set<ResourceLocation> snapshot = stagesAfterPartyLeave(teams.getOrDefault(partyId, Set.of()));
        teams.put(personalTeamId, new LinkedHashSet<>(snapshot));
        membership.put(playerId, personalTeamId);
    }

    /**
     * @return number of stages newly added to the personal team
     */
    public int migrateLegacy(String playerId, String teamId, boolean isPartyTeam, Set<ResourceLocation> legacy) {
        if (!shouldMigrateLegacy(isPartyTeam, legacy == null || legacy.isEmpty())) {
            return 0;
        }
        membership.put(playerId, teamId);
        Set<ResourceLocation> target = teams.computeIfAbsent(teamId, k -> new LinkedHashSet<>());
        int added = 0;
        for (ResourceLocation id : legacy) {
            if (target.add(id)) {
                added++;
            }
        }
        return added;
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

    private Set<ResourceLocation> mutableStore(String playerId) {
        String teamId = membership.get(playerId);
        if (teamId != null) {
            return teams.computeIfAbsent(teamId, k -> new LinkedHashSet<>());
        }
        return personal.computeIfAbsent(playerId, k -> new LinkedHashSet<>());
    }

    private static Set<ResourceLocation> view(Set<ResourceLocation> source) {
        Objects.requireNonNull(source);
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
