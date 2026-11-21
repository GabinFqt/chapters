package com.gabinx.chapters.e2e.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drives a fresh creative world through the JEI rows of the flags protocol and writes
 * {@code e2e-report.md}. The process exits 0 only when every row passes.
 */
public final class E2eClientHarness {
    private static final String WORLD_NAME = "flags-e2e";
    private static final Pattern RUNTIME_COUNT = Pattern.compile("at runtime: (\\d+)");
    private static final Pattern REFRESH_MS = Pattern.compile("(\\d+) ms");

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<String> ROWS = new ArrayList<>();
    private static int bootTicks;
    private static int stepIndex;
    private static boolean createOpened;
    private static boolean createSubmitted;
    private static boolean inWorld;
    private static boolean failed;
    private static boolean finished;
    private static String worldFolder = WORLD_NAME;

    private E2eClientHarness() {
    }

    public static void start() {
        LogTap.install();
        NeoForge.EVENT_BUS.addListener(E2eClientHarness::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (finished) {
            return;
        }
        try {
            tick();
        } catch (Throwable thrown) {
            row("harness", false, thrown.toString());
            finish();
        }
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        if (!inWorld) {
            boot(minecraft);
            return;
        }
        if (stepIndex >= STEPS.size()) {
            finish();
            return;
        }
        if (STEPS.get(stepIndex).tick(minecraft)) {
            stepIndex++;
        }
    }

    private static void boot(Minecraft minecraft) {
        bootTicks++;
        if (!createOpened) {
            createOpened = true;
            deleteWorld(minecraft);
            minecraft.tell(() -> CreateWorldScreen.openFresh(minecraft, null));
            return;
        }
        if (!createSubmitted && minecraft.screen instanceof CreateWorldScreen screen) {
            submitWorld(screen);
            createSubmitted = true;
            return;
        }
        if (createSubmitted
                && minecraft.player != null
                && minecraft.level != null
                && minecraft.screen == null
                && JeiProbes.ready()
                && !JeiProbes.itemVisible(Items.PRISMARINE_SHARD)) {
            LogTap.mark();
            inWorld = true;
            buildScript();
            return;
        }
        if (bootTicks > 3600) {
            row("0.boot", false, "timed out waiting for a world with JEI locks applied");
            finish();
        }
    }

    private static void submitWorld(CreateWorldScreen screen) {
        WorldCreationUiState ui = screen.getUiState();
        ui.setName(WORLD_NAME);
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        ui.setAllowCommands(true);
        selectFlat(ui.getNormalPresetList(), ui);
        selectFlat(ui.getAltPresetList(), ui);
        worldFolder = ui.getTargetFolder();
        try {
            Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            onCreate.invoke(screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the e2e world", e);
        }
    }

    private static void selectFlat(List<WorldCreationUiState.WorldTypeEntry> entries, WorldCreationUiState ui) {
        for (WorldCreationUiState.WorldTypeEntry entry : entries) {
            if (entry.preset() != null && entry.preset().is(WorldPresets.FLAT)) {
                ui.setWorldType(entry);
                return;
            }
        }
    }

    private static void deleteWorld(Minecraft minecraft) {
        Path saves = minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD_NAME);
        if (Files.exists(saves)) {
            deleteRecursively(saves);
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var children = Files.list(path)) {
                    children.forEach(E2eClientHarness::deleteRecursively);
                }
            }
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // A leftover lock is reported by world creation if it still blocks the new world.
        }
    }

    private static void buildScript() {
        // Visible even while locked (hideInJei false).
        check("1A.1", () -> JeiProbes.itemVisible(Items.AMETHYST_SHARD), "amethyst shard should be visible");
        check("2A.1", () -> JeiProbes.itemVisible(Items.QUARTZ), "quartz should be visible");
        check("3A.1", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "prismarine shard should be hidden");
        check("4A.3", () -> !JeiProbes.itemVisible(Items.BIRCH_PLANKS), "birch planks should be hidden");
        check("4A.4", () -> !JeiProbes.itemVisible(Items.OAK_PLANKS), "oak planks should be hidden");
        check("5A.3", () -> !JeiProbes.spyglassRecipeVisible(), "spyglass recipe should be hidden");
        check("6A.1", () -> !JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.LAVA)
                && !JeiProbes.itemVisible(Items.LAVA_BUCKET), "lava and lava bucket should be hidden");
        check("7A.1", () -> !JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.WATER)
                && !JeiProbes.itemVisible(Items.WATER_BUCKET), "water and water bucket should be hidden");

        mark();
        command("chapters add @s chapters:e2e_full_prismarine");
        settle();
        check("3B.1", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "prismarine should reappear without a restart");
        command("chapters remove @s chapters:e2e_full_prismarine");
        settle();
        check("3C.1", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "prismarine should hide again");

        mark();
        command("chapters add @s chapters:e2e_planks");
        settle();
        check("4B.2", () -> JeiProbes.itemVisible(Items.OAK_PLANKS) && JeiProbes.itemVisible(Items.BIRCH_PLANKS),
                "oak and birch should both be visible");
        expect("11D.1", lines -> countAdded(lines, "ItemStack") == 1 && addedCount(lines, "ItemStack") > 1,
                "one added ItemStack line, count greater than 1");
        mark();
        command("chapters remove @s chapters:e2e_planks");
        settle();
        expect("11D.2", lines -> countRemoved(lines, "ItemStack") == 1, "one removed ItemStack line");

        mark();
        command("chapters add @s chapters:e2e_water_transfer");
        settle();
        check("7B.2", () -> JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.WATER)
                && JeiProbes.itemVisible(Items.WATER_BUCKET), "water and the bucket should be visible");
        expect("11D.3", lines -> countAdded(lines, "ItemStack") == 1 && countAdded(lines, "FluidStack") == 1,
                "one added ItemStack line and one added FluidStack line");
        mark();
        command("chapters remove @s chapters:e2e_water_transfer");
        settle();
        expect("11D.4", lines -> countRemoved(lines, "ItemStack") == 1 && countRemoved(lines, "FluidStack") == 1,
                "one removed line per type");
        check("11D.5", () -> !JeiProbes.itemVisible(Items.BIRCH_PLANKS)
                && !JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.WATER)
                && !JeiProbes.itemVisible(Items.WATER_BUCKET), "planks, water, and the bucket should be hidden");

        mark();
        command("chapters add @s chapters:e2e_lava_no_transfer");
        settle();
        check("6B.1", () -> JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.LAVA)
                && JeiProbes.itemVisible(Items.LAVA_BUCKET), "lava and the bucket should be visible");
        command("chapters remove @s chapters:e2e_lava_no_transfer");
        settle();

        mark();
        command("chapters add @s chapters:e2e_recipe_spyglass");
        settle();
        check("5B.2", JeiProbes::spyglassRecipeVisible, "spyglass recipe should be visible");
        command("chapters remove @s chapters:e2e_recipe_spyglass");
        settle();

        // Respawn with the stages (11A), then without them (11B).
        command("chapters add @s chapters:e2e_full_prismarine");
        command("chapters add @s chapters:e2e_planks");
        settle();
        check("11A.1", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD) && JeiProbes.itemVisible(Items.BIRCH_PLANKS),
                "unlocked items should be visible before dying");
        for (int i = 0; i < 3; i++) {
            respawn("11A." + (i == 0 ? "2" : "8"));
        }
        check("11A.3", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD) && JeiProbes.itemVisible(Items.BIRCH_PLANKS),
                "unlocked items should stay visible after respawn");
        check("11A.4", () -> JeiProbes.craftingRecipeVisible(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "birch_planks"), Items.BIRCH_PLANKS),
                "birch planks recipe should be visible");
        command("gamerule keepInventory true");
        respawn("11A.9");
        check("11A.9b", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "keepInventory respawn should keep JEI");
        command("gamerule keepInventory false");

        command("chapters remove @s chapters:e2e_full_prismarine");
        command("chapters remove @s chapters:e2e_planks");
        settle();
        check("11B.1", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD) && !JeiProbes.itemVisible(Items.BIRCH_PLANKS),
                "locks should hide the items again");
        respawn("11B.2");
        check("11B.3", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD) && !JeiProbes.itemVisible(Items.BIRCH_PLANKS),
                "respawn should not reveal locked items");

        // End exit uses the same server path as respawn.
        command("chapters add @s chapters:e2e_end_travel");
        command("chapters add @s chapters:e2e_full_prismarine");
        settle();
        mark();
        // forceload takes block coordinates. Chunk 0,0 is not the portal at x=100.
        command("execute in minecraft:the_end run forceload add 80 -16 112 16");
        STEPS.add(new Wait(40));
        command("execute in minecraft:the_end run fill 98 79 -2 102 79 2 minecraft:obsidian");
        command("execute in minecraft:the_end run setblock 100 80 0 minecraft:end_portal");
        STEPS.add(new Wait(10));
        command("execute in minecraft:the_end run tp @s 100.5 80.5 0.5");
        STEPS.add(new Until("11C.1", E2eClientHarness::backInOverworld, 600, "should return to the Overworld"));
        check("11C.2", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "prismarine should stay visible after leaving the End");
        expect("11C.3", E2eClientHarness::noIngredientLines, "leaving the End should not touch JEI ingredients");
        command("chapters remove @s chapters:e2e_end_travel");
        command("chapters remove @s chapters:e2e_full_prismarine");
        settle();

        mark();
        command("reload");
        STEPS.add(new Until("11E.1", E2eClientHarness::settledInWorld, 3600, "reload should return to the world"));
        expect("11E.2", E2eClientHarness::reloadLogOk, "at most one removed line per type, no added lines, no count of 1");
        check("11E.3", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD)
                && !JeiProbes.itemVisible(Items.BIRCH_PLANKS)
                && !JeiProbes.fluidVisible(net.minecraft.world.level.material.Fluids.WATER),
                "locked items should stay hidden after reload");
        check("11E.4", () -> JeiProbes.itemVisible(Items.QUARTZ) && JeiProbes.itemVisible(Items.AMETHYST_SHARD),
                "hideInJei false items should stay visible");
        command("chapters add @s chapters:e2e_full_prismarine");
        settle();
        mark();
        command("reload");
        STEPS.add(new Until("11E.5a", E2eClientHarness::settledInWorld, 3600, "second reload should return"));
        check("11E.5", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "an owned item should stay visible across reload");
        expect("11E.5b", lines -> countAdded(lines, "ItemStack") == 0, "reload should not add ingredients");
        command("chapters remove @s chapters:e2e_full_prismarine");
        settle();

        rejoin("11F.1");
        expect("11F.2", E2eClientHarness::reloadLogOk, "rejoin should remove at most once per type and add nothing");
        check("11F.3", () -> !JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "prismarine should be hidden after rejoin");

        command("chapters add @s chapters:e2e_full_prismarine");
        settle();
        rejoin("10.3");
        check("10.3", () -> JeiProbes.itemVisible(Items.PRISMARINE_SHARD), "the stage should still be owned after rejoin");
        command("chapters remove @s chapters:e2e_full_prismarine");
        settle();

        mark();
        command("chapters add @s chapters:e2e_big");
        STEPS.add(new Until("11I.1", () -> JeiProbes.itemVisible(Items.WHITE_WOOL), 600, "big stage should reveal wool"));
        expect("11I.2", lines -> countAdded(lines, "ItemStack") == 1 && addedCount(lines, "ItemStack") > 1,
                "one added ItemStack line for the whole stage");
        STEPS.add(minecraft -> {
            String ms = refreshMillis(LogTap.sinceMark());
            row("11I.3", ms != null, ms == null ? "missing Recipe viewer refresh line" : "refresh " + ms + " ms");
            return true;
        });
        check("11I.4", () -> JeiProbes.itemVisible(Items.WHITE_WOOL)
                && JeiProbes.itemVisible(Items.RED_BED)
                && JeiProbes.itemVisible(Items.WHITE_BANNER), "wool, bed, and banner should be visible");
        check("11I.5", () -> JeiProbes.craftingRecipeVisible(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "red_bed"), Items.RED_BED),
                "red bed recipe should be visible");
        mark();
        command("chapters remove @s chapters:e2e_big");
        settle();
        expect("11I.6", lines -> countRemoved(lines, "ItemStack") == 1, "one removed ItemStack line");
        check("11I.6b", () -> !JeiProbes.itemVisible(Items.WHITE_WOOL) && !JeiProbes.itemVisible(Items.RED_BED),
                "wool and beds should hide again");
        STEPS.add(minecraft -> {
            String ms = refreshMillis(LogTap.sinceMark());
            row("11I.7", ms != null, ms == null ? "missing remove refresh line" : "remove " + ms + " ms");
            return true;
        });
        for (int cycle = 2; cycle <= 3; cycle++) {
            int n = cycle;
            mark();
            command("chapters add @s chapters:e2e_big");
            STEPS.add(new Until("11I.8-add-" + n, () -> JeiProbes.itemVisible(Items.WHITE_WOOL), 600, "cycle add"));
            mark();
            command("chapters remove @s chapters:e2e_big");
            STEPS.add(new Until("11I.8-remove-" + n, () -> !JeiProbes.itemVisible(Items.WHITE_WOOL), 600, "cycle remove"));
        }
        command("chapters add @s chapters:e2e_planks");
        command("chapters add @s chapters:e2e_big");
        settle();
        command("chapters remove @s chapters:e2e_big");
        settle();
        check("11I.9", () -> JeiProbes.itemVisible(Items.BIRCH_PLANKS) && !JeiProbes.itemVisible(Items.WHITE_WOOL),
                "planks stay visible when only the big stage is removed");
    }

    private static void respawn(String id) {
        mark();
        command("kill @s");
        STEPS.add(new Until(id, E2eClientHarness::deathOrGone, 1200, "death screen"));
        STEPS.add(minecraft -> {
            if (minecraft.player instanceof LocalPlayer local && (local.isDeadOrDying() || minecraft.screen instanceof DeathScreen)) {
                local.respawn();
            }
            return true;
        });
        STEPS.add(new Until(id + "-back", E2eClientHarness::aliveInWorld, 1200, "respawn"));
        expect(id + "-log", E2eClientHarness::noIngredientLines, "respawn should not touch JEI ingredients");
    }

    private static void rejoin(String id) {
        mark();
        STEPS.add(minecraft -> {
            quitToTitle(minecraft);
            return true;
        });
        STEPS.add(new Until(id + "-title", () -> {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player == null && minecraft.screen instanceof TitleScreen;
        }, 600, "should reach the title screen"));
        STEPS.add(minecraft -> {
            minecraft.tell(() -> minecraft.createWorldOpenFlows().openWorld(worldFolder, () -> {
                row(id, false, "could not reopen " + worldFolder);
                finish();
            }));
            return true;
        });
        STEPS.add(new Until(id, E2eClientHarness::settledInWorld, 3600, "should rejoin the world"));
    }

    private static void quitToTitle(Minecraft minecraft) {
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        minecraft.setScreen(new TitleScreen());
    }

    private static boolean deathOrGone() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof DeathScreen || minecraft.player == null || minecraft.player.isDeadOrDying();
    }

    private static boolean aliveInWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.isAlive()
                && minecraft.level != null
                && !(minecraft.screen instanceof DeathScreen);
    }

    private static boolean settledInWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null && minecraft.screen == null && JeiProbes.ready();
    }

    private static boolean backInOverworld() {
        Minecraft minecraft = Minecraft.getInstance();
        // Closing the credits screen only calls removed(). The respawn packet is what brings the player back.
        if (minecraft.screen instanceof WinScreen && minecraft.player != null) {
            minecraft.player.connection.send(
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            minecraft.setScreen(null);
        }
        return minecraft.player != null
                && minecraft.player.isAlive()
                && minecraft.level != null
                && minecraft.level.dimension() == Level.OVERWORLD
                && minecraft.screen == null;
    }

    private static void command(String command) {
        STEPS.add(minecraft -> {
            if (!(minecraft.player instanceof LocalPlayer local)) {
                row("command", false, "no player for /" + command);
                return true;
            }
            local.connection.sendCommand(command);
            return true;
        });
    }

    private static void settle() {
        STEPS.add(new Wait(20));
    }

    private static void mark() {
        STEPS.add(minecraft -> {
            LogTap.mark();
            return true;
        });
    }

    private static void check(String id, BooleanSupplier ok, String why) {
        STEPS.add(minecraft -> {
            boolean pass;
            try {
                pass = ok.getAsBoolean();
            } catch (RuntimeException e) {
                row(id, false, e.toString());
                return true;
            }
            row(id, pass, pass ? "" : why);
            return true;
        });
    }

    private static void expect(String id, Predicate<List<String>> test, String why) {
        STEPS.add(minecraft -> {
            List<String> lines = LogTap.sinceMark();
            boolean pass;
            try {
                pass = test.test(lines);
            } catch (RuntimeException e) {
                row(id, false, e.toString());
                return true;
            }
            row(id, pass, pass ? "" : why + " lines=" + lines);
            return true;
        });
    }

    private static boolean noIngredientLines(List<String> lines) {
        return lines.stream().noneMatch(line -> line.contains("Ingredients are being"));
    }

    private static boolean reloadLogOk(List<String> lines) {
        return countAdded(lines, "ItemStack") == 0
                && countAdded(lines, "FluidStack") == 0
                && countRemoved(lines, "ItemStack") <= 1
                && countRemoved(lines, "FluidStack") <= 1
                && lines.stream().noneMatch(line -> line.contains("at runtime: 1 "));
    }

    private static long countAdded(List<String> lines, String type) {
        return lines.stream().filter(line -> line.contains("are being added") && line.contains(type)).count();
    }

    private static long countRemoved(List<String> lines, String type) {
        return lines.stream().filter(line -> line.contains("are being removed") && line.contains(type)).count();
    }

    private static int addedCount(List<String> lines, String type) {
        for (String line : lines) {
            if (line.contains("are being added") && line.contains(type)) {
                Matcher matcher = RUNTIME_COUNT.matcher(line);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }
        return -1;
    }

    private static String refreshMillis(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Recipe viewer refresh")) {
                Matcher matcher = REFRESH_MS.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    private static void row(String id, boolean pass, String detail) {
        if (!pass) {
            failed = true;
        }
        ROWS.add("| " + id + " | " + (pass ? "PASS" : "FAIL") + " | " + detail.replace("|", "/") + " |");
    }

    private static void finish() {
        if (finished) {
            return;
        }
        finished = true;
        Minecraft minecraft = Minecraft.getInstance();
        Path report = minecraft.gameDirectory.toPath().resolve("e2e-report.md");
        StringBuilder text = new StringBuilder();
        text.append("# Chapters e2e\n\n");
        text.append(failed ? "Result: FAIL\n\n" : "Result: PASS\n\n");
        text.append("| Id | Result | Detail |\n| --- | --- | --- |\n");
        for (String line : ROWS) {
            text.append(line).append('\n');
        }
        try {
            Files.writeString(report, text.toString());
        } catch (Exception e) {
            System.err.println("Could not write " + report + ": " + e);
        }
        System.exit(failed ? 1 : 0);
    }

    @FunctionalInterface
    private interface Step {
        boolean tick(Minecraft minecraft);
    }

    private static final class Wait implements Step {
        private int left;

        private Wait(int ticks) {
            this.left = ticks;
        }

        @Override
        public boolean tick(Minecraft minecraft) {
            return --left <= 0;
        }
    }

    private static final class Until implements Step {
        private final String id;
        private final BooleanSupplier condition;
        private int left;
        private final String why;

        private Until(String id, BooleanSupplier condition, int timeout, String why) {
            this.id = id;
            this.condition = condition;
            this.left = timeout;
            this.why = why;
        }

        @Override
        public boolean tick(Minecraft minecraft) {
            if (condition.getAsBoolean()) {
                row(id, true, "");
                return true;
            }
            if (--left <= 0) {
                String screen = minecraft.screen == null ? "no screen" : minecraft.screen.getClass().getSimpleName();
                row(id, false, why + " (" + screen + ")");
                return true;
            }
            return false;
        }
    }
}
