package com.gabinx.chapters.api;

import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.network.ClientboundRecipeStageIndexPayload;
import com.gabinx.chapters.network.ClientboundStageDeltaPayload;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import com.gabinx.chapters.stage.PlayerStages;
import com.gabinx.chapters.stage.StageDefinition;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.Set;

public final class ChaptersAPI {
    private ChaptersAPI() {
    }

    public static boolean addStage(ServerPlayer player, ResourceLocation stageId) {
        PlayerStages stages = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        boolean changed = stages.add(stageId);
        if (changed) {
            player.setData(ChaptersRegistries.PLAYER_STAGES.get(), stages);
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, true));
        }
        return changed;
    }

    public static boolean removeStage(ServerPlayer player, ResourceLocation stageId) {
        PlayerStages stages = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        boolean changed = stages.remove(stageId);
        if (changed) {
            player.setData(ChaptersRegistries.PLAYER_STAGES.get(), stages);
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, false));
            InventoryAuditor.auditNow(player);
        }
        return changed;
    }

    public static boolean hasStage(ServerPlayer player, ResourceLocation stageId) {
        return player.getData(ChaptersRegistries.PLAYER_STAGES.get()).has(stageId);
    }

    public static Set<ResourceLocation> getStages(ServerPlayer player) {
        return player.getData(ChaptersRegistries.PLAYER_STAGES.get()).view();
    }

    public static Optional<StageDefinition> getDefinition(ResourceLocation stageId) {
        return StageManager.get().get(stageId);
    }

    public static void syncAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundStagesPayload(getStages(player)));
        syncRecipeStageIndexToPlayer(player);
    }

    /**
     * Sends the server's recipe lock index so JEI can evaluate {@code recipe:…} locks defined only on the server
     * (e.g. KubeJS {@code defineStage}).
     */
    public static void syncRecipeStageIndexToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new ClientboundRecipeStageIndexPayload(StageManager.get().recipeStagesIndexView()));
    }

    /** Broadcast after stage definition rebuild (datapack reload, KubeJS flush, etc.). */
    public static void broadcastRecipeStageIndex() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        var payload = new ClientboundRecipeStageIndexPayload(StageManager.get().recipeStagesIndexView());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
