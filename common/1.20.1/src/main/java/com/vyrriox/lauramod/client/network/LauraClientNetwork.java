package com.vyrriox.lauramod.client.network;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.gui.LauraMenuScreen;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.util.TextCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of the mod channel.
 *
 * @author vyrriox
 */
public final class LauraClientNetwork {
    private static final Map<String, byte[]> DOWNLOADS = new HashMap<>();
    private static final Map<String, Integer> RECEIVED = new HashMap<>();

    private LauraClientNetwork() {
    }

    /** Entry point for packets received by the client, already on the client thread. */
    public static void handle(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            int id = buf.readVarInt();
            Minecraft mc = Minecraft.getInstance();
            switch (id) {
                case LauraNetwork.S2C_SPEECH -> {
                    int entityId = buf.readVarInt();
                    String json = buf.readUtf(32000);
                    if (mc.level != null && LauraClientConfig.speechBubbles.get()) {
                        Component text = TextCodec.fromJson(json, mc.level.registryAccess());
                        ClientState.addBubble(entityId, text, LauraClientConfig.bubbleSeconds.getInt());
                    }
                }
                case LauraNetwork.S2C_ASSET -> handleAsset(buf);
                case LauraNetwork.S2C_ASSET_LIST -> {
                    AssetKind kind = AssetKind.byId(buf.readVarInt());
                    int count = buf.readVarInt();
                    List<String[]> entries = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        entries.add(new String[]{buf.readUtf(256), buf.readUtf(64)});
                    }
                    if (kind != null) {
                        ClientState.setAssetList(kind, entries);
                        if (mc.screen instanceof LauraMenuScreen menu) {
                            menu.onAssetList();
                        }
                    }
                }
                case LauraNetwork.S2C_SETTINGS -> ClientState.applySettings(buf.readUtf(32000));
                case LauraNetwork.S2C_MENU_CONTEXT -> {
                    int entityId = buf.readVarInt();
                    int rows = buf.readVarInt();
                    LauraInventoryMenu.setPendingClientRows(rows, entityId);
                }
                case LauraNetwork.S2C_STATUS -> {
                    int entityId = buf.readVarInt();
                    ClientState.putStatus(entityId, buf.readUtf(32000));
                    if (mc.screen instanceof LauraMenuScreen menu) {
                        menu.onStatus(entityId);
                    }
                }
                case LauraNetwork.S2C_UPLOAD_RESULT -> {
                    boolean ok = buf.readBoolean();
                    String json = buf.readUtf(32000);
                    if (mc.level != null) {
                        ClientState.lastUploadSuccess = ok;
                        ClientState.lastUploadMessage = TextCodec.fromJson(json, mc.level.registryAccess());
                        if (ok) {
                            requestList(AssetKind.SKIN);
                            requestList(AssetKind.MODEL);
                        }
                    }
                }
                default -> LauraMod.LOGGER.debug("Unknown Laura packet {}", id);
            }
        } catch (RuntimeException e) {
            LauraMod.LOGGER.warn("Malformed Laura packet: {}", e.toString());
        } finally {
            buf.release();
        }
    }

    private static void handleAsset(FriendlyByteBuf buf) {
        AssetKind kind = AssetKind.byId(buf.readVarInt());
        String name = buf.readUtf(256);
        String sha1 = buf.readUtf(64);
        int total = buf.readVarInt();
        int offset = buf.readVarInt();
        byte[] chunk = buf.readByteArray(LauraNetwork.DOWNLOAD_CHUNK + 16);
        if (kind == null) {
            return;
        }
        String key = kind.name() + "|" + name;
        int limit = (kind == AssetKind.SKIN ? LauraClientConfig.maxSkinDownloadKb.getInt() : LauraClientConfig.maxModelDownloadKb.getInt()) * 1024;
        if (total <= 0 || total > limit) {
            DOWNLOADS.remove(key);
            RECEIVED.remove(key);
            if (kind == AssetKind.SKIN) {
                SkinTextures.onServerSkinMissing(name);
            } else {
                ClientModels.onServerModelMissing(name);
            }
            return;
        }
        byte[] data = DOWNLOADS.get(key);
        if (offset == 0 || data == null || data.length != total) {
            data = new byte[total];
            DOWNLOADS.put(key, data);
            RECEIVED.put(key, 0);
        }
        if (offset < 0 || offset + chunk.length > total) {
            DOWNLOADS.remove(key);
            return;
        }
        System.arraycopy(chunk, 0, data, offset, chunk.length);
        int received = RECEIVED.getOrDefault(key, 0) + chunk.length;
        RECEIVED.put(key, received);
        if (received >= total) {
            DOWNLOADS.remove(key);
            RECEIVED.remove(key);
            if (kind == AssetKind.SKIN) {
                SkinTextures.onServerSkinReceived(name, sha1, data);
            } else {
                ClientModels.onServerModelReceived(name, sha1, data);
            }
        }
    }

    // ------------------------------------------------------------------ sending

    public static void action(int entityId, LauraAction action, String arg) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_ACTION);
        buf.writeVarInt(entityId);
        buf.writeVarInt(action.ordinal());
        buf.writeUtf(arg == null ? "" : arg, 256);
        LauraNetwork.sendToServer(buf);
    }

    public static void setSkin(int entityId, String ref, boolean slim) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_SET_SKIN);
        buf.writeVarInt(entityId);
        buf.writeUtf(ref, 2048);
        buf.writeBoolean(slim);
        LauraNetwork.sendToServer(buf);
    }

    public static void setModel(int entityId, String model) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_SET_MODEL);
        buf.writeVarInt(entityId);
        buf.writeUtf(model, 256);
        LauraNetwork.sendToServer(buf);
    }

    public static void requestAsset(AssetKind kind, String name) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_REQUEST_ASSET);
        buf.writeVarInt(kind.ordinal());
        buf.writeUtf(name, 256);
        LauraNetwork.sendToServer(buf);
    }

    public static void requestList(AssetKind kind) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_REQUEST_LIST);
        buf.writeVarInt(kind.ordinal());
        LauraNetwork.sendToServer(buf);
    }

    public static void requestStatus(int entityId) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_REQUEST_STATUS);
        buf.writeVarInt(entityId);
        LauraNetwork.sendToServer(buf);
    }

    public static void hello() {
        LauraNetwork.sendToServer(LauraNetwork.buffer(LauraNetwork.C2S_HELLO));
    }

    /** Uploads a file in chunks under the vanilla size limit. */
    public static void upload(AssetKind kind, String name, byte[] data, int entityId, boolean slim) {
        for (int offset = 0; offset < data.length; offset += LauraNetwork.UPLOAD_CHUNK) {
            int len = Math.min(LauraNetwork.UPLOAD_CHUNK, data.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(data, offset, chunk, 0, len);
            FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_UPLOAD);
            buf.writeVarInt(kind.ordinal());
            buf.writeUtf(name, 128);
            buf.writeVarInt(data.length);
            buf.writeVarInt(offset);
            buf.writeByteArray(chunk);
            buf.writeVarInt(entityId);
            buf.writeBoolean(slim);
            LauraNetwork.sendToServer(buf);
        }
    }

    public static void reset() {
        DOWNLOADS.clear();
        RECEIVED.clear();
    }
}
