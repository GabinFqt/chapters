package com.gabinx.chapters.e2e.gametest;

import com.gabinx.chapters.ChaptersConfig;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.e2e.E2ePlayers;
import com.gabinx.chapters.e2e.E2eSupport;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.event.PickupHandler;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.stage.StageManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server timings for the perf pack. Not part of the required {@code chapters} namespace.
 * Uses only APIs that exist before and after the lock-check speedup.
 */
public class PerfGameTests {
    private static final Identifier BULK = Identifier.fromNamespaceAndPath("chapters", "e2e_perf_bulk");
    private static final Identifier PROBE = Identifier.fromNamespaceAndPath("chapters_e2e", "perf_0");

    public static void measure(GameTestHelper helper) {
        ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(false);
        helper.assertTrue(StageManager.get().get(BULK).isPresent(), "perf pack stage was not loaded");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(PROBE), "perf items were not registered");
        E2ePlayers.Joined solo = E2ePlayers.join(helper);
        StringBuilder report = new StringBuilder();
        report.append("login_ms\t").append(fmt(ms(time(() -> E2ePlayers.join(helper))))).append("\n");

        ClientboundStageIndicesPayload payload = indices();
        byte[] encoded = encode(payload);
        report.append("index_bytes\t").append(encoded.length).append("\n");
        report.append("index_encode_ms\t").append(fmt(median(5, () -> encode(payload)))).append("\n");

        ItemStack locked = new ItemStack(BuiltInRegistries.ITEM.getValue(PROBE));
        ItemEntity entity = new ItemEntity(helper.getLevel(), 0, 0, 0, locked);
        int pickupOps = 2_000;
        report.append("pickup_ns\t").append(fmt(nsPer(time(() -> {
            for (int i = 0; i < pickupOps; i++) {
                PickupHandler.onPickup(new ItemEntityPickupEvent.Pre(solo.player(), entity));
            }
        }), pickupOps))).append("\n");

        for (int slot = 0; slot < solo.player().getInventory().getContainerSize(); slot++) {
            solo.player().getInventory().setItem(slot, new ItemStack(Items.STONE));
        }
        int audits = 200;
        report.append("audit_us\t").append(fmt(usPer(time(() -> {
            for (int i = 0; i < audits; i++) {
                InventoryAuditor.auditNow(solo.player());
            }
        }), audits))).append("\n");
        solo.player().getInventory().clearContent();

        report.append("craft_miss_us\t").append(fmt(usPer(time(() -> {
            for (int i = 0; i < 20; i++) {
                E2eSupport.craft(solo.player(), E2eSupport.grid(new ItemStack(Items.STICK), new ItemStack(Items.STONE)));
            }
        }), 20))).append("\n");

        report.append("add_stage_ms\t").append(fmt(ms(time(() -> ChaptersAPI.addStage(solo.player(), BULK))))).append("\n");
        report.append("remove_stage_ms\t").append(fmt(ms(time(() -> ChaptersAPI.removeStage(solo.player(), BULK))))).append("\n");

        // Rebuild of every lock index from the loaded definitions (part of /reload and of every KubeJS flush).
        var definitions = new java.util.LinkedHashMap<>(StageManager.get().book().allDefinitions());
        report.append("rebuild_ms\t").append(fmt(median(7, () -> StageManager.get().book().replaceDatapack(definitions)))).append("\n");

        int lookups = 200_000;
        java.util.Set<Identifier> owned = new java.util.LinkedHashSet<>();
        for (int s = 0; s < PERF_STAGES; s += 2) {
            owned.add(perfStage(s));
        }

        int soloChanges = 2 * PERF_STAGES;
        report.append("stage_change_ms\t").append(fmt(ms(time(() -> {
            for (int s = 0; s < PERF_STAGES; s++) {
                ChaptersAPI.addStage(solo.player(), perfStage(s));
            }
            for (int s = 0; s < PERF_STAGES; s++) {
                ChaptersAPI.removeStage(solo.player(), perfStage(s));
            }
        })) / soloChanges)).append("\n");
        Identifier[] ids = new Identifier[64];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Identifier.fromNamespaceAndPath("chapters_e2e", "perf_" + (i * 31 % 2000));
        }
        report.append("lookup_ns\t").append(fmt(median(5, () -> {
            int hits = 0;
            for (int i = 0; i < lookups; i++) {
                if (StageManager.get().book().isItemRestricted(ids[i & 63], owned, com.gabinx.chapters.logic.Restriction.PICKUP)) {
                    hits++;
                }
            }
            sink = hits;
        }) * 1_000_000.0 / lookups)).append("\n");

        if (FTBTeamsAPI.api().isManagerLoaded()) {
            ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(true);
            try {
                Team team = FTBTeamsAPI.api().getManager().createPartyTeam(solo.player(), "perf", null, null);
                E2ePlayers.Joined other = E2ePlayers.join(helper);
                team.getClass().getMethod("join", ServerPlayer.class).invoke(team, other.player());
                report.append("party_add_ms\t").append(fmt(ms(time(() -> ChaptersAPI.addStage(solo.player(), BULK))))).append("\n");
                report.append("party_remove_ms\t").append(fmt(ms(time(() -> ChaptersAPI.removeStage(solo.player(), BULK))))).append("\n");

                // 40 adds then 40 removes on a 2-player party: time and bytes sent to both clients per change.
                solo.drainPayloads();
                other.drainPayloads();
                int changes = 2 * PERF_STAGES;
                long partyNanos = time(() -> {
                    for (int s = 0; s < PERF_STAGES; s++) {
                        ChaptersAPI.addStage(solo.player(), perfStage(s));
                    }
                    for (int s = 0; s < PERF_STAGES; s++) {
                        ChaptersAPI.removeStage(solo.player(), perfStage(s));
                    }
                });
                long[] sent = sentTotals(solo, other);
                report.append("party_change_ms\t").append(fmt(ms(partyNanos) / changes)).append("\n");
                report.append("party_change_payloads\t").append(fmt((double) sent[0] / changes)).append("\n");
                report.append("party_change_index_payloads\t").append(fmt((double) sent[1] / changes)).append("\n");
                report.append("party_change_kib\t").append(fmt(sent[2] / 1024.0 / changes)).append("\n");

                // First login after FTB Teams is added: 40 stages in the player file move onto the personal team.
                E2ePlayers.Joined legacy = E2ePlayers.join(helper);
                Team personal = FTBTeamsAPI.api().getManager().getTeamForPlayer(legacy.player()).orElseThrow();
                java.util.List<Identifier> legacyStages = new java.util.ArrayList<>();
                for (int s = 0; s < PERF_STAGES; s++) {
                    legacyStages.add(perfStage(s));
                }
                legacy.player().setData(
                        com.gabinx.chapters.ChaptersRegistries.PLAYER_STAGES.get(),
                        new com.gabinx.chapters.stage.PlayerStages(legacyStages));
                legacy.drainPayloads();
                long migrateNanos = time(() -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent.TeamPlayerLoggedIn(
                        new dev.ftb.mods.ftbteams.api.event.TeamPlayerLoggedInEvent.Data(personal, legacy.player()))));
                long[] migrateSent = sentTotals(legacy);
                helper.assertTrue(
                        ChaptersAPI.getStages(legacy.player()).size() == PERF_STAGES, "migration should move every stage");
                report.append("migrate40_ms\t").append(fmt(ms(migrateNanos))).append("\n");
                report.append("migrate40_index_payloads\t").append(migrateSent[1]).append("\n");
                report.append("migrate40_kib\t").append(fmt(migrateSent[2] / 1024.0)).append("\n");
            } catch (ReflectiveOperationException | com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException("perf party setup failed", e);
            }
        }

        var server = solo.player().level().getServer();
        long reloadStart = System.nanoTime();
        AtomicBoolean finished = new AtomicBoolean();
        String soFar = report.toString();
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((ignored, error) -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
                if (error != null) {
                    helper.fail("reload failed: " + error.getMessage());
                    return;
                }
                double reloadMs = (System.nanoTime() - reloadStart) / 1_000_000.0;
                Path out = server.getServerDirectory().resolve("perf-server.tsv");
                Files.writeString(out, soFar + "reload_ms\t" + fmt(reloadMs) + "\n");
                helper.succeed();
            } catch (Throwable thrown) {
                helper.fail(thrown.toString());
            }
        }, server);
    }

    private static final int PERF_STAGES = 40;
    private static volatile Object sink;

    private static Identifier perfStage(int s) {
        return Identifier.fromNamespaceAndPath("chapters", "e2e_perf_" + s);
    }

    /** {all Chapters payloads, index payloads, encoded bytes} drained from the given players. */
    private static long[] sentTotals(E2ePlayers.Joined... players) {
        long[] totals = new long[3];
        for (E2ePlayers.Joined joined : players) {
            for (var payload : joined.drainPayloads()) {
                if (!payload.type().id().getNamespace().equals("chapters")) {
                    continue;
                }
                totals[0]++;
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                if (payload instanceof ClientboundStageIndicesPayload indices) {
                    totals[1]++;
                    ClientboundStageIndicesPayload.STREAM_CODEC.encode(buf, indices);
                } else if (payload instanceof com.gabinx.chapters.network.ClientboundStagesPayload stages) {
                    com.gabinx.chapters.network.ClientboundStagesPayload.STREAM_CODEC.encode(buf, stages);
                } else if (payload instanceof com.gabinx.chapters.network.ClientboundStageDeltaPayload delta) {
                    com.gabinx.chapters.network.ClientboundStageDeltaPayload.STREAM_CODEC.encode(buf, delta);
                }
                totals[2] += buf.readableBytes();
                buf.release();
            }
        }
        return totals;
    }

    private static ClientboundStageIndicesPayload indices() {
        StageManager mgr = StageManager.get();
        return new ClientboundStageIndicesPayload(
                mgr.itemStagesIndexView(),
                mgr.fluidStagesIndexView(),
                mgr.chemicalStagesIndexView(),
                mgr.recipeStagesIndexView(),
                mgr.book().itemJeiStagesIndexView(),
                mgr.book().fluidJeiStagesIndexView(),
                mgr.book().chemicalJeiStagesIndexView(),
                mgr.book().recipeJeiStagesIndexView(),
                mgr.book().fluidTransferStagesIndexView(),
                mgr.book().chemicalTransferStagesIndexView());
    }

    private static byte[] encode(ClientboundStageIndicesPayload payload) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ClientboundStageIndicesPayload.STREAM_CODEC.encode(buf, payload);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    private static long time(Runnable body) {
        long start = System.nanoTime();
        body.run();
        return System.nanoTime() - start;
    }

    private static double median(int runs, Runnable body) {
        long[] samples = new long[runs];
        for (int i = 0; i < runs; i++) {
            samples[i] = time(body);
        }
        Arrays.sort(samples);
        return samples[runs / 2] / 1_000_000.0;
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double nsPer(long nanos, int ops) {
        return (double) nanos / ops;
    }

    private static double usPer(long nanos, int ops) {
        return nanos / 1_000.0 / ops;
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
