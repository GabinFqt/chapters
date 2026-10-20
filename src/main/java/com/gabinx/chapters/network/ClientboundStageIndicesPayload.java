package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-to-client payload carrying lock indices plus JEI-filtered indices.
 */
public record ClientboundStageIndicesPayload(
        Map<Identifier, Set<Identifier>> itemStages,
        Map<Identifier, Set<Identifier>> fluidStages,
        Map<Identifier, Set<Identifier>> chemicalStages,
        Map<Identifier, Set<Identifier>> recipeStages,
        Map<Identifier, Set<Identifier>> itemJeiStages,
        Map<Identifier, Set<Identifier>> fluidJeiStages,
        Map<Identifier, Set<Identifier>> chemicalJeiStages,
        Map<Identifier, Set<Identifier>> recipeJeiStages
) implements CustomPacketPayload {

    public ClientboundStageIndicesPayload {
        itemStages = Map.copyOf(itemStages);
        fluidStages = Map.copyOf(fluidStages);
        chemicalStages = Map.copyOf(chemicalStages);
        recipeStages = Map.copyOf(recipeStages);
        itemJeiStages = Map.copyOf(itemJeiStages);
        fluidJeiStages = Map.copyOf(fluidJeiStages);
        chemicalJeiStages = Map.copyOf(chemicalJeiStages);
        recipeJeiStages = Map.copyOf(recipeJeiStages);
    }

    public static final Type<ClientboundStageIndicesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Chapters.MOD_ID, "stage_indices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStageIndicesPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundStageIndicesPayload::write, ClientboundStageIndicesPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, ClientboundStageIndicesPayload payload) {
        writeMap(buf, payload.itemStages);
        writeMap(buf, payload.fluidStages);
        writeMap(buf, payload.chemicalStages);
        writeMap(buf, payload.recipeStages);
        writeMap(buf, payload.itemJeiStages);
        writeMap(buf, payload.fluidJeiStages);
        writeMap(buf, payload.chemicalJeiStages);
        writeMap(buf, payload.recipeJeiStages);
    }

    private static ClientboundStageIndicesPayload read(RegistryFriendlyByteBuf buf) {
        return new ClientboundStageIndicesPayload(
                readMap(buf),
                readMap(buf),
                readMap(buf),
                readMap(buf),
                readMap(buf),
                readMap(buf),
                readMap(buf),
                readMap(buf));
    }

    private static void writeMap(RegistryFriendlyByteBuf buf, Map<Identifier, Set<Identifier>> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<Identifier, Set<Identifier>> e : map.entrySet()) {
            Identifier.STREAM_CODEC.encode(buf, e.getKey());
            buf.writeVarInt(e.getValue().size());
            for (Identifier s : e.getValue()) {
                Identifier.STREAM_CODEC.encode(buf, s);
            }
        }
    }

    private static Map<Identifier, Set<Identifier>> readMap(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<Identifier, Set<Identifier>> map = new LinkedHashMap<>(n);
        for (int i = 0; i < n; i++) {
            Identifier key = Identifier.STREAM_CODEC.decode(buf);
            int m = buf.readVarInt();
            LinkedHashSet<Identifier> values = new LinkedHashSet<>(Math.max(m, 1));
            for (int j = 0; j < m; j++) {
                values.add(Identifier.STREAM_CODEC.decode(buf));
            }
            map.put(key, values);
        }
        return map;
    }

    public static void handle(ClientboundStageIndicesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStageIndices.replace(
                    payload.itemStages,
                    payload.fluidStages,
                    payload.chemicalStages,
                    payload.recipeStages,
                    payload.itemJeiStages,
                    payload.fluidJeiStages,
                    payload.chemicalJeiStages,
                    payload.recipeJeiStages
            );
            RecipeViewerCompat.refresh();
        });
    }
}
