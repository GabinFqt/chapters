package com.gabinx.stagecraft.api;

import com.gabinx.stagecraft.StagecraftRegistries;
import com.gabinx.stagecraft.event.InventoryAuditor;
import com.gabinx.stagecraft.network.ClientboundStageDeltaPayload;
import com.gabinx.stagecraft.network.ClientboundStagesPayload;
import com.gabinx.stagecraft.stage.PlayerStages;
import com.gabinx.stagecraft.stage.StageDefinition;
import com.gabinx.stagecraft.stage.StageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.Set;

public final class StagecraftAPI {
    private StagecraftAPI() {
    }

    public static boolean addStage(ServerPlayer player, ResourceLocation stageId) {
        PlayerStages stages = player.getData(StagecraftRegistries.PLAYER_STAGES.get());
        boolean changed = stages.add(stageId);
        if (changed) {
            player.setData(StagecraftRegistries.PLAYER_STAGES.get(), stages);
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, true));
        }
        return changed;
    }

    public static boolean removeStage(ServerPlayer player, ResourceLocation stageId) {
        PlayerStages stages = player.getData(StagecraftRegistries.PLAYER_STAGES.get());
        boolean changed = stages.remove(stageId);
        if (changed) {
            player.setData(StagecraftRegistries.PLAYER_STAGES.get(), stages);
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, false));
            InventoryAuditor.auditNow(player);
        }
        return changed;
    }

    public static boolean hasStage(ServerPlayer player, ResourceLocation stageId) {
        return player.getData(StagecraftRegistries.PLAYER_STAGES.get()).has(stageId);
    }

    public static Set<ResourceLocation> getStages(ServerPlayer player) {
        return player.getData(StagecraftRegistries.PLAYER_STAGES.get()).view();
    }

    public static Optional<StageDefinition> getDefinition(ResourceLocation stageId) {
        return StageManager.get().get(stageId);
    }

    public static void syncAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundStagesPayload(getStages(player)));
    }
}
