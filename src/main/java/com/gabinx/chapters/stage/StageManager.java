package com.gabinx.chapters.stage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.DimensionHandler;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.logic.StageBook;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * NeoForge reload listener that owns the live {@link StageBook} singleton used by the game.
 */
public final class StageManager extends SimplePreparableReloadListener<Map<Identifier, JsonObject>> {
    private static final FileToIdConverter LISTER = FileToIdConverter.json("chapters/stages");
    private static final StageManager INSTANCE = new StageManager();

    private final StageBook book = new StageBook(RegistryContentCatalog.INSTANCE);

    private StageManager() {
    }

    public static StageManager get() {
        return INSTANCE;
    }

    /** Live stage book (datapack + runtime indices). */
    public StageBook book() {
        return book;
    }

    @Override
    protected Map<Identifier, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonObject> next = new LinkedHashMap<>();
        for (var entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier id = LISTER.fileToId(fileId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    Chapters.LOGGER.warn("Ignoring non-object stage definition {}", id);
                    continue;
                }
                next.put(id, element.getAsJsonObject());
            } catch (Exception e) {
                Chapters.LOGGER.error("Failed to read stage definition {} from {}", id, fileId, e);
            }
        }
        return next;
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        book.replaceDatapackFromJson(objects);
        Chapters.LOGGER.info("Loaded {} datapack stage definitions", objects.size());
        auditLoadedPlayersAfterReload();
        ChaptersAPI.broadcastStageIndices();
    }

    public void setRuntimeDefinitions(Collection<StageDefinition> runtime) {
        book.setRuntimeDefinitions(runtime);
        auditLoadedPlayersAfterReload();
        ChaptersAPI.broadcastStageIndices();
    }

    private static void auditLoadedPlayersAfterReload() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            InventoryAuditor.auditNow(player);
            DimensionHandler.auditNow(player);
        }
    }

    public Map<Identifier, Set<Identifier>> itemStagesIndexView() {
        return book.itemStagesIndexView();
    }

    public Map<Identifier, Set<Identifier>> fluidStagesIndexView() {
        return book.fluidStagesIndexView();
    }

    public Map<Identifier, Set<Identifier>> chemicalStagesIndexView() {
        return book.chemicalStagesIndexView();
    }

    public Map<Identifier, Set<Identifier>> recipeStagesIndexView() {
        return book.recipeStagesIndexView();
    }

    public Map<Identifier, Set<Identifier>> dimensionStagesIndexView() {
        return book.dimensionStagesIndexView();
    }

    public Map<Identifier, StageDefinition> allDefinitions() {
        return book.allDefinitions();
    }

    public Optional<StageDefinition> get(Identifier stageId) {
        return book.get(stageId);
    }

    public Set<Identifier> stageIds() {
        return book.stageIds();
    }

    public boolean isItemLocked(PlayerStages stages, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return book.isItemLocked(key, stages.view());
    }

    public boolean isFluidLocked(PlayerStages stages, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        return book.isFluidLocked(kind, stages.view());
    }

    public boolean isChemicalLocked(PlayerStages stages, Identifier chemicalRegistryKey) {
        return book.isChemicalLocked(chemicalRegistryKey, stages.view());
    }

    public boolean isRecipeLocked(PlayerStages stages, Identifier recipeHolderId) {
        return book.isRecipeLocked(recipeHolderId, stages.view());
    }

    public boolean isDimensionLocked(PlayerStages stages, Identifier dimensionId) {
        return book.isDimensionLocked(dimensionId, stages.view());
    }
}
