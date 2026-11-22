package com.gabinx.chapters.e2e.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.gabinx.chapters.ChaptersConfig;
import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.compat.ftb.FtbCompat;
import com.gabinx.chapters.stage.PlayerStages;
import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eStages;
import com.gabinx.chapters.e2e.E2eSupport;
import com.gabinx.chapters.network.ClientboundStageDeltaPayload;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import com.gabinx.chapters.stage.StageManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamPlayerLoggedInEvent;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Set;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * FTB Teams party path. These tests run in their own batch so they do not race the standalone flag.
 * Leaving a party keeps a copy of the stages, then the two sides diverge.
 */
public class TeamGameTests {
    public static void party_sharesUnlockAndAudit(GameTestHelper helper) {
        Party party = party(helper);
        E2eSupport.give(party.b.player(), Items.PRISMARINE_SHARD, 4);
        E2eSupport.settle(party.b.player());
        helper.assertTrue(E2eSupport.count(party.b.player(), Items.PRISMARINE_SHARD) == 0, "B starts locked");
        E2eSupport.grant(party.a.player(), E2eStages.FULL_PRISMARINE);
        helper.assertTrue(ChaptersAPI.hasStage(party.b.player(), E2eStages.FULL_PRISMARINE), "B should share A's stage");
        E2eSupport.give(party.a.player(), Items.PRISMARINE_SHARD, 4);
        E2eSupport.give(party.b.player(), Items.PRISMARINE_SHARD, 4);
        E2eSupport.settle(party.a.player());
        E2eSupport.settle(party.b.player());
        helper.assertTrue(E2eSupport.count(party.b.player(), Items.PRISMARINE_SHARD) >= 4, "B should keep the unlocked item");
        E2eSupport.revoke(party.a.player(), E2eStages.FULL_PRISMARINE);
        helper.assertTrue(E2eSupport.count(party.a.player(), Items.PRISMARINE_SHARD) == 0, "removing the stage should audit A");
        helper.assertTrue(E2eSupport.count(party.b.player(), Items.PRISMARINE_SHARD) == 0, "removing the stage should audit B");
        helper.succeed();
    }

    public static void party_ejectsBothFromEnd(GameTestHelper helper) {
        Party party = party(helper);
        ServerLevel end = party.a.player().level().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the End level should exist");
        E2eSupport.grant(party.a.player(), E2eStages.END_TRAVEL);
        party.a.player().teleportTo(end, 0.5, 80, 0.5, Set.<Relative>of(), 0, 0, false);
        party.b.player().teleportTo(end, 2.5, 80, 0.5, Set.<Relative>of(), 0, 0, false);
        helper.assertTrue(party.a.player().level().dimension() == Level.END, "A should reach the End");
        helper.assertTrue(party.b.player().level().dimension() == Level.END, "B should reach the End");
        E2eSupport.revoke(party.a.player(), E2eStages.END_TRAVEL);
        helper.assertTrue(party.a.player().level().dimension() == Level.OVERWORLD, "A should be ejected");
        helper.assertTrue(party.b.player().level().dimension() == Level.OVERWORLD, "B should be ejected");
        helper.succeed();
    }

    public static void party_leaveKeepsCopyThenDiverges(GameTestHelper helper) {
        Party party = party(helper);
        E2eSupport.grant(party.a.player(), E2eStages.FULL_PRISMARINE);
        leave(party.team, party.b.player().getUUID());
        helper.assertTrue(
                ChaptersAPI.hasStage(party.b.player(), E2eStages.FULL_PRISMARINE),
                "leaving should copy the party stages onto the personal team");
        E2eSupport.grant(party.a.player(), E2eStages.TEASE_QUARTZ);
        helper.assertFalse(
                ChaptersAPI.hasStage(party.b.player(), E2eStages.TEASE_QUARTZ),
                "B should stop following the party");
        E2eSupport.revoke(party.b.player(), E2eStages.FULL_PRISMARINE);
        helper.assertTrue(
                ChaptersAPI.hasStage(party.a.player(), E2eStages.FULL_PRISMARINE),
                "B's personal remove should not take the stage from A");
        helper.succeed();
    }

    public static void party_syncsFullPayloadNotDelta(GameTestHelper helper) {
        Party party = party(helper);
        party.a.drainPayloads();
        party.b.drainPayloads();
        E2eSupport.grant(party.a.player(), E2eStages.FULL_PRISMARINE);
        boolean full = false;
        boolean delta = false;
        for (CustomPacketPayload payload : party.b.drainPayloads()) {
            if (payload instanceof ClientboundStagesPayload stages && stages.stages().contains(E2eStages.FULL_PRISMARINE)) {
                full = true;
            }
            if (payload instanceof ClientboundStageDeltaPayload added && added.added() && E2eStages.FULL_PRISMARINE.equals(added.stage())) {
                delta = true;
            }
        }
        helper.assertTrue(full, "B should receive a full stage sync");
        helper.assertFalse(delta, "a party change should not also send a per-player delta");
        helper.succeed();
    }

    /**
     * A stage left in the player attachment must not come back onto the personal team after login.
     */
    public static void legacy_login_remove(GameTestHelper helper) {
        ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(true);
        helper.assertTrue(StageManager.get().get(E2eStages.SOFT_AMETHYST).isPresent(), "e2e stages were not loaded");
        helper.assertTrue(FtbCompat.isTeamsLoaded(), "FTB Teams should be loaded in this run");
        helper.assertTrue(FTBTeamsAPI.api().isManagerLoaded(), "the FTB team manager should be loaded");
        ServerPlayer player = E2ePlayers.join(helper).player();
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElseThrow();
        helper.assertFalse(team.isPartyTeam(), "the joined player should be on a personal team");

        ChaptersAPI.addStage(player, E2eStages.FULL_PRISMARINE);
        ChaptersAPI.addStage(player, E2eStages.PLANKS);
        player.setData(
                ChaptersRegistries.PLAYER_STAGES.get(),
                new PlayerStages(List.of(E2eStages.FULL_PRISMARINE, E2eStages.PLANKS)));

        ChaptersAPI.removeStage(player, E2eStages.FULL_PRISMARINE);
        helper.assertFalse(
                ChaptersAPI.hasStage(player, E2eStages.FULL_PRISMARINE),
                "remove should drop the stage before the next login");
        login(team, player);
        helper.assertFalse(
                ChaptersAPI.hasStage(player, E2eStages.FULL_PRISMARINE),
                "a removed stage should stay gone after login copies the player file");
        helper.assertTrue(
                ChaptersAPI.hasStage(player, E2eStages.PLANKS),
                "a stage that was kept should still be on the team");
        helper.assertTrue(
                player.getData(ChaptersRegistries.PLAYER_STAGES.get()).view().isEmpty(),
                "login should clear the player file after copying it");

        ChaptersAPI.removeStage(player, E2eStages.PLANKS);
        login(team, player);
        helper.assertFalse(
                ChaptersAPI.hasStage(player, E2eStages.PLANKS),
                "removing the last stage should survive the next login");
        helper.succeed();
    }

    private static void login(Team team, ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new FTBTeamsEvent.TeamPlayerLoggedIn(new TeamPlayerLoggedInEvent.Data(team, player)));
    }

    private record Party(E2ePlayers.Joined a, E2ePlayers.Joined b, Team team) {
    }

    private static Party party(GameTestHelper helper) {
        ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(true);
        helper.assertTrue(StageManager.get().get(E2eStages.SOFT_AMETHYST).isPresent(), "e2e stages were not loaded");
        helper.assertTrue(FtbCompat.isTeamsLoaded(), "FTB Teams should be loaded in this run");
        helper.assertTrue(FTBTeamsAPI.api().isManagerLoaded(), "the FTB team manager should be loaded");
        E2ePlayers.Joined a = E2ePlayers.join(helper);
        E2ePlayers.Joined b = E2ePlayers.join(helper);
        helper.assertTrue(
                FTBTeamsAPI.api().getManager().getTeamForPlayer(a.player()).isPresent(),
                "A should have a personal team after login");
        try {
            Team team = FTBTeamsAPI.api().getManager().createPartyTeam(a.player(), "e2e", null, null);
            Method join = team.getClass().getMethod("join", ServerPlayer.class);
            join.invoke(team, b.player());
            helper.assertTrue(team.getMembers().contains(b.player().getUUID()), "B should be in the party");
            return new Party(a, b, team);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("Could not build the e2e party", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not build the e2e party", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not build the e2e party", e);
        }
    }

    private static void leave(Team team, UUID playerId) {
        try {
            team.getClass().getMethod("leave", UUID.class).invoke(team, playerId);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not leave the party", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not leave the party", e);
        }
    }
}
