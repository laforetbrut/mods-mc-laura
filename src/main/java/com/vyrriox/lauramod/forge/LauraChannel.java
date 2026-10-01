package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.network.LauraNetwork;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * The single mod channel {@code lauramod:main}: raw bytes in both directions, encoded and decoded by
 * the common code.
 *
 * <p>Since 26.1 vanilla handles its packets in a queue of their own, which runs before the game
 * thread tasks. Work queued with {@code Context.enqueueWork} would then run after the vanilla
 * packets that followed it (an open screen packet would beat the menu context sent just before).
 * {@code addMain} hands the message to that same packet queue, so the order is kept.
 *
 * @author vyrriox
 */
final class LauraChannel {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(LauraMod.id(LauraNetwork.CHANNEL))
            .networkProtocolVersion(LauraNetwork.PROTOCOL)
            .simpleChannel()
            .play()
            .bidirectional()
            .addMain(Message.class, Message.STREAM_CODEC, LauraChannel::onMessage)
            .build();

    private LauraChannel() {
    }

    /** Creates the channel. Forge only accepts new channels while the mods are being constructed. */
    static void init() {
        LauraMod.LOGGER.debug("Channel {} ready (protocol {})", CHANNEL.getName(), CHANNEL.getProtocolVersion());
    }

    /** Called on the game thread, in the order the packets arrived. */
    private static void onMessage(Message message, CustomPayloadEvent.Context context) {
        if (context.isServerSide()) {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LauraNetwork.handleServer(player, message.data());
            }
        } else {
            LauraMod.client().handlePacket(message.data());
        }
    }

    static void sendToServer(byte[] data) {
        CHANNEL.send(new Message(data), PacketDistributor.SERVER.noArg());
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
        CHANNEL.send(new Message(data), connection);
    }

    /** The only message of the channel. The bytes are copied off the network buffer when decoded. */
    private record Message(byte[] data) {
        static final StreamCodec<RegistryFriendlyByteBuf, Message> STREAM_CODEC =
                ByteBufCodecs.byteArray(LauraNetwork.MAX_PACKET).map(Message::new, Message::data).cast();
    }
}
