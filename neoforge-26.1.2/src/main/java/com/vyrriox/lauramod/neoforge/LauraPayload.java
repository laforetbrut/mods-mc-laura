package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.network.LauraNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The single mod packet: raw bytes encoded and decoded by the common code.
 *
 * @author vyrriox
 */
public record LauraPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<LauraPayload> TYPE = new Type<>(LauraMod.id(LauraNetwork.CHANNEL));
    public static final StreamCodec<ByteBuf, LauraPayload> STREAM_CODEC =
            ByteBufCodecs.byteArray(LauraNetwork.MAX_PACKET).map(LauraPayload::new, LauraPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
