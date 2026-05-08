package com.gabinx.stagecraft;

import com.gabinx.stagecraft.network.ClientboundStageDeltaPayload;
import com.gabinx.stagecraft.network.ClientboundStagesPayload;
import com.gabinx.stagecraft.stage.PlayerStages;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class StagecraftRegistries {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Stagecraft.MOD_ID);

    public static final Supplier<AttachmentType<PlayerStages>> PLAYER_STAGES = ATTACHMENTS.register(
            "player_stages",
            () -> AttachmentType.builder(PlayerStages::empty)
                    .serialize(PlayerStages.CODEC)
                    .copyOnDeath()
                    .build()
    );

    private StagecraftRegistries() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(StagecraftRegistries::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ClientboundStagesPayload.TYPE, ClientboundStagesPayload.STREAM_CODEC, ClientboundStagesPayload::handle);
        registrar.playToClient(ClientboundStageDeltaPayload.TYPE, ClientboundStageDeltaPayload.STREAM_CODEC, ClientboundStageDeltaPayload::handle);
    }
}
