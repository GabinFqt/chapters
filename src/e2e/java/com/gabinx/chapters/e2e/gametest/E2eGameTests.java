package com.gabinx.chapters.e2e.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * 26.1 registers tests as instances. The built-in function registry is frozen before mods load,
 * so each instance calls its test directly. The reload test has its own environment so it runs alone.
 */
public final class E2eGameTests {
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("chapters", "empty");

    private E2eGameTests() {
    }

    public static void registerInstances(RegisterGameTestsEvent event) {
        // The integrated server syncs this registry during login. These instances are not a real
        // codec type, so registering them on the client run hangs world join on "Loading terrain...".
        // The client perf run sets this flag too. Direct instances are not a synced codec, so only the
        // headless perf server registers the timing test.
        if (Boolean.getBoolean("chapters.e2e.perf") && !Boolean.getBoolean("chapters.e2e")) {
            Holder<TestEnvironmentDefinition<?>> perf = event.registerEnvironment(
                    Identifier.fromNamespaceAndPath("chapters", "e2e_perf"),
                    new TestEnvironmentDefinition.AllOf(List.of()));
            instance(event, perf, "perf_measure", 6000, PerfGameTests::measure);
        }
        if (!Boolean.getBoolean("chapters.e2e.gametest")) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> normal = event.registerEnvironment(
                Identifier.fromNamespaceAndPath("chapters", "e2e"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Holder<TestEnvironmentDefinition<?>> reload = event.registerEnvironment(
                Identifier.fromNamespaceAndPath("chapters", "e2e_reload"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Holder<TestEnvironmentDefinition<?>> teams = event.registerEnvironment(
                Identifier.fromNamespaceAndPath("chapters", "e2e_teams"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        instance(event, normal, "soft_amethyst_keeps", 120, ItemFlagGameTests::softAmethyst_keepsAndBlocksPickup);
        instance(event, normal, "soft_amethyst_craft", 40, ItemFlagGameTests::softAmethyst_craftOutput);
        instance(event, normal, "tease_quartz", 120, ItemFlagGameTests::teaseQuartz_dropsAndBlocksPickup);
        instance(event, normal, "full_prismarine", 120, ItemFlagGameTests::fullPrismarine_dropsAndBlocksPickup);
        instance(event, normal, "planks_oak", 40, ItemFlagGameTests::planks_oakOverride);
        instance(event, normal, "or_stages", 240, ItemFlagGameTests::orStages_oneUnlocksDiamond);
        instance(event, normal, "spyglass_recipe", 40, CraftAndFluidGameTests::spyglassRecipe_blockedUntilStage);
        instance(event, normal, "lava_transfer", 80, CraftAndFluidGameTests::lava_transferAllowedAndBucketDrops);
        instance(event, normal, "water_transfer", 80, CraftAndFluidGameTests::water_transferBlockedUntilStage);
        instance(event, normal, "vanilla_water", 40, CraftAndFluidGameTests::vanillaWater_blockedUntilStage);
        instance(event, normal, "vanilla_lava", 40, CraftAndFluidGameTests::vanillaLava_placeAndPickupAllowed);
        instance(event, normal, "craft_pickup", 40, CraftGameTests::lockedResult_pickupClearsCursor);
        instance(event, normal, "craft_quick_move", 40, CraftGameTests::lockedResult_quickMoveDoesNotEnterInventory);
        instance(event, normal, "craft_inventory_grid", 40, CraftGameTests::inventoryGrid_hidesLockedPlanks);
        instance(event, normal, "craft_alt_spyglass", 40, CraftGameTests::altSpyglass_craftsWhileVanillaRecipeLocked);
        instance(event, normal, "craft_planks_tag", 40, CraftGameTests::planksTag_blocksOakAndBirchOutputs);
        instance(event, normal, "end_travel", 40, TravelGameTests::endTravel_blockedUntilStage);
        instance(event, normal, "respawn_stages", 40, TravelGameTests::respawn_keepsStagesAndSyncsThem);
        instance(event, normal, "player_file_stages", 40, TravelGameTests::savedPlayerFile_keepsAddedAndRemovedStages);
        instance(event, reload, "reload_stages", 400, TravelGameTests::reload_keepsStagesAndLocks);
        instance(event, teams, "party_share", 80, TeamGameTests::party_sharesUnlockAndAudit);
        instance(event, teams, "party_end", 80, TeamGameTests::party_ejectsBothFromEnd);
        instance(event, teams, "party_leave", 80, TeamGameTests::party_leaveKeepsCopyThenDiverges);
        instance(event, teams, "party_sync", 80, TeamGameTests::party_syncsFullPayloadNotDelta);
        instance(event, teams, "legacy_login_remove", 80, TeamGameTests::legacy_login_remove);
        instance(event, teams, "legacy_remove_team_only", 80, TeamGameTests::legacy_remove_reportsTeamOnly);
        instance(event, teams, "party_join_clears_file", 80, TeamGameTests::party_joinClearsPlayerFile);
    }

    private static void instance(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            int ticks,
            Consumer<GameTestHelper> function
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath("chapters", path),
                new DirectTest(path, function, new TestData<>(environment, STRUCTURE, ticks, 0, true, Rotation.NONE)));
    }

    /** In-memory test. Nothing encodes it, so the codec is only here to satisfy the type. */
    private static final class DirectTest extends GameTestInstance {
        private final String name;
        private final Consumer<GameTestHelper> function;

        private DirectTest(String name, Consumer<GameTestHelper> function, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
            super(info);
            this.name = name;
            this.function = function;
        }

        @Override
        public void run(GameTestHelper helper) {
            function.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            throw new UnsupportedOperationException(name + " is not synced to clients");
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal(name);
        }
    }
}
