package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientRecipeStagesIndex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record ClientboundRecipeStageIndexPayload(Map<ResourceLocation, Set<ResourceLocation>> recipeStages)
        implements CustomPacketPayload {

    public ClientboundRecipeStageIndexPayload {
        recipeStages = Map.copyOf(recipeStages);
    }

    public static final Type<ClientboundRecipeStageIndexPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Chapters.MOD_ID, "recipe_stage_index"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRecipeStageIndexPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundRecipeStageIndexPayload::write, ClientboundRecipeStageIndexPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, ClientboundRecipeStageIndexPayload payload) {
        buf.writeVarInt(payload.recipeStages.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : payload.recipeStages.entrySet()) {
            ResourceLocation.STREAM_CODEC.encode(buf, e.getKey());
            buf.writeVarInt(e.getValue().size());
            for (ResourceLocation s : e.getValue()) {
                ResourceLocation.STREAM_CODEC.encode(buf, s);
            }
        }
    }

    private static ClientboundRecipeStageIndexPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<ResourceLocation, Set<ResourceLocation>> map = new LinkedHashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buf);
            int m = buf.readVarInt();
            LinkedHashSet<ResourceLocation> stages = new LinkedHashSet<>(Math.max(m, 1));
            for (int j = 0; j < m; j++) {
                stages.add(ResourceLocation.STREAM_CODEC.decode(buf));
            }
            map.put(recipeId, stages);
        }
        return new ClientboundRecipeStageIndexPayload(map);
    }

    public static void handle(ClientboundRecipeStageIndexPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientRecipeStagesIndex.replace(payload.recipeStages);
            RecipeViewerCompat.refresh();
        });
    }
}
