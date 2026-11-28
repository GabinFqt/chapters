package com.gabinx.chapters.stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.DimensionHandler;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.logic.RegistryContentCatalog;
import com.gabinx.chapters.logic.StageBook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * NeoForge reload listener that owns the live {@link StageBook} singleton used by the game.
 */
public final class StageManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final StageManager INSTANCE = new StageManager();

    private final StageBook book = new StageBook(RegistryContentCatalog.INSTANCE);

    private StageManager() {
        super(GSON, "chapters/stages");
    }

    public static StageManager get() {
        return INSTANCE;
    }

    public StageBook book() {
        return book;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, StageDefinition> next = new LinkedHashMap<>();
        objects.forEach((id, jsonElement) -> {
            if (!jsonElement.isJsonObject()) {
                Chapters.LOGGER.warn("Ignoring non-object stage definition {}", id);
                return;
            }
            JsonObject json = jsonElement.getAsJsonObject();
            next.put(id, StageDefinition.fromJson(id, json));
        });

        book.replaceDatapack(next);
        Chapters.LOGGER.info("Loaded {} datapack stage definitions", next.size());
        auditLoadedPlayersAfterReload();
        ChaptersAPI.broadcastStageIndices();
    }

    public void setRuntimeDefinitions(Collection<StageDefinition> runtime) {
        book.setRuntimeDefinitions(runtime);
        auditLoadedPlayersAfterReload();
        ChaptersAPI.broadcastStageIndices();
    }

    /**
     * The first datapack load runs before the server exists, so dimension tags and namespaces resolve to nothing
     * then. Rebuild once the levels are loaded so those entries lock from the first login, not only after /reload.
     */
    public void onServerStarted(ServerStartedEvent event) {
        if (book.usesDimensionExpansion()) {
            book.rebuild();
            Chapters.LOGGER.info("Rebuilt stage indices now that dimensions are loaded");
        }
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

    public Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndexView() {
        return book.itemStagesIndexView();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndexView() {
        return book.fluidStagesIndexView();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> chemicalStagesIndexView() {
        return book.chemicalStagesIndexView();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> recipeStagesIndexView() {
        return book.recipeStagesIndexView();
    }

    public Map<ResourceLocation, Set<ResourceLocation>> dimensionStagesIndexView() {
        return book.dimensionStagesIndexView();
    }

    public Map<ResourceLocation, StageDefinition> allDefinitions() {
        return book.allDefinitions();
    }

    public Optional<StageDefinition> get(ResourceLocation stageId) {
        return book.get(stageId);
    }

    public Set<ResourceLocation> stageIds() {
        return book.stageIds();
    }
}
