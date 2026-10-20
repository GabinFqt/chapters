package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.DimensionHandler;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.logic.StageAccounts;
import com.gabinx.chapters.stage.PlayerStages;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wires Chapters reactions to FTB Teams events. Only loaded when both
 * {@code ftblibrary} and {@code ftbteams} are present.
 */
final class FtbTeamsListeners {
    private FtbTeamsListeners() {
    }

    static void register() {
        TeamEvent.PROPERTIES_CHANGED.register(FtbTeamsListeners::onPropertiesChanged);
        TeamEvent.PLAYER_CHANGED.register(FtbTeamsListeners::onPlayerChangedTeam);
        TeamEvent.PLAYER_JOINED_PARTY.register(FtbTeamsListeners::onJoinedParty);
        TeamEvent.PLAYER_LEFT_PARTY.register(FtbTeamsListeners::onLeftParty);
        TeamEvent.PLAYER_LOGGED_IN.register(FtbTeamsListeners::onPlayerLoggedIn);
        Chapters.LOGGER.info("Chapters: FTB Teams listeners registered");
    }

    private static void onPropertiesChanged(TeamPropertiesChangedEvent event) {
        if (!EffectiveStages.useTeamStages()) {
            return;
        }
        Team team = event.getTeam();
        if (team == null || team.isClientTeam()) {
            return;
        }
        Set<String> previousStages = event.getPreviousProperties().get(TeamProperties.TEAM_STAGES);
        Set<String> currentStages = team.getProperty(TeamProperties.TEAM_STAGES);
        if (previousStages != null && previousStages.equals(currentStages)) {
            return;
        }
        propagateToTeamMembers(team);
    }

    private static void onPlayerChangedTeam(PlayerChangedTeamEvent event) {
        if (!EffectiveStages.useTeamStages()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        ChaptersAPI.syncAll(player);
        InventoryAuditor.auditNow(player);
        DimensionHandler.auditNow(player);
    }

    /**
     * Party join: union previous personal stages + current party stages into the party store,
     * so every online member sees the merged set.
     */
    private static void onJoinedParty(PlayerJoinedPartyTeamEvent event) {
        if (!EffectiveStages.useTeamStages()) {
            return;
        }
        Team party = event.getTeam();
        Team previous = event.getPreviousTeam();
        ServerPlayer player = event.getPlayer();
        if (party == null || party.isClientTeam()) {
            return;
        }

        Set<ResourceLocation> partyIds = toResourceLocations(FtbTeamsBridge.readStageStrings(party));
        Set<ResourceLocation> previousIds = previous != null
                ? toResourceLocations(FtbTeamsBridge.readStageStrings(previous))
                : Set.of();
        Set<ResourceLocation> attachmentIds = player != null
                ? player.getData(ChaptersRegistries.PLAYER_STAGES.get()).view()
                : Set.of();
        Set<ResourceLocation> mergedIds = StageAccounts.mergeOnPartyJoin(partyIds, previousIds, attachmentIds);
        Set<String> merged = toStrings(mergedIds);

        FtbTeamsBridge.writeStageStrings(party, merged);
        Chapters.LOGGER.info(
                "Chapters: merged {} stage(s) into party {} after {} joined",
                merged.size(),
                party.getShortName(),
                player != null ? player.getGameProfile().getName() : "?"
        );
    }

    /**
     * Party leave: copy the party's stages onto the player's new personal team so they keep
     * everything they had while in the party, but are no longer synced with the party afterward.
     */
    private static void onLeftParty(PlayerLeftPartyTeamEvent event) {
        if (!EffectiveStages.useTeamStages()) {
            return;
        }
        Team party = event.getTeam();
        Team personal = event.getPlayerTeam();
        ServerPlayer player = event.getPlayer();

        Set<ResourceLocation> snapshot = StageAccounts.stagesAfterPartyLeave(
                party != null ? toResourceLocations(FtbTeamsBridge.readStageStrings(party)) : Set.of()
        );
        if (personal != null && !personal.isClientTeam()) {
            FtbTeamsBridge.writeStageStrings(personal, toStrings(snapshot));
            Chapters.LOGGER.info(
                    "Chapters: copied {} party stage(s) onto personal team {} after leave",
                    snapshot.size(),
                    personal.getShortName()
            );
        }

        if (player != null) {
            ChaptersAPI.syncAll(player);
            InventoryAuditor.auditNow(player);
            DimensionHandler.auditNow(player);
        }
    }

    /**
     * One-time migration: when a player logs in for the first time after FTB Teams
     * is installed, copy any pre-existing per-player attachment stages into their
     * personal team's {@code TEAM_STAGES}. Skipped for party teams to avoid leaking
     * personal stages into a shared party.
     */
    private static void onPlayerLoggedIn(PlayerLoggedInAfterTeamEvent event) {
        if (!EffectiveStages.useTeamStages()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        Team team = event.getTeam();
        PlayerStages legacy = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        boolean isParty = team != null && !team.isClientTeam() && team.isPartyTeam();
        if (team == null || team.isClientTeam()
                || !StageAccounts.shouldMigrateLegacy(isParty, legacy.view().isEmpty())) {
            ChaptersAPI.syncAll(player);
            return;
        }

        int added = 0;
        for (ResourceLocation rl : legacy.view()) {
            if (FtbTeamsBridge.addStage(player, rl)) {
                added++;
            }
        }
        if (added > 0) {
            Chapters.LOGGER.info(
                    "Chapters: migrated {} legacy stage(s) from player {} attachment into personal team {}",
                    added,
                    player.getGameProfile().getName(),
                    team.getShortName()
            );
        }
        ChaptersAPI.syncAll(player);
    }

    private static void propagateToTeamMembers(Team team) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null && belongsToTeam(member, team)) {
                ChaptersAPI.syncAll(member);
                InventoryAuditor.auditNow(member);
                DimensionHandler.auditNow(member);
            }
        }
    }

    private static boolean belongsToTeam(ServerPlayer player, Team team) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(t -> t.getId().equals(team.getId()))
                .orElse(false);
    }

    private static Set<ResourceLocation> toResourceLocations(Iterable<String> raw) {
        Set<ResourceLocation> out = new HashSet<>();
        for (String s : raw) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) {
                out.add(rl);
            }
        }
        return out;
    }

    private static Set<String> toStrings(Set<ResourceLocation> ids) {
        Set<String> out = new HashSet<>();
        for (ResourceLocation id : ids) {
            out.add(id.toString());
        }
        return out;
    }
}
