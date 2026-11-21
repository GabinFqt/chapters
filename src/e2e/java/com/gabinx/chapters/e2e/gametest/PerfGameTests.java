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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server timings for the perf pack. Not part of the required {@code chapters} namespace.
 * Uses only APIs that exist before and after the lock-check speedup.
 */
@GameTestHolder("chapters_perf")
@PrefixGameTestTemplate(false)
public class PerfGameTests {
    private static final ResourceLocation BULK = ResourceLocation.fromNamespaceAndPath("chapters", "e2e_perf_bulk");
    private static final ResourceLocation PROBE = ResourceLocation.fromNamespaceAndPath("chapters_e2e", "perf_0");

    @GameTest(template = "empty", batch = "chaptersperf", timeoutTicks = 6000)
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

        ItemStack locked = new ItemStack(BuiltInRegistries.ITEM.get(PROBE));
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

        if (FTBTeamsAPI.api().isManagerLoaded()) {
            ChaptersConfig.SYNC_WITH_FTB_TEAMS.set(true);
            try {
                Team team = FTBTeamsAPI.api().getManager().createPartyTeam(solo.player(), "perf", null, null);
                E2ePlayers.Joined other = E2ePlayers.join(helper);
                team.getClass().getMethod("join", ServerPlayer.class).invoke(team, other.player());
                report.append("party_add_ms\t").append(fmt(ms(time(() -> ChaptersAPI.addStage(solo.player(), BULK))))).append("\n");
                report.append("party_remove_ms\t").append(fmt(ms(time(() -> ChaptersAPI.removeStage(solo.player(), BULK))))).append("\n");
            } catch (ReflectiveOperationException | com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException("perf party setup failed", e);
            }
        }

        var server = solo.player().server;
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
