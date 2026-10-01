package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.network.LauraNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;

/**
 * The single mod channel: a raw event channel of the Forge network API, which NeoForge 47.1
 * keeps. Each packet is the byte array of the common code written with its length, the same bytes
 * the payload of later versions carries, so every loader of one Minecraft version can talk to the
 * others.
 *
 * @author vyrriox
 */
final class LauraChannel {
    static final ResourceLocation ID = LauraMod.id(LauraNetwork.CHANNEL);
    private static final String VERSION = String.valueOf(LauraNetwork.PROTOCOL);

    private static EventNetworkChannel channel;

    private LauraChannel() {
    }

    static void register() {
        // The mod is needed on both sides, like on the later versions: a missing channel or another
        // protocol is refused when the connection is made, on the client and on the server.
        channel = NetworkRegistry.newEventChannel(ID, () -> VERSION, VERSION::equals, VERSION::equals);
        channel.addListener(LauraChannel::onClientPayload);
        channel.addListener(LauraChannel::onServerPayload);
    }

    /** A packet sent by a client, received on the server network thread. */
    private static void onClientPayload(NetworkEvent.ClientCustomPayloadEvent event) {
        NetworkEvent.Context context = event.getSource().get();
        byte[] data = read(event.getPayload());
        ServerPlayer player = context.getSender();
        if (data != null && player != null) {
            context.enqueueWork(() -> LauraNetwork.handleServer(player, data));
        }
        context.setPacketHandled(true);
    }

    /** A packet sent by the server, received on the client network thread. */
    private static void onServerPayload(NetworkEvent.ServerCustomPayloadEvent event) {
        NetworkEvent.Context context = event.getSource().get();
        byte[] data = read(event.getPayload());
        if (data != null) {
            context.enqueueWork(() -> LauraMod.client().handlePacket(data));
        }
        context.setPacketHandled(true);
    }

    private static byte[] read(FriendlyByteBuf payload) {
        if (payload == null) {
            return null;
        }
        try {
            return payload.readByteArray(LauraNetwork.MAX_PACKET);
        } catch (RuntimeException e) {
            LauraMod.LOGGER.warn("Malformed packet on {}: {}", ID, e.toString());
            return null;
        }
    }

    private static FriendlyByteBuf write(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(data.length + 5));
        buf.writeByteArray(data);
        return buf;
    }

    static boolean isRemotePresent(Connection connection) {
        return connection != null && connection.channel() != null && channel.isRemotePresent(connection);
    }

    static void sendToServer(byte[] data) {
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(ID, write(data)));
    }

    static void sendToPlayer(ServerPlayer player, byte[] data) {
        player.connection.send(new ClientboundCustomPayloadPacket(ID, write(data)));
    }
}
