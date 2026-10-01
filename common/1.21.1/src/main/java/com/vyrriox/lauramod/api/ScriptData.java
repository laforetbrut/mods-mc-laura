package com.vyrriox.lauramod.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.vyrriox.lauramod.dialogue.DialogueSet;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Content added by scripts (KubeJS or other mods) through {@link LauraAPI}: gifts, desires,
 * cookbook meals and dialogues. Entries use the same format as the config files and are merged into
 * them every time the mod reloads, so they survive {@code /laura reload} and script reloads.
 *
 * @author vyrriox
 */
public final class ScriptData {
    /** "file.path.to.array" to the entries appended to that array (for example "gifts.foods.favorites"). */
    private static final Map<String, List<JsonElement>> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, List<JsonObject>> DIALOGUES = new LinkedHashMap<>();
    private static volatile boolean reloadRequested;

    private ScriptData() {
    }

    /** Forgets everything scripts added (called before scripts run again). */
    public static synchronized void clear() {
        ENTRIES.clear();
        DIALOGUES.clear();
    }

    public static synchronized void add(String path, JsonElement entry) {
        ENTRIES.computeIfAbsent(path, k -> new ArrayList<>()).add(entry);
    }

    public static synchronized void addDialogue(String locale, JsonObject json) {
        DIALOGUES.computeIfAbsent(locale, k -> new ArrayList<>()).add(json);
    }

    /** Appends the script entries of a config file ("gifts", "desires", "recipes") into its parsed root. */
    public static synchronized void extend(String file, JsonObject root) {
        String prefix = file + ".";
        for (Map.Entry<String, List<JsonElement>> e : ENTRIES.entrySet()) {
            if (!e.getKey().startsWith(prefix)) {
                continue;
            }
            String[] path = e.getKey().substring(prefix.length()).split("\\.");
            JsonObject parent = root;
            for (int i = 0; i < path.length - 1; i++) {
                if (!parent.has(path[i]) || !parent.get(path[i]).isJsonObject()) {
                    parent.add(path[i], new JsonObject());
                }
                parent = parent.getAsJsonObject(path[i]);
            }
            String last = path[path.length - 1];
            if (!parent.has(last) || !parent.get(last).isJsonArray()) {
                parent.add(last, new JsonArray());
            }
            JsonArray array = parent.getAsJsonArray(last);
            for (JsonElement entry : e.getValue()) {
                array.add(entry.deepCopy());
            }
        }
    }

    /** Merges script dialogues into the loaded dialogue sets. */
    public static synchronized void mergeDialogues(Map<String, DialogueSet> loaded) {
        for (Map.Entry<String, List<JsonObject>> e : DIALOGUES.entrySet()) {
            for (JsonObject json : e.getValue()) {
                loaded.computeIfAbsent(e.getKey(), DialogueSet::new).merge(json, false);
            }
        }
    }

    public static void requestReload() {
        reloadRequested = true;
    }

    /** True once after a reload was requested. */
    public static boolean consumeReloadRequest() {
        boolean r = reloadRequested;
        reloadRequested = false;
        return r;
    }

    /**
     * Converts a value coming from a script (JSON text, maps, lists, numbers, strings, booleans) to
     * JSON. Script engines give their own map and list types, all of which implement
     * {@link Map} and {@link List}.
     */
    public static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JsonElement json) {
            return json;
        }
        if (value instanceof CharSequence text) {
            String s = text.toString().trim();
            if (s.startsWith("{") || s.startsWith("[")) {
                try {
                    return JsonParser.parseReader(new JsonReader(new StringReader(s)));
                } catch (RuntimeException e) {
                    return new JsonPrimitive(text.toString());
                }
            }
            return new JsonPrimitive(text.toString());
        }
        if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return new JsonPrimitive((long) d);
            }
            return new JsonPrimitive(n);
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject o = new JsonObject();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                o.add(String.valueOf(e.getKey()), toJson(e.getValue()));
            }
            return o;
        }
        if (value instanceof Iterable<?> list) {
            JsonArray a = new JsonArray();
            for (Object item : list) {
                a.add(toJson(item));
            }
            return a;
        }
        if (value instanceof Object[] array) {
            JsonArray a = new JsonArray();
            for (Object item : array) {
                a.add(toJson(item));
            }
            return a;
        }
        return new JsonPrimitive(value.toString());
    }
}
