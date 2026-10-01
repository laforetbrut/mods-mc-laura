package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.network.LauraNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;

/**
 * The single mod channel {@code lauramod:main}: raw bytes in both directions, encoded and decoded by
 * the common code.
 *
 * @author vyrriox
 */
final class LauraChannel {
    private static final EventNetworkChannel CHANNEL = ChannelBuilder.named(LauraMod.id(LauraNetwork.CHANNEL))
            .networkProtocolVersion(LauraNetwork.PROTOCOL)
            .eventNetworkChannel()
            .addListener(LauraChannel::onPacket);

    private LauraChannel() {
    }

    /** Creates the channel. Forge only accepts new channels while the mods are being constructed. */
    static void init() {
        LauraMod.LOGGER.debug("Channel {} ready (protocol {})", CHANNEL.getName(), CHANNEL.getProtocolVersion());
    }

    private static void onPacket(CustomPayloadEvent event) {
        CustomPayloadEvent.Context context = event.getSource();
        FriendlyByteBuf payload = event.getPayload();
        if (payload == null) {
            return;
        }
        // The buffer belongs to the network thread: copy it before handing the work to the game thread.
        byte[] data = new byte[payload.readableBytes()];
        payload.readBytes(data);
        if (context.isServerSide()) {
            ServerPlayer player = context.getSender();
            if (player != null) {
                context.enqueueWork(() -> LauraNetwork.handleServer(player, data));
            }
        } else {
            context.enqueueWork(() -> LauraMod.client().handlePacket(data));
        }
        context.setPacketHandled(true);
    }

    static void sendToServer(byte[] data) {
        CHANNEL.send(wrap(data), PacketDistributor.SERVER.noArg());
    }

    /** Sends to one player, or does nothing when that player's client does not have the channel. */
    static void sendToPlayer(ServerPlayer player, byte[] data) {
        if (player.connection == null) {
            return;
        }
        Connection connection = player.connection.getConnection();
        if (connection == null || !CHANNEL.isRemotePresent(connection)) {
            return;
        }
        CHANNEL.send(wrap(data), connection);
    }

    private static FriendlyByteBuf wrap(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }
}
