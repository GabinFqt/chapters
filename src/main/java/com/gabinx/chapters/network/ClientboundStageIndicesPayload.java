package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-to-client payload carrying lock indices, JEI-filtered indices, and transfer-filtered fluid/chemical indices.
 */
public record ClientboundStageIndicesPayload(
        Map<ResourceLocation, Set<ResourceLocation>> itemStages,
        Map<ResourceLocation, Set<ResourceLocation>> fluidStages,
        Map<ResourceLocation, Set<ResourceLocation>> chemicalStages,
        Map<ResourceLocation, Set<ResourceLocation>> recipeStages,
        Map<ResourceLocation, Set<ResourceLocation>> itemJeiStages,
        Map<ResourceLocation, Set<ResourceLocation>> fluidJeiStages,
        Map<ResourceLocation, Set<ResourceLocation>> chemicalJeiStages,
        Map<ResourceLocation, Set<ResourceLocation>> recipeJeiStages,
        Map<ResourceLocation, Set<ResourceLocation>> fluidTransferStages,
        Map<ResourceLocation, Set<ResourceLocation>> chemicalTransferStages
) implements CustomPacketPayload {

    /**
     * Wraps instead of copying: callers pass {@code StageBook}'s frozen indices or freshly decoded maps, and
     * {@link Map#copyOf} probes very slowly on tens of thousands of ids that share a prefix.
     */
    public ClientboundStageIndicesPayload {
        itemStages = Collections.unmodifiableMap(itemStages);
        fluidStages = Collections.unmodifiableMap(fluidStages);
        chemicalStages = Collections.unmodifiableMap(chemicalStages);
        recipeStages = Collections.unmodifiableMap(recipeStages);
        itemJeiStages = Collections.unmodifiableMap(itemJeiStages);
        fluidJeiStages = Collections.unmodifiableMap(fluidJeiStages);
        chemicalJeiStages = Collections.unmodifiableMap(chemicalJeiStages);
        recipeJeiStages = Collections.unmodifiableMap(recipeJeiStages);
        fluidTransferStages = Collections.unmodifiableMap(fluidTransferStages);
        chemicalTransferStages = Collections.unmodifiableMap(chemicalTransferStages);
    }

    private static final int FLAG_JEI = 1;
    private static final int FLAG_TRANSFER = 2;
    private static final int FLAG_BITS = 2;

    public static final Type<ClientboundStageIndicesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Chapters.MOD_ID, "stage_indices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStageIndicesPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundStageIndicesPayload::write, ClientboundStageIndicesPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Wire layout: stage id palette, then one section per content kind (items, fluids, chemicals, recipes). A section
     * groups ids by namespace; each defining stage is a palette index carrying the JEI / transfer bits of the flag
     * indices, so those indices cost no extra ids on the wire.
     */
    private static void write(RegistryFriendlyByteBuf buf, ClientboundStageIndicesPayload payload) {
        List<ResourceLocation> palette = new ArrayList<>();
        Map<ResourceLocation, Integer> paletteIndex = new HashMap<>();
        for (Map<ResourceLocation, Set<ResourceLocation>> index : List.of(
                payload.itemStages, payload.fluidStages, payload.chemicalStages, payload.recipeStages)) {
            for (Set<ResourceLocation> stages : index.values()) {
                for (ResourceLocation stage : stages) {
                    if (paletteIndex.putIfAbsent(stage, palette.size()) == null) {
                        palette.add(stage);
                    }
                }
            }
        }
        buf.writeVarInt(palette.size());
        for (ResourceLocation stage : palette) {
            ResourceLocation.STREAM_CODEC.encode(buf, stage);
        }
        writeSection(buf, paletteIndex, payload.itemStages, payload.itemJeiStages, Map.of());
        writeSection(buf, paletteIndex, payload.fluidStages, payload.fluidJeiStages, payload.fluidTransferStages);
        writeSection(buf, paletteIndex, payload.chemicalStages, payload.chemicalJeiStages, payload.chemicalTransferStages);
        writeSection(buf, paletteIndex, payload.recipeStages, payload.recipeJeiStages, Map.of());
    }

    private static ClientboundStageIndicesPayload read(RegistryFriendlyByteBuf buf) {
        int paletteSize = buf.readVarInt();
        List<ResourceLocation> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }
        Section items = readSection(buf, palette);
        Section fluids = readSection(buf, palette);
        Section chemicals = readSection(buf, palette);
        Section recipes = readSection(buf, palette);
        return new ClientboundStageIndicesPayload(
                items.lock(),
                fluids.lock(),
                chemicals.lock(),
                recipes.lock(),
                items.jei(),
                fluids.jei(),
                chemicals.jei(),
                recipes.jei(),
                fluids.transfer(),
                chemicals.transfer());
    }

    /** Flag indices must be subsets of {@code lock} (as {@code StageBook} builds them); other entries are not sent. */
    private static void writeSection(
            RegistryFriendlyByteBuf buf,
            Map<ResourceLocation, Integer> paletteIndex,
            Map<ResourceLocation, Set<ResourceLocation>> lock,
            Map<ResourceLocation, Set<ResourceLocation>> jei,
            Map<ResourceLocation, Set<ResourceLocation>> transfer
    ) {
        Map<String, List<Map.Entry<ResourceLocation, Set<ResourceLocation>>>> byNamespace = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : lock.entrySet()) {
            byNamespace.computeIfAbsent(e.getKey().getNamespace(), k -> new ArrayList<>()).add(e);
        }
        buf.writeVarInt(byNamespace.size());
        for (Map.Entry<String, List<Map.Entry<ResourceLocation, Set<ResourceLocation>>>> group : byNamespace.entrySet()) {
            buf.writeUtf(group.getKey());
            buf.writeVarInt(group.getValue().size());
            for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : group.getValue()) {
                ResourceLocation id = e.getKey();
                Set<ResourceLocation> jeiStages = jei.getOrDefault(id, Set.of());
                Set<ResourceLocation> transferStages = transfer.getOrDefault(id, Set.of());
                buf.writeUtf(id.getPath());
                buf.writeVarInt(e.getValue().size());
                for (ResourceLocation stage : e.getValue()) {
                    int flags = (jeiStages.contains(stage) ? FLAG_JEI : 0)
                            | (transferStages.contains(stage) ? FLAG_TRANSFER : 0);
                    buf.writeVarInt(paletteIndex.get(stage) << FLAG_BITS | flags);
                }
            }
        }
    }

    private static Section readSection(RegistryFriendlyByteBuf buf, List<ResourceLocation> palette) {
        Map<ResourceLocation, Set<ResourceLocation>> lock = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> jei = new LinkedHashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> transfer = new LinkedHashMap<>();
        int namespaces = buf.readVarInt();
        for (int n = 0; n < namespaces; n++) {
            String namespace = buf.readUtf();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, buf.readUtf());
                int stageCount = buf.readVarInt();
                Set<ResourceLocation> stages = new LinkedHashSet<>(Math.max(stageCount, 1));
                Set<ResourceLocation> jeiStages = new LinkedHashSet<>();
                Set<ResourceLocation> transferStages = new LinkedHashSet<>();
                for (int j = 0; j < stageCount; j++) {
                    int ref = buf.readVarInt();
                    ResourceLocation stage = palette.get(ref >>> FLAG_BITS);
                    stages.add(stage);
                    if ((ref & FLAG_JEI) != 0) {
                        jeiStages.add(stage);
                    }
                    if ((ref & FLAG_TRANSFER) != 0) {
                        transferStages.add(stage);
                    }
                }
                lock.put(id, stages);
                if (!jeiStages.isEmpty()) {
                    jei.put(id, jeiStages);
                }
                if (!transferStages.isEmpty()) {
                    transfer.put(id, transferStages);
                }
            }
        }
        return new Section(lock, jei, transfer);
    }

    private record Section(
            Map<ResourceLocation, Set<ResourceLocation>> lock,
            Map<ResourceLocation, Set<ResourceLocation>> jei,
            Map<ResourceLocation, Set<ResourceLocation>> transfer) {
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
                    payload.recipeJeiStages,
                    payload.fluidTransferStages,
                    payload.chemicalTransferStages
            );
            RecipeViewerCompat.refresh();
        });
    }
}
