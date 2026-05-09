package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundStageDeltaPayload(ResourceLocation stage, boolean added) implements CustomPacketPayload {
    public static final Type<ClientboundStageDeltaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Chapters.MOD_ID, "stage_delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStageDeltaPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> payload.write(buffer),
                    ClientboundStageDeltaPayload::new
            );

    private ClientboundStageDeltaPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(stage);
        buffer.writeBoolean(added);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundStageDeltaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.added) {
                ClientStageCache.add(payload.stage);
            } else {
                ClientStageCache.remove(payload.stage);
            }
            RecipeViewerCompat.refresh();
        });
    }
}
