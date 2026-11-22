package com.gabinx.chapters.e2e.gametest;

import com.gabinx.chapters.ChaptersConfig;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eStages;
import com.gabinx.chapters.e2e.E2eSupport;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Section 8 plus the server half of respawn, End exit, and {@code /reload}.
 * The respawn check is the 1.4 regression: the stage payload sent for the new player must not be empty.
 */
public class TravelGameTests {
    private static final Identifier PRISMARINE = BuiltInRegistries.ITEM.getKey(Items.PRISMARINE_SHARD);

    public static void endTravel_blockedUntilStage(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        ServerLevel end = player.level().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the End level should exist");
        player.teleportTo(end, 0.5, 80, 0.5, Set.of(), 0, 0, false);
        helper.assertTrue(player.level().dimension() == Level.OVERWORLD, "travel into the End should be cancelled");
        E2eSupport.grant(player, E2eStages.END_TRAVEL);
        player.teleportTo(end, 0.5, 80, 0.5, Set.of(), 0, 0, false);
        helper.assertTrue(player.level().dimension() == Level.END, "unlocked travel should reach the End");
        E2eSupport.revoke(player, E2eStages.END_TRAVEL);
        helper.assertTrue(player.level().dimension() == Level.OVERWORLD, "losing the stage should eject back to the Overworld");
        helper.succeed();
    }

    public static void respawn_keepsStagesAndSyncsThem(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        E2ePlayers.Joined joined = E2ePlayers.join(helper);
        helper.assertTrue(joined.drainedIndicesContainItem(PRISMARINE), "login should send the item index");
        E2eSupport.grant(joined.player(), E2eStages.FULL_PRISMARINE);
        helper.assertTrue(joined.drainedStagesContain(E2eStages.FULL_PRISMARINE), "adding a stage should sync it");
        ServerPlayer respawned = joined.player().level().getServer().getPlayerList().respawn(joined.player(), false, Entity.RemovalReason.KILLED);
        helper.assertTrue(ChaptersAPI.hasStage(respawned, E2eStages.FULL_PRISMARINE), "the new player should still own the stage");
        boolean sawStages = false;
        for (CustomPacketPayload payload : joined.drainPayloads()) {
            if (payload instanceof ClientboundStagesPayload stages) {
                sawStages = true;
                helper.assertTrue(
                        stages.stages().contains(E2eStages.FULL_PRISMARINE),
                        "every stage sync after respawn must include the owned stage, not an empty set");
            }
        }
        helper.assertTrue(sawStages, "respawn should send a stage sync");
        helper.succeed();
    }

    /**
     * Quit and reopen, not {@code /reload}. Disconnect saves through {@code PlayerDataStorage}
     * and the next join reads that file back.
     */
    public static void rejoin_keepsAddedAndRemovedStages(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(false);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.grant(player, E2eStages.FULL_PRISMARINE);
        E2eSupport.grant(player, E2eStages.PLANKS);

        MinecraftServer server = player.level().getServer();
        NameAndId id = new NameAndId(player.getGameProfile());
        server.getPlayerList().getPlayerIo().save(player);
        Set<Identifier> added = stagesInPlayerFile(server, id);
        helper.assertTrue(added.contains(E2eStages.FULL_PRISMARINE),
                "an added stage should still be in the player file");
        helper.assertTrue(added.contains(E2eStages.PLANKS),
                "a second stage should still be in the player file");

        ChaptersAPI.removeStage(player, E2eStages.FULL_PRISMARINE);
        server.getPlayerList().getPlayerIo().save(player);
        Set<Identifier> removed = stagesInPlayerFile(server, id);
        helper.assertFalse(removed.contains(E2eStages.FULL_PRISMARINE),
                "a removed stage should stay out of the player file");
        helper.assertTrue(removed.contains(E2eStages.PLANKS),
                "the stage that was kept should still be in the player file");
        helper.succeed();
    }

    /** The attachment compound the next join deserializes. No second player: a connectionless one gets ticked and crashes. */
    private static Set<Identifier> stagesInPlayerFile(MinecraftServer server, NameAndId id) {
        CompoundTag tag = server.getPlayerList().loadPlayerData(id).orElseThrow(
                () -> new IllegalStateException("player file was not written for " + id));
        ListTag list = tag.getCompoundOrEmpty("neoforge:attachments")
                .getCompoundOrEmpty("chapters:player_stages")
                .getListOrEmpty("stages");
        Set<Identifier> ids = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Identifier parsed = Identifier.tryParse(list.getString(i).orElse(""));
            if (parsed != null) {
                ids.add(parsed);
            }
        }
        return ids;
    }

    public static void reload_keepsStagesAndLocks(GameTestHelper helper) {
        E2eSupport.prepare(helper);
        ServerPlayer player = E2ePlayers.join(helper).player();
        E2eSupport.grant(player, E2eStages.FULL_PRISMARINE);
        var server = player.level().getServer();
        AtomicBoolean finished = new AtomicBoolean();
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((ignored, error) -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
            if (error != null) {
                helper.fail("reload failed: " + error.getMessage());
                return;
            }
            helper.assertTrue(
                    StageManagerPresent.prismarine(helper),
                    "prismarine stage should still be loaded after reload");
            helper.assertTrue(ChaptersAPI.hasStage(player, E2eStages.FULL_PRISMARINE), "the player should keep the stage");
            E2eSupport.give(player, Items.QUARTZ, 4);
            E2eSupport.settle(player);
            helper.assertTrue(E2eSupport.count(player, Items.QUARTZ) == 0, "locks should still drop quartz after reload");
            E2eSupport.give(player, Items.PRISMARINE_SHARD, 4);
            E2eSupport.settle(player);
            helper.assertTrue(
                    E2eSupport.count(player, Items.PRISMARINE_SHARD) >= 4,
                    "owned prismarine should stay after reload");
            helper.succeed();
            } catch (Throwable thrown) {
                helper.fail(thrown.toString());
            }
        }, server);
    }

    /** Indirection so the reload callback can mention StageManager without a noisy import in the lambda above. */
    private static final class StageManagerPresent {
        private StageManagerPresent() {
        }

        static boolean prismarine(GameTestHelper helper) {
            return com.gabinx.chapters.stage.StageManager.get().get(E2eStages.FULL_PRISMARINE).isPresent();
        }
    }
}
