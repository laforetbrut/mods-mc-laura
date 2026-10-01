package com.vyrriox.lauramod.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import com.vyrriox.lauramod.skin.SkinService;
import com.vyrriox.lauramod.util.TextCodec;
import com.vyrriox.lauramod.world.LauraActions;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * The single mod channel ({@code lauramod:main}). Loaders only move byte arrays; every message is
 * encoded and decoded here, so the protocol is identical on every loader and version.
 *
 * @author vyrriox
 */
public final class LauraNetwork {
    public static final String CHANNEL = "main";
    public static final int PROTOCOL = 2;

    /** Largest payload a loader should accept. */
    public static final int MAX_PACKET = 1_048_000;
    /** Client to server chunks must stay below the vanilla 32 KiB custom payload limit. */
    public static final int UPLOAD_CHUNK = 30_000;
    /** Largest server to client chunk a client accepts. */
    public static final int DOWNLOAD_CHUNK = 500_000;
    /**
     * Skin and model bytes one player is sent per tick (640 KB per second). Files leave the server
     * at this pace whatever the client asks, so a connection that does not read cannot make the
     * server pile up replies in memory.
     */
    public static final int ASSET_BYTES_PER_TICK = 32 * 1024;
    /** Asset and list requests a player may send in a row, then {@link #ASSET_REQUESTS_PER_SECOND}. */
    public static final int ASSET_REQUEST_BURST = 64;
    public static final int ASSET_REQUESTS_PER_SECOND = 8;
    /** A list request weighs as much as this many asset requests (its answer names every file). */
    private static final int LIST_REQUEST_COST = 4;
    /** Files waiting to be sent to one player. */
    public static final int MAX_QUEUED_ASSETS = 64;

    // client -> server
    public static final int C2S_ACTION = 1;
    public static final int C2S_SET_SKIN = 2;
    public static final int C2S_SET_MODEL = 3;
    public static final int C2S_REQUEST_ASSET = 4;
    public static final int C2S_UPLOAD = 5;
    public static final int C2S_REQUEST_LIST = 6;
    public static final int C2S_REQUEST_STATUS = 7;
    public static final int C2S_HELLO = 8;

    // server -> client
    public static final int S2C_SPEECH = 101;
    public static final int S2C_ASSET = 102;
    public static final int S2C_ASSET_LIST = 103;
    public static final int S2C_SETTINGS = 104;
    public static final int S2C_MENU_CONTEXT = 105;
    public static final int S2C_STATUS = 106;
    public static final int S2C_UPLOAD_RESULT = 107;

    private static final Map<UUID, UploadBuffer> UPLOADS = new HashMap<>();
    private static final Map<UUID, Downloads> DOWNLOADS = new HashMap<>();
    private static volatile BiConsumer<ServerPlayer, byte[]> observer;

    private LauraNetwork() {
    }

    public static FriendlyByteBuf buffer(int id) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(id);
        return buf;
    }

    public static byte[] toBytes(FriendlyByteBuf buf) {
        byte[] out = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), out);
        buf.release();
        return out;
    }

    public static void send(ServerPlayer player, FriendlyByteBuf buf) {
        byte[] bytes = toBytes(buf);
        BiConsumer<ServerPlayer, byte[]> watching = observer;
        if (watching != null) {
            watching.accept(player, bytes);
        }
        LauraMod.platform().sendToPlayer(player, bytes);
    }

    /** Lets the self tests see every message sent to players (null to stop). Not for gameplay code. */
    public static void observe(BiConsumer<ServerPlayer, byte[]> watcher) {
        observer = watcher;
    }

    public static void sendToServer(FriendlyByteBuf buf) {
        LauraMod.platform().sendToServer(toBytes(buf));
    }

    // ------------------------------------------------------------------ server side

    /** Entry point for packets received by the server, already on the server thread. */
    public static void handleServer(ServerPlayer player, byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            int id = buf.readVarInt();
            switch (id) {
                case C2S_ACTION -> {
                    int entityId = buf.readVarInt();
                    LauraAction action = LauraAction.byId(buf.readVarInt());
                    String arg = buf.readUtf(256);
                    LauraEntity laura = lauraById(player, entityId);
                    if (action != null && laura != null) {
                        LauraActions.perform(player, laura, action, arg, LauraActions.Source.MENU);
                    }
                }
                case C2S_SET_SKIN -> {
                    int entityId = buf.readVarInt();
                    String ref = buf.readUtf(2048);
                    boolean slim = buf.readBoolean();
                    LauraEntity laura = lauraById(player, entityId);
                    if (laura != null) {
                        SkinService.requestSkin(player, laura, ref, slim);
                    }
                }
                case C2S_SET_MODEL -> {
                    int entityId = buf.readVarInt();
                    String model = buf.readUtf(256);
                    LauraEntity laura = lauraById(player, entityId);
                    if (laura != null) {
                        SkinService.requestModel(player, laura, model);
                    }
                }
                case C2S_REQUEST_ASSET -> {
                    // The budget comes first: a flood is dropped before anything is decoded or looked up.
                    if (downloads(player).allow(player, 1)) {
                        AssetKind kind = AssetKind.byId(buf.readVarInt());
                        String name = buf.readUtf(256);
                        if (kind != null) {
                            requestAsset(player, kind, name);
                        }
                    }
                }
                case C2S_UPLOAD -> handleUpload(player, buf);
                case C2S_REQUEST_LIST -> {
                    if (downloads(player).allow(player, LIST_REQUEST_COST)) {
                        AssetKind kind = AssetKind.byId(buf.readVarInt());
                        if (kind != null) {
                            sendAssetList(player, kind);
                        }
                    }
                }
                case C2S_REQUEST_STATUS -> {
                    LauraEntity laura = lauraById(player, buf.readVarInt());
                    // Her status holds her home, her jobs and her needs: only for who may command her.
                    if (laura != null && LauraActions.canCommand(player, laura)) {
                        sendStatus(player, laura);
                    }
                }
                case C2S_HELLO -> sendSettings(player);
                default -> LauraMod.LOGGER.debug("Unknown packet {} from {}", id, player.getGameProfile().getName());
            }
        } catch (RuntimeException | StackOverflowError e) {
            // Nothing a client sends may reach the server loop as an error.
            LauraMod.LOGGER.warn("Malformed packet from {}: {}", player.getGameProfile().getName(), e.toString());
        } finally {
            buf.release();
        }
    }

    private static LauraEntity lauraById(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (entity instanceof LauraEntity laura && laura.isAlive() && laura.distanceToSqr(player) < 128 * 128) {
            return laura;
        }
        return null;
    }

    private static Downloads downloads(ServerPlayer player) {
        return DOWNLOADS.computeIfAbsent(player.getUUID(), id -> new Downloads());
    }

    /**
     * Puts a file in the player's queue; {@link #tick} sends it. Asking again for a file that is
     * already waiting changes nothing, and nothing is read from disk here.
     */
    private static void requestAsset(ServerPlayer player, AssetKind kind, String name) {
        if (!ServerAssetStore.isSafeName(name)) {
            return;
        }
        Downloads state = downloads(player);
        String key = kind.ordinal() + "|" + name;
        if (state.queue.containsKey(key) || state.queue.size() >= MAX_QUEUED_ASSETS) {
            return;
        }
        ServerAssetStore.Entry entry = ServerAssetStore.get(kind, name);
        if (entry == null) {
            sendAssetChunk(player, kind, name, "", 0, 0, new byte[0]);
            return;
        }
        state.queue.put(key, new Download(entry));
    }

    private static void sendAssetChunk(ServerPlayer player, AssetKind kind, String name, String sha1, int total, int offset, byte[] chunk) {
        FriendlyByteBuf buf = buffer(S2C_ASSET);
        buf.writeVarInt(kind.ordinal());
        buf.writeUtf(name, 256);
        buf.writeUtf(sha1, 64);
        buf.writeVarInt(total);
        buf.writeVarInt(offset);
        buf.writeByteArray(chunk);
        send(player, buf);
    }

    /** Sends the waiting files, at most {@link #ASSET_BYTES_PER_TICK} per player. Called every server tick. */
    public static void tick(MinecraftServer server) {
        if (DOWNLOADS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Downloads>> players = DOWNLOADS.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Downloads> state = players.next();
            ServerPlayer player = server.getPlayerList().getPlayer(state.getKey());
            if (player == null || player.hasDisconnected()) {
                players.remove();
                continue;
            }
            int budget = ASSET_BYTES_PER_TICK;
            Iterator<Download> queue = state.getValue().queue.values().iterator();
            while (budget > 0 && queue.hasNext()) {
                Download download = queue.next();
                ServerAssetStore.Entry entry = download.entry;
                if (download.bytes == null) {
                    download.bytes = ServerAssetStore.read(entry);
                    if (download.bytes == null) {
                        queue.remove();
                        continue;
                    }
                }
                int length = Math.min(budget, download.bytes.length - download.offset);
                byte[] chunk = new byte[length];
                System.arraycopy(download.bytes, download.offset, chunk, 0, length);
                sendAssetChunk(player, entry.kind(), entry.name(), entry.sha1(), download.bytes.length, download.offset, chunk);
                download.offset += length;
                // An empty file still costs one message.
                budget -= Math.max(1, length);
                if (download.offset >= download.bytes.length) {
                    queue.remove();
                }
            }
        }
    }

    /** Files waiting to be sent to this player (for the self tests and diagnostics). */
    public static int queuedAssets(ServerPlayer player) {
        Downloads state = DOWNLOADS.get(player.getUUID());
        return state == null ? 0 : state.queue.size();
    }

    public static void sendAssetList(ServerPlayer player, AssetKind kind) {
        FriendlyByteBuf buf = buffer(S2C_ASSET_LIST);
        buf.writeVarInt(kind.ordinal());
        var entries = ServerAssetStore.list(kind);
        int count = Math.min(entries.size(), 512);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUtf(entries.get(i).name(), 256);
            buf.writeUtf(entries.get(i).sha1(), 64);
        }
        send(player, buf);
    }

    private static void handleUpload(ServerPlayer player, FriendlyByteBuf buf) {
        AssetKind kind = AssetKind.byId(buf.readVarInt());
        String name = buf.readUtf(128);
        int total = buf.readVarInt();
        int offset = buf.readVarInt();
        byte[] chunk = buf.readByteArray(UPLOAD_CHUNK + 16);
        int entityId = buf.readVarInt();
        boolean slim = buf.readBoolean();
        if (kind == null) {
            return;
        }
        int limit = (kind == AssetKind.SKIN ? LauraConfig.maxSkinKb.getInt() : LauraConfig.maxModelKb.getInt()) * 1024;
        boolean allowed = kind == AssetKind.SKIN ? LauraConfig.allowSkinUploads.get() : LauraConfig.allowModelUploads.get() && LauraConfig.allowCustomModels.get();
        if (!allowed) {
            uploadResult(player, false, Component.translatable("lauramod.upload.disabled"));
            return;
        }
        // Nothing is buffered or written for a player who may not change that companion's look,
        // or who already filled their share of the uploads folder.
        LauraEntity target = lauraById(player, entityId);
        if (target == null || !SkinService.canChangeLook(player, target)) {
            uploadResult(player, false, Component.translatable("lauramod.skin.not_allowed"));
            UPLOADS.remove(player.getUUID());
            return;
        }
        if (offset == 0 && !SkinService.hasUploadRoom(player, kind, name)) {
            uploadResult(player, false, Component.translatable("lauramod.upload.quota", SkinService.uploadQuota(kind)));
            UPLOADS.remove(player.getUUID());
            return;
        }
        if (total <= 0 || total > limit) {
            uploadResult(player, false, Component.translatable("lauramod.upload.too_big", limit / 1024));
            UPLOADS.remove(player.getUUID());
            return;
        }
        UploadBuffer upload = UPLOADS.get(player.getUUID());
        if (offset == 0 || upload == null || upload.kind != kind || upload.data.length != total || !upload.name.equals(name)) {
            upload = new UploadBuffer(kind, name, new byte[total]);
            UPLOADS.put(player.getUUID(), upload);
        }
        if (offset < 0 || offset + chunk.length > total) {
            UPLOADS.remove(player.getUUID());
            return;
        }
        System.arraycopy(chunk, 0, upload.data, offset, chunk.length);
        upload.received += chunk.length;
        if (upload.received >= total) {
            UPLOADS.remove(player.getUUID());
            LauraEntity laura = lauraById(player, entityId);
            SkinService.finishUpload(player, laura, kind, name, upload.data, slim);
        }
    }

    public static void uploadResult(ServerPlayer player, boolean success, Component message) {
        FriendlyByteBuf buf = buffer(S2C_UPLOAD_RESULT);
        buf.writeBoolean(success);
        buf.writeUtf(TextCodec.toJson(message, player.registryAccess()), 32000);
        send(player, buf);
    }

    public static void forget(ServerPlayer player) {
        UPLOADS.remove(player.getUUID());
        DOWNLOADS.remove(player.getUUID());
    }

    /** Forgets every transfer (the server starts or stops). */
    public static void clear() {
        UPLOADS.clear();
        DOWNLOADS.clear();
    }

    /** Settings the client needs to build its screens. */
    public static void sendSettings(ServerPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("protocol", PROTOCOL);
        json.addProperty("allowUrlSkins", LauraConfig.allowUrlSkins.get());
        json.addProperty("allowPlayerNameSkins", LauraConfig.allowPlayerNameSkins.get());
        json.addProperty("allowSkinUploads", LauraConfig.allowSkinUploads.get());
        json.addProperty("maxSkinKb", LauraConfig.maxSkinKb.getInt());
        json.addProperty("allowCustomModels", LauraConfig.allowCustomModels.get());
        json.addProperty("allowModelUploads", LauraConfig.allowModelUploads.get());
        json.addProperty("maxModelKb", LauraConfig.maxModelKb.getInt());
        json.addProperty("fetchEnabled", LauraConfig.fetchEnabled.get());
        json.addProperty("gagEnabled", LauraConfig.gagEnabled.get());
        json.addProperty("needsEnabled", LauraConfig.needsEnabled.get());
        json.addProperty("othersCanInteract", LauraConfig.othersCanInteract.get());
        json.addProperty("speechBubbles", LauraConfig.speechBubbles.get());
        JsonArray domains = new JsonArray();
        LauraConfig.urlDomainWhitelist.get().forEach(domains::add);
        json.add("urlDomainWhitelist", domains);
        FriendlyByteBuf buf = buffer(S2C_SETTINGS);
        buf.writeUtf(json.toString(), 32000);
        send(player, buf);
    }

    /** Tells the client which Laura the menu it is about to open belongs to. */
    public static void sendMenuContext(ServerPlayer player, LauraEntity laura) {
        FriendlyByteBuf buf = buffer(S2C_MENU_CONTEXT);
        buf.writeVarInt(laura.getId());
        buf.writeVarInt(laura.inventory().getContainerSize() / 9);
        send(player, buf);
    }

    /** Detailed status for her menu (what is not already synced through entity data). */
    public static void sendStatus(ServerPlayer player, LauraEntity laura) {
        FriendlyByteBuf buf = buffer(S2C_STATUS);
        buf.writeVarInt(laura.getId());
        buf.writeUtf(laura.statusJson(player).toString(), 32000);
        send(player, buf);
    }

    /** Shows a line above her head for everyone nearby. */
    public static void sendSpeech(LauraEntity laura, Component text) {
        if (!LauraConfig.speechBubbles.get() || !(laura.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        String json = TextCodec.toJson(text, level.registryAccess());
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(laura) < 48 * 48) {
                FriendlyByteBuf buf = buffer(S2C_SPEECH);
                buf.writeVarInt(laura.getId());
                buf.writeUtf(json, 32000);
                send(p, buf);
            }
        }
    }

    /** What one player asked for: a budget of requests and the files still to send. */
    private static final class Downloads {
        final Map<String, Download> queue = new LinkedHashMap<>();
        double tokens = ASSET_REQUEST_BURST;
        int lastRefill = Integer.MIN_VALUE;

        /** Takes {@code cost} from the budget, refilled with the server ticks. False when it is used up. */
        boolean allow(ServerPlayer player, int cost) {
            MinecraftServer server = player.getServer();
            int now = server == null ? 0 : server.getTickCount();
            if (lastRefill != Integer.MIN_VALUE && now > lastRefill) {
                tokens = Math.min(ASSET_REQUEST_BURST, tokens + (now - lastRefill) * (ASSET_REQUESTS_PER_SECOND / 20.0));
            }
            lastRefill = now;
            if (tokens < cost) {
                return false;
            }
            tokens -= cost;
            return true;
        }
    }

    private static final class Download {
        final ServerAssetStore.Entry entry;
        byte[] bytes;
        int offset;

        Download(ServerAssetStore.Entry entry) {
            this.entry = entry;
        }
    }

    private static final class UploadBuffer {
        final AssetKind kind;
        final String name;
        final byte[] data;
        int received;

        UploadBuffer(AssetKind kind, String name, byte[] data) {
            this.kind = kind;
            this.name = name;
            this.data = data;
        }
    }
}
