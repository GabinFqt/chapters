package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.stage.PlayerStages;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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
        TeamEvent.PLAYER_LOGGED_IN.register(FtbTeamsListeners::onPlayerLoggedIn);
        Chapters.LOGGER.info("Chapters: FTB Teams listeners registered");
    }

    private static void onPropertiesChanged(TeamPropertiesChangedEvent event) {
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
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        ChaptersAPI.syncAll(player);
        InventoryAuditor.auditNow(player);
    }

    /**
     * One-time migration: when a player logs in for the first time after FTB Teams
     * is installed, copy any pre-existing per-player attachment stages into their
     * personal team's {@code TEAM_STAGES}. Skipped for party teams to avoid leaking
     * personal stages into a shared party.
     */
    private static void onPlayerLoggedIn(PlayerLoggedInAfterTeamEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        Team team = event.getTeam();
        if (team == null || team.isClientTeam() || team.isPartyTeam()) {
            ChaptersAPI.syncAll(player);
            return;
        }

        PlayerStages legacy = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        if (legacy.view().isEmpty()) {
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
            }
        }
    }

    private static boolean belongsToTeam(ServerPlayer player, Team team) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(t -> t.getId().equals(team.getId()))
                .orElse(false);
    }
}
