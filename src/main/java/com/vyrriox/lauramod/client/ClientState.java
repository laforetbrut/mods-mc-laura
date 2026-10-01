package com.vyrriox.lauramod.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vyrriox.lauramod.skin.AssetKind;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the client knows about the server and about each Laura, received through the mod channel.
 *
 * @author vyrriox
 */
public final class ClientState {
    /** Settings sent by the server (defaults are the values of a vanilla-configured server). */
    public static boolean allowUrlSkins = true;
    public static boolean allowPlayerNameSkins = true;
    public static boolean allowSkinUploads = true;
    public static int maxSkinKb = 256;
    public static boolean allowCustomModels = true;
    public static boolean allowModelUploads = false;
    public static int maxModelKb = 2048;
    public static boolean fetchEnabled = true;
    public static boolean gagEnabled = true;
    public static boolean needsEnabled = true;
    public static boolean othersCanInteract = false;
    public static boolean serverKnown = false;
    public static List<String> urlWhitelist = new ArrayList<>();

    /** Latest detailed status per entity id. */
    private static final Map<Integer, JsonObject> STATUS = new HashMap<>();
    /** Speech bubbles: entity id to text and expiry (client ticks). */
    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();
    private static final Map<AssetKind, List<String[]>> ASSET_LISTS = new EnumMap<>(AssetKind.class);
    public static Component lastUploadMessage;
    public static boolean lastUploadSuccess;
    public static long ticks;

    private ClientState() {
    }

    public record Bubble(Component text, long expires) {
    }

    public static void applySettings(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        allowUrlSkins = bool(o, "allowUrlSkins", true);
        allowPlayerNameSkins = bool(o, "allowPlayerNameSkins", true);
        allowSkinUploads = bool(o, "allowSkinUploads", true);
        maxSkinKb = integer(o, "maxSkinKb", 256);
        allowCustomModels = bool(o, "allowCustomModels", true);
        allowModelUploads = bool(o, "allowModelUploads", false);
        maxModelKb = integer(o, "maxModelKb", 2048);
        fetchEnabled = bool(o, "fetchEnabled", true);
        gagEnabled = bool(o, "gagEnabled", true);
        needsEnabled = bool(o, "needsEnabled", true);
        othersCanInteract = bool(o, "othersCanInteract", false);
        List<String> domains = new ArrayList<>();
        if (o.has("urlDomainWhitelist") && o.get("urlDomainWhitelist").isJsonArray()) {
            JsonArray a = o.getAsJsonArray("urlDomainWhitelist");
            for (JsonElement e : a) {
                domains.add(e.getAsString());
            }
        }
        urlWhitelist = domains;
        serverKnown = true;
    }

    public static void putStatus(int entityId, String json) {
        try {
            STATUS.put(entityId, JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException ignored) {
            // Malformed status: keep the previous one.
        }
    }

    public static JsonObject status(int entityId) {
        return STATUS.get(entityId);
    }

    public static void addBubble(int entityId, Component text, int seconds) {
        BUBBLES.put(entityId, new Bubble(text, ticks + seconds * 20L));
    }

    public static Bubble bubble(int entityId) {
        Bubble b = BUBBLES.get(entityId);
        if (b != null && b.expires() < ticks) {
            BUBBLES.remove(entityId);
            return null;
        }
        return b;
    }

    public static void setAssetList(AssetKind kind, List<String[]> entries) {
        ASSET_LISTS.put(kind, List.copyOf(entries));
    }

    /** Pairs of (name, sha1). */
    public static List<String[]> assetList(AssetKind kind) {
        return ASSET_LISTS.getOrDefault(kind, Collections.emptyList());
    }

    public static void reset() {
        STATUS.clear();
        BUBBLES.clear();
        ASSET_LISTS.clear();
        serverKnown = false;
        allowUrlSkins = true;
        allowPlayerNameSkins = true;
        allowSkinUploads = true;
        allowCustomModels = true;
        allowModelUploads = false;
        othersCanInteract = false;
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        return o.has(key) ? o.get(key).getAsBoolean() : def;
    }

    private static int integer(JsonObject o, String key, int def) {
        return o.has(key) ? o.get(key).getAsInt() : def;
    }
}
