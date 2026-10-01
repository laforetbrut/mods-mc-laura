package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.network.LauraNetwork;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The single mod channel: a raw custom payload. Each packet is the byte array of the common code
 * written with its length, the same bytes the payload of later versions carries, so every loader of
 * one Minecraft version can talk to the others.
 *
 * @author vyrriox
 */
final class LauraChannel {
    static final ResourceLocation ID = LauraMod.id(LauraNetwork.CHANNEL);

    private LauraChannel() {
    }

    static void registerServer() {
        // Received on the server network thread: the bytes are read there, the packet is handled on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            byte[] data = read(buf);
            if (data != null) {
                server.execute(() -> LauraNetwork.handleServer(player, data));
            }
        });
    }

    static byte[] read(FriendlyByteBuf payload) {
        try {
            return payload.readByteArray(LauraNetwork.MAX_PACKET);
        } catch (RuntimeException e) {
            LauraMod.LOGGER.warn("Malformed packet on {}: {}", ID, e.toString());
            return null;
        }
    }

    static FriendlyByteBuf write(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(data.length + 5));
        buf.writeByteArray(data);
        return buf;
    }

    static boolean isRemotePresent(ServerPlayer player) {
        return player.connection != null && ServerPlayNetworking.canSend(player, ID);
    }

    static void sendToPlayer(ServerPlayer player, byte[] data) {
        ServerPlayNetworking.send(player, ID, write(data));
    }
}
