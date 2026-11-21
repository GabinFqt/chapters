package com.gabinx.chapters.perf;

import com.gabinx.chapters.logic.ClientStageView;
import com.gabinx.chapters.logic.Restriction;
import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.stage.PlayerStages;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in benchmark: {@code ./gradlew test --tests '*PerfBenchmarkTest' -Pchapters.perf=true -Pchapters.perf.label=before}.
 * Writes a markdown report to {@code build/perf/<label>.md}.
 * <p>
 * Code paths that need a live player, JEI or a mixin are reproduced here with the same call shape as the mod
 * (see each section); everything else calls production classes directly.
 */
@EnabledIfSystemProperty(named = "chapters.perf", matches = "true")
class PerfBenchmarkTest {
    private static volatile Object sink;

    private final List<String[]> rows = new ArrayList<>();

    @Test
    void bigModpack() throws IOException {
        PerfScenario sc = new PerfScenario();

        section("Pack synthétique");
        row("Items dans le catalogue", sc.items.size(), "");
        row("Recettes (ids)", sc.recipes.size(), "");
        row("Stages", sc.stageIds.size(), "joueur en possède " + sc.ownedStages.size());

        // ---- Server: datapack reload / KubeJS flush ----
        section("Serveur: rechargement des stages (/reload, KubeJS)");
        Map<Identifier, StageDefinition> defs = parseAll(sc);
        row("Parse JSON des 120 stages", ms(medianNanos(3, 10, () -> sink = parseAll(sc))), "ms");
        StageBook book = new StageBook(sc.catalog);
        row("Reconstruction des index (StageBook)", ms(medianNanos(2, 7, () -> book.replaceDatapack(defs))), "ms");
        row("Items verrouillés (index)", book.itemStagesIndexView().size(), "");
        row("Recettes verrouillées (index)", book.recipeStagesIndexView().size(), "");

        // ---- Server: per-event lock checks ----
        section("Serveur: vérification d'un lock (par appel)");
        Random random = new Random(1);
        Identifier[] probes = new Identifier[4096];
        for (int i = 0; i < probes.length; i++) {
            probes[i] = sc.items.get(random.nextInt(sc.items.size()));
        }
        Set<Identifier> owned = Set.copyOf(sc.ownedStages);
        PlayerStages playerStages = new PlayerStages(sc.ownedStages);
        Set<String> ftbTeamStages = new HashSet<>();
        sc.ownedStages.forEach(s -> ftbTeamStages.add(s.toString()));
        int rounds = 40;
        long ops = (long) probes.length * rounds;

        row("Lookup index seul", nsPerOp(medianNanos(5, 15, () -> {
            int hits = 0;
            for (int r = 0; r < rounds; r++) {
                for (Identifier id : probes) {
                    if (book.isItemRestricted(id, owned, Restriction.PICKUP)) {
                        hits++;
                    }
                }
            }
            sink = hits;
        }), ops), "ns");
        // LockResolver.isRestricted → EffectiveStages.of(player): new LinkedHashSet<>(PlayerStages.view())
        row("Pickup/craft sans FTB Teams (copie des stages + lookup)", nsPerOp(medianNanos(5, 15, () -> {
            int hits = 0;
            for (int r = 0; r < rounds; r++) {
                for (Identifier id : probes) {
                    Set<Identifier> effective = PlayerStageSnapshots.solo(playerStages);
                    if (book.isItemRestricted(id, effective, Restriction.PICKUP)) {
                        hits++;
                    }
                }
            }
            sink = hits;
        }), ops), "ns");
        // EffectiveStages.of with FTB Teams → FtbTeamsBridge.viewStages: parse every TEAM_STAGES string
        row("Pickup/craft avec FTB Teams (parse des stages + lookup)", nsPerOp(medianNanos(5, 15, () -> {
            int hits = 0;
            for (int r = 0; r < rounds; r++) {
                for (Identifier id : probes) {
                    Set<Identifier> effective = PlayerStageSnapshots.team(ftbTeamStages);
                    if (book.isItemRestricted(id, effective, Restriction.PICKUP)) {
                        hits++;
                    }
                }
            }
            sink = hits;
        }), ops), "ns");

        // InventoryAuditor.auditNow: 41 slots, one LockResolver.isRestricted per non-empty slot, every 20 ticks
        Identifier[] slots = Arrays.copyOf(probes, 41);
        int audits = 4_000;
        row("Audit d'inventaire plein, FTB Teams (par joueur, 1x/s)", nsPerOp(medianNanos(3, 11, () -> {
            int drops = 0;
            for (int a = 0; a < audits; a++) {
                for (Identifier id : slots) {
                    if (book.isItemRestricted(id, PlayerStageSnapshots.team(ftbTeamStages), Restriction.INVENTORY)) {
                        drops++;
                    }
                }
            }
            sink = drops;
        }), audits) / 1000.0, "µs");

        // ---- Network: index payload sent on login and after every reload ----
        section("Réseau: paquet d'index (login + chaque reload, par joueur)");
        ClientboundStageIndicesPayload payload = payloadFrom(book);
        byte[] encoded = encode(payload);
        row("Taille brute", kib(encoded.length), "KiB");
        row("Taille compressée (zlib, comme le réseau MC)", kib(deflatedSize(encoded)), "KiB");
        row("Encodage (serveur)", ms(medianNanos(3, 11, () -> sink = encode(payload))), "ms");
        row("Décodage (client)", ms(medianNanos(3, 11, () -> sink = decode(encoded))), "ms");
        ClientboundStageIndicesPayload decoded = decode(encoded);
        assertEquals(payload, decoded, "payload round-trip");

        // ---- Client: recompute what the recipe viewer hides ----
        section("Client: recalcul de ce que JEI masque (chaque changement de stage)");
        ClientStageView view = new ClientStageView();
        row("Application de l'index reçu", ms(medianNanos(3, 11, () -> replaceIndices(view, decoded))), "ms");
        view.setStages(sc.ownedStages);
        row("Calcul des sets masqués (items, fluides, chimiques, recettes)",
                ms(medianNanos(3, 11, () -> sink = computeHidden(view, sc))), "ms");

        // ---- Client: JEI hide pass (ChaptersJeiModPlugin.hideOutputRecipesForItemsBatch) ----
        jeiHidePass("Client: JEI, masquage des recettes au login (joueur avec 60 stages)",
                computeHidden(view, sc).get(0), sc);
        ClientStageView newPlayer = new ClientStageView();
        replaceIndices(newPlayer, decoded);
        jeiHidePass("Client: JEI, masquage des recettes au login (nouveau joueur, 0 stage)",
                computeHidden(newPlayer, sc).get(0), sc);

        // ---- Server: crafting grid (CraftingMenuMixin + CraftingHandler) ----
        section("Serveur: grille de craft (25 000 recettes de craft)");
        if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
            // 26.1 binds item components on datapack load, so ItemStack cannot exist in unit tests.
            rows.add(new String[] {"Non mesurable ici (composants d'items non chargés en test unitaire)", "-", ""});
            writeReport();
            return;
        }
        CraftingBench crafting = new CraftingBench(25_000);
        row("Recherche recette, aucune ne correspond", nsPerOp(medianNanos(5, 21, () -> {
            for (int i = 0; i < 20; i++) {
                sink = crafting.lookup(crafting.missInput, null);
            }
        }), 20) / 1000.0, "µs");
        row("Recherche recette, trouvée au milieu", nsPerOp(medianNanos(5, 21, () -> {
            for (int i = 0; i < 20; i++) {
                sink = crafting.lookup(crafting.hitInput, null);
            }
        }), 20) / 1000.0, "µs");
        row("Vérification d'une recette déjà connue (hint)", nsPerOp(medianNanos(5, 21, () -> {
            for (int i = 0; i < 10_000; i++) {
                sink = crafting.lookup(crafting.hitInput, crafting.hitHolder);
            }
        }), 10_000), "ns");

        writeReport();
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void jeiHidePass(String title, Set<Identifier> itemsToLock, PerfScenario sc) {
        section(title);
        List<FakeRecipe> found = new ArrayList<>();
        sc.recipeOutputs.forEach((recipe, output) -> {
            if (itemsToLock.contains(output)) {
                found.add(new FakeRecipe(recipe, output));
            }
        });
        row("Items à masquer", itemsToLock.size(), "");
        row("Recettes renvoyées par JEI pour ces sorties", found.size(), "");
        assertEquals(JeiHidePass.legacy(found, itemsToLock), JeiHidePass.fixed(found, itemsToLock),
                "both hide passes hide the same recipes");
        row("Boucle de correspondance d'origine (scan de tous les items par recette)",
                ms(medianNanos(0, 3, () -> sink = JeiHidePass.legacy(found, itemsToLock))), "ms");
        row("Boucle de correspondance indexée (1 lookup par recette)",
                ms(medianNanos(3, 11, () -> sink = JeiHidePass.fixed(found, itemsToLock))), "ms");
    }

    private static Map<Identifier, StageDefinition> parseAll(PerfScenario sc) {
        Map<Identifier, StageDefinition> out = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonObject> e : sc.stageJson.entrySet()) {
            out.put(e.getKey(), StageDefinition.fromJson(e.getKey(), e.getValue()));
        }
        return out;
    }

    static ClientboundStageIndicesPayload payloadFrom(StageBook book) {
        return new ClientboundStageIndicesPayload(
                book.itemStagesIndexView(),
                book.fluidStagesIndexView(),
                book.chemicalStagesIndexView(),
                book.recipeStagesIndexView(),
                book.itemJeiStagesIndexView(),
                book.fluidJeiStagesIndexView(),
                book.chemicalJeiStagesIndexView(),
                book.recipeJeiStagesIndexView(),
                book.fluidTransferStagesIndexView(),
                book.chemicalTransferStagesIndexView());
    }

    static byte[] encode(ClientboundStageIndicesPayload payload) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ClientboundStageIndicesPayload.STREAM_CODEC.encode(buf, payload);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    static ClientboundStageIndicesPayload decode(byte[] bytes) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), RegistryAccess.EMPTY);
        ClientboundStageIndicesPayload out = ClientboundStageIndicesPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "decoder consumed the whole payload");
        return out;
    }

    private static long deflatedSize(byte[] bytes) {
        Deflater deflater = new Deflater();
        deflater.setInput(bytes);
        deflater.finish();
        byte[] chunk = new byte[1 << 16];
        long total = 0;
        while (!deflater.finished()) {
            total += deflater.deflate(chunk);
        }
        deflater.end();
        return total;
    }

    private static void replaceIndices(ClientStageView view, ClientboundStageIndicesPayload p) {
        view.replaceIndices(
                p.itemStages(),
                p.fluidStages(),
                p.chemicalStages(),
                p.recipeStages(),
                p.itemJeiStages(),
                p.fluidJeiStages(),
                p.chemicalJeiStages(),
                p.recipeJeiStages(),
                p.fluidTransferStages(),
                p.chemicalTransferStages());
    }

    /** Same work as {@code RecipeViewerCompat.applyNow} before it hands the sets to JEI. */
    private static List<Set<Identifier>> computeHidden(ClientStageView view, PerfScenario sc) {
        Set<Identifier> active = view.snapshotStages();
        Set<Identifier> lockedFluids =
                ClientStageView.computeFlagRestricted(view.fluidsView(), view.fluidJeiView(), active);
        Set<Identifier> lockedItems =
                ClientStageView.computeFlagRestricted(view.itemsView(), view.itemJeiView(), active);
        for (Identifier fluidId : lockedFluids) {
            lockedItems.addAll(sc.catalog.bucketItemsForFluid(fluidId));
        }
        Set<Identifier> lockedChemicals =
                ClientStageView.computeFlagRestricted(view.chemicalsView(), view.chemicalJeiView(), active);
        Set<Identifier> lockedRecipes =
                ClientStageView.computeFlagRestricted(view.recipesView(), view.recipeJeiView(), active);
        return List.of(new LinkedHashSet<>(lockedItems), lockedFluids, lockedChemicals, lockedRecipes);
    }

    record FakeRecipe(Identifier id, Identifier output) {
    }

    /** Matching step of {@code ChaptersJeiModPlugin.hideOutputRecipesForItemsBatch}, minus the JEI calls. */
    static final class JeiHidePass {
        /** Current shape: for every recipe, scan every locked item id. */
        static int legacy(List<FakeRecipe> found, Set<Identifier> itemsToLock) {
            Map<Identifier, List<FakeRecipe>> contributedByItem = new LinkedHashMap<>();
            for (Identifier id : itemsToLock) {
                contributedByItem.put(id, new ArrayList<>());
            }
            Map<FakeRecipe, Integer> refCount = new HashMap<>();
            List<FakeRecipe> firstHide = new ArrayList<>();
            for (FakeRecipe recipe : found) {
                Set<Identifier> outputs = new LinkedHashSet<>();
                outputs.add(recipe.output());
                for (Identifier itemId : itemsToLock) {
                    if (outputs.contains(itemId)) {
                        contributedByItem.get(itemId).add(recipe);
                        if (refCount.merge(recipe, 1, Integer::sum) == 1) {
                            firstHide.add(recipe);
                        }
                    }
                }
            }
            return firstHide.size();
        }

        /** Indexed shape: one hash lookup per recipe output. */
        static int fixed(List<FakeRecipe> found, Set<Identifier> itemsToLock) {
            Map<Identifier, List<FakeRecipe>> contributedByItem = new LinkedHashMap<>();
            for (Identifier id : itemsToLock) {
                contributedByItem.put(id, new ArrayList<>());
            }
            Map<FakeRecipe, Integer> refCount = new HashMap<>();
            List<FakeRecipe> firstHide = new ArrayList<>();
            for (FakeRecipe recipe : found) {
                Identifier output = recipe.output();
                if (output == null || !itemsToLock.contains(output)) {
                    continue;
                }
                contributedByItem.get(output).add(recipe);
                if (refCount.merge(recipe, 1, Integer::sum) == 1) {
                    firstHide.add(recipe);
                }
            }
            return firstHide.size();
        }
    }

    /**
     * Shaped 3x3 recipes over vanilla items in a real {@link RecipeMap}; {@link #lookup} mirrors
     * {@code RecipeManager.getRecipeFor(type, input, level, hint)}.
     */
    static final class CraftingBench {
        final RecipeMap recipes;
        final CraftingInput missInput;
        final CraftingInput hitInput;
        final RecipeHolder<CraftingRecipe> hitHolder;

        @SuppressWarnings("unchecked")
        CraftingBench(int count) {
            List<Item> pool = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).toList();
            assertTrue(pool.size() > 500, "vanilla items are registered in the test environment");
            Random random = new Random(7);
            List<RecipeHolder<?>> holders = new ArrayList<>(count + 1);
            for (int r = 0; r < count; r++) {
                holders.add(new RecipeHolder<>(recipeKey("craft_" + r), randomShaped(pool, random)));
                if (r == count / 2) {
                    holders.add(new RecipeHolder<>(recipeKey("target"), fullGrid(pool, 0)));
                }
            }
            recipes = RecipeMap.create(holders);
            hitHolder = (RecipeHolder<CraftingRecipe>) recipes.byKey(recipeKey("target"));
            hitInput = CraftingInput.of(3, 3, gridStacks(pool, 0));
            missInput = CraftingInput.of(3, 3, gridStacks(pool, 9));
            assertTrue(lookup(hitInput, null).isPresent(), "hit grid matches");
            assertTrue(lookup(missInput, null).isEmpty(), "miss grid matches nothing");
        }

        Optional<RecipeHolder<CraftingRecipe>> lookup(CraftingInput input, RecipeHolder<CraftingRecipe> hint) {
            if (hint != null && hint.value().matches(input, null)) {
                return Optional.of(hint);
            }
            return recipes.getRecipesFor(RecipeType.CRAFTING, input, null).findFirst();
        }

        private static ResourceKey<Recipe<?>> recipeKey(String path) {
            return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("perf", path));
        }

        private static ShapedRecipe shaped(ShapedRecipePattern pattern, Item result) {
            return new ShapedRecipe(
                    new Recipe.CommonInfo(true),
                    new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                    pattern,
                    new ItemStackTemplate(result));
        }

        private static ShapedRecipe randomShaped(List<Item> pool, Random random) {
            char[] cells = "         ".toCharArray();
            Map<Character, Ingredient> key = new HashMap<>();
            int filled = 1 + random.nextInt(9);
            for (int i = 0; i < filled; i++) {
                char symbol = (char) ('a' + i);
                cells[random.nextInt(9)] = symbol;
            }
            for (char c : cells) {
                if (c != ' ' && !key.containsKey(c)) {
                    key.put(c, Ingredient.of(pool.get(random.nextInt(pool.size()))));
                }
            }
            if (key.isEmpty()) {
                cells[0] = 'a';
                key.put('a', Ingredient.of(pool.get(random.nextInt(pool.size()))));
            }
            String all = new String(cells);
            ShapedRecipePattern pattern =
                    ShapedRecipePattern.of(key, all.substring(0, 3), all.substring(3, 6), all.substring(6, 9));
            return shaped(pattern, pool.get(random.nextInt(pool.size())));
        }

        private static ShapedRecipe fullGrid(List<Item> pool, int offset) {
            Map<Character, Ingredient> key = new HashMap<>();
            for (int i = 0; i < 9; i++) {
                key.put((char) ('a' + i), Ingredient.of(pool.get(offset + i)));
            }
            return shaped(ShapedRecipePattern.of(key, "abc", "def", "ghi"), Items.DIAMOND);
        }

        private static List<ItemStack> gridStacks(List<Item> pool, int offset) {
            List<ItemStack> stacks = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                stacks.add(new ItemStack(pool.get(offset + i)));
            }
            return stacks;
        }
    }

    // ---- timing + report helpers ----

    private static long medianNanos(int warmup, int runs, Runnable body) {
        for (int i = 0; i < warmup; i++) {
            body.run();
        }
        long[] samples = new long[runs];
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            body.run();
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        return samples[runs / 2];
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double nsPerOp(long nanos, long ops) {
        return (double) nanos / ops;
    }

    private static double kib(long bytes) {
        return bytes / 1024.0;
    }

    private void section(String title) {
        rows.add(new String[] {title});
    }

    private void row(String label, double value, String unit) {
        String formatted = value == Math.rint(value) && Math.abs(value) < 1e12
                ? String.format("%,d", (long) value)
                : String.format(value < 10 ? "%.2f" : "%,.1f", value);
        rows.add(new String[] {label, formatted, unit});
    }

    private void writeReport() throws IOException {
        String label = System.getProperty("chapters.perf.label", "run");
        StringBuilder md = new StringBuilder();
        md.append("# Chapters perf: ").append(label).append("\n\n");
        md.append("Java ").append(System.getProperty("java.version"))
                .append(", ").append(Runtime.getRuntime().availableProcessors()).append(" threads\n");
        for (String[] r : rows) {
            if (r.length == 1) {
                md.append("\n## ").append(r[0]).append("\n\n| Mesure | Valeur | Unité |\n| --- | ---: | --- |\n");
            } else {
                md.append("| ").append(r[0]).append(" | ").append(r[1]).append(" | ").append(r[2]).append(" |\n");
            }
        }
        Path out = Path.of(System.getProperty("chapters.perf.out", "build/perf")).resolve(label + ".md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, md);
        System.out.println(md);
    }
}
