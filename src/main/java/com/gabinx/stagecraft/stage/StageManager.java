package com.gabinx.stagecraft.stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gabinx.stagecraft.Stagecraft;
import com.gabinx.stagecraft.event.InventoryAuditor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StageManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final StageManager INSTANCE = new StageManager();

    private final Map<ResourceLocation, StageDefinition> datapackDefinitions = new LinkedHashMap<>();
    private final Map<ResourceLocation, StageDefinition> runtimeDefinitions = new LinkedHashMap<>();

    /** Merged datapack + runtime definitions (immutable snapshot). */
    private List<StageDefinition> mergedDefinitions = List.of();

    /**
     * Item registry key → stage ids that gate this item (expanded from definitions). Used for fast lock checks and
     * recipe viewers.
     */
    private Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndex = Map.of();

    /** Fluid kind registry key → stage ids that gate this fluid. */
    private Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndex = Map.of();

    private StageManager() {
        super(GSON, "stagecraft/stages");
    }

    public static StageManager get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, StageDefinition> next = new LinkedHashMap<>();
        objects.forEach((id, jsonElement) -> {
            if (!jsonElement.isJsonObject()) {
                Stagecraft.LOGGER.warn("Ignoring non-object stage definition {}", id);
                return;
            }
            JsonObject json = jsonElement.getAsJsonObject();
            StageDefinition definition = StageDefinition.fromJson(id, json);
            next.put(id, definition);
        });

        synchronized (this) {
            datapackDefinitions.clear();
            datapackDefinitions.putAll(next);
            rebuildMergedAndIndicesLocked();
        }
        Stagecraft.LOGGER.info("Loaded {} datapack stage definitions", next.size());
        auditLoadedPlayersAfterReload();
    }

    public synchronized void setRuntimeDefinitions(Collection<StageDefinition> runtime) {
        runtimeDefinitions.clear();
        for (StageDefinition definition : runtime) {
            runtimeDefinitions.put(definition.id(), definition);
        }
        rebuildMergedAndIndicesLocked();
        auditLoadedPlayersAfterReload();
    }

    private void rebuildMergedAndIndicesLocked() {
        Map<ResourceLocation, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        mergedDefinitions = List.copyOf(merged.values());

        Map<ResourceLocation, Set<ResourceLocation>> itemMap = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> fluidMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            for (ResourceLocation itemId : def.items()) {
                itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (TagKey<Item> tag : def.tags()) {
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders -> {
                    for (Holder<Item> holder : holders) {
                        ResourceLocation key = BuiltInRegistries.ITEM.getKey(holder.value());
                        if (key != null) {
                            itemMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                        }
                    }
                });
            }

            for (String ns : def.namespaces()) {
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    if (key != null && ns.equals(key.getNamespace())) {
                        itemMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }

            for (ResourceLocation fluidId : def.fluids()) {
                fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (TagKey<Fluid> tag : def.fluidTags()) {
                BuiltInRegistries.FLUID.getTag(tag).ifPresent(holders -> {
                    for (Holder<Fluid> holder : holders) {
                        Fluid fluid = holder.value();
                        ResourceLocation kind = StageDefinition.fluidKindRegistryKey(fluid);
                        if (kind != null) {
                            fluidMap.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def.id());
                        }
                    }
                });
            }

            for (String ns : def.fluidNamespaces()) {
                for (Fluid fluid : BuiltInRegistries.FLUID) {
                    if (fluid == null || fluid == Fluids.EMPTY) {
                        continue;
                    }
                    ResourceLocation kind = StageDefinition.fluidKindRegistryKey(fluid);
                    if (kind != null && ns.equals(kind.getNamespace())) {
                        fluidMap.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }
        }

        itemStagesIndex = Map.copyOf(itemMap);
        fluidStagesIndex = Map.copyOf(fluidMap);
    }

    private static void auditLoadedPlayersAfterReload() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            InventoryAuditor.auditNow(player);
        }
    }

    /**
     * Snapshot of item registry key → defining stage ids (for recipe viewers). Keys are only items referenced by at
     * least one stage rule.
     */
    public synchronized Map<ResourceLocation, Set<ResourceLocation>> itemStagesIndexView() {
        return itemStagesIndex;
    }

    /**
     * Snapshot of fluid kind key → defining stage ids (for recipe viewers).
     */
    public synchronized Map<ResourceLocation, Set<ResourceLocation>> fluidStagesIndexView() {
        return fluidStagesIndex;
    }

    public synchronized Map<ResourceLocation, StageDefinition> allDefinitions() {
        Map<ResourceLocation, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        return merged;
    }

    public synchronized Optional<StageDefinition> get(ResourceLocation stageId) {
        StageDefinition runtime = runtimeDefinitions.get(stageId);
        if (runtime != null) {
            return Optional.of(runtime);
        }
        return Optional.ofNullable(datapackDefinitions.get(stageId));
    }

    public synchronized Set<ResourceLocation> stageIds() {
        Set<ResourceLocation> ids = new LinkedHashSet<>(datapackDefinitions.keySet());
        ids.addAll(runtimeDefinitions.keySet());
        return ids;
    }

    /**
     * Whether the stack is locked for this player: every defining stage must be absent for “locked”.
     */
    public synchronized boolean isItemLocked(PlayerStages stages, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        Set<ResourceLocation> defining = itemStagesIndex.get(key);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (ResourceLocation stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isFluidLocked(PlayerStages stages, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        if (kind == null) {
            return false;
        }
        Set<ResourceLocation> defining = fluidStagesIndex.get(kind);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (ResourceLocation stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }
}
