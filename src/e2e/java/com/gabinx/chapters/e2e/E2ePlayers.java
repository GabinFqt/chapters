package com.gabinx.chapters.e2e;

import com.gabinx.chapters.network.ClientboundStageDeltaPayload;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A real {@link ServerPlayer} joined through {@code placeNewPlayer}, so login, respawn, and
 * {@code ChaptersAPI.syncAll} run. The embedded channel keeps the outbound packets.
 */
public final class E2ePlayers {
    private E2ePlayers() {
    }

    public record Joined(ServerPlayer player, EmbeddedChannel channel) {
        public List<CustomPacketPayload> drainPayloads() {
            channel.runPendingTasks();
            List<CustomPacketPayload> payloads = new ArrayList<>();
            Object message;
            while ((message = channel.readOutbound()) != null) {
                if (message instanceof ClientboundCustomPayloadPacket packet) {
                    payloads.add(packet.payload());
                }
            }
            return payloads;
        }

        public boolean drainedStagesContain(net.minecraft.resources.Identifier stage) {
            for (CustomPacketPayload payload : drainPayloads()) {
                if (payload instanceof ClientboundStagesPayload stages && stages.stages().contains(stage)) {
                    return true;
                }
                if (payload instanceof ClientboundStageDeltaPayload delta && delta.added() && stage.equals(delta.stage())) {
                    return true;
                }
            }
            return false;
        }

        public boolean drainedIndicesContainItem(net.minecraft.resources.Identifier itemId) {
            for (CustomPacketPayload payload : drainPayloads()) {
                if (payload instanceof ClientboundStageIndicesPayload indices && indices.itemStages().containsKey(itemId)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static Joined join(GameTestHelper helper) {
        return join(helper, true);
    }

    /** {@code creative} is false for crafting tests, where creative click behavior would hide a blocked take. */
    public static Joined join(GameTestHelper helper, boolean creative) {
        ServerLevel level = helper.getLevel();
        UUID uuid = UUID.randomUUID();
        String name = ("e" + uuid.toString().replace("-", "")).substring(0, 16);
        GameProfile profile = new GameProfile(uuid, name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return creative ? GameType.CREATIVE : GameType.SURVIVAL;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        // The mock client never negotiates channels. Without this, KubeJS and Chapters payloads throw
        // "may not be sent to the client" during login.
        acceptModPayloads(connection);
        // Respawn builds a plain ServerPlayer on this connection. NeoForge refuses to tick a
        // plain player whose connection never went through the configuration sync task.
        markConfigSynced(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.teleportTo(feet.x, feet.y, feet.z);
        return new Joined(player, channel);
    }

    @SuppressWarnings("unchecked")
    private static void acceptModPayloads(Connection connection) {
        Set<Identifier> adhoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        try {
            Field field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            field.setAccessible(true);
            Map<ConnectionProtocol, Map<Identifier, ?>> registrations =
                    (Map<ConnectionProtocol, Map<Identifier, ?>>) field.get(null);
            Map<Identifier, ?> play = registrations.get(ConnectionProtocol.PLAY);
            if (play != null) {
                adhoc.addAll(play.keySet());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not open the mock connection to mod payloads", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void markConfigSynced(Connection connection) {
        try {
            Class<?> sync = Class.forName("net.neoforged.neoforge.network.ConfigSync");
            Field lockField = sync.getDeclaredField("lock");
            lockField.setAccessible(true);
            Field mapField = sync.getDeclaredField("configsToSync");
            mapField.setAccessible(true);
            Object lock = lockField.get(null);
            Map<Connection, Map<String, byte[]>> configs = (Map<Connection, Map<String, byte[]>>) mapField.get(null);
            synchronized (lock) {
                configs.put(connection, new LinkedHashMap<>());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register the mock connection for config sync", e);
        }
    }
}
