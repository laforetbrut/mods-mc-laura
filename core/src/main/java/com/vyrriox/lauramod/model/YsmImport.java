package com.vyrriox.lauramod.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Reads models made for Yes Steve Model in their open folder form (plain Bedrock geometry, Bedrock
 * animations and PNG textures) and turns them into the files Laura already understands: one
 * {@code .geo.json}, one {@code .animation.json} and one {@code .png}.
 *
 * <p>Two layouts exist. Version 2 has a {@code ysm.json} at the root of the folder that names every
 * file ({@code models/main.json}, {@code animations/main.animation.json}, {@code textures/*.png}).
 * Version 1 has no manifest: {@code main.json}, {@code main.animation.json},
 * {@code extra.animation.json} and the textures sit side by side. Encrypted {@code .ysm} files are
 * never read: they are made to be used by Yes Steve Model only.
 *
 * <p>Paths are relative to the model folder, with {@code /} as separator.
 *
 * @author vyrriox
 */
public final class YsmImport {
    /** Scale Yes Steve Model applies when the model does not set one. */
    public static final float DEFAULT_SCALE = 0.7F;

    /** Files Laura can use, the scale to render them at, and the model name from its metadata. */
    public record Result(Map<String, byte[]> files, float heightScale, float widthScale, String displayName) {
    }

    private YsmImport() {
    }

    /** True when these paths hold a model in the YSM folder layout (1.x or 2.x). */
    public static boolean isYsm(Collection<String> paths) {
        return find(paths, "ysm.json") != null
                || find(paths, "main.json") != null && (find(paths, "main.animation.json") != null || find(paths, "arm.json") != null);
    }

    /** True when the folder only holds encrypted .ysm files and nothing Laura can read instead. */
    public static boolean onlyEncrypted(Collection<String> paths) {
        boolean ysm = false;
        for (String p : paths) {
            String n = p.toLowerCase(Locale.ROOT);
            if (n.endsWith(".ysm")) {
                ysm = true;
            } else if (n.endsWith(".bbmodel") || n.endsWith(".geo.json")) {
                return false;
            }
        }
        return ysm && !isYsm(paths);
    }

    /** Message for a folder that {@link #onlyEncrypted} refused. */
    public static String encryptedMessage(String name) {
        return "Model " + name + " is an encrypted .ysm file, which cannot be used: export it as a folder (main.json, animations, textures) instead";
    }

    /**
     * The files of a YSM folder that Laura needs: the main geometry, the main and extra animations,
     * the manifest or info file and the default texture. Arm models, other animations, sounds and
     * the other textures are left out.
     *
     * @param paths every relative path of the folder
     * @param read  reads one of these paths (only the manifest is read)
     */
    public static List<String> neededFiles(Collection<String> paths, Function<String, byte[]> read) {
        Set<String> out = new LinkedHashSet<>();
        String manifest = find(paths, "ysm.json");
        if (manifest != null) {
            out.add(manifest);
            JsonObject player = player(json(read.apply(manifest)));
            addIfPresent(out, paths, string(obj(player, "model"), "main"));
            JsonObject animation = obj(player, "animation");
            addIfPresent(out, paths, string(animation, "main"));
            addIfPresent(out, paths, string(animation, "extra"));
            addIfPresent(out, paths, defaultTexture2(json(read.apply(manifest)), player, paths));
        } else if (isYsm(paths)) {
            out.add(find(paths, "main.json"));
            addIfPresent(out, paths, "main.animation.json");
            addIfPresent(out, paths, "extra.animation.json");
            addIfPresent(out, paths, "info.json");
            addIfPresent(out, paths, defaultTexture1(paths));
        }
        return new ArrayList<>(out);
    }

    /**
     * Converts a YSM folder into {@code <name>.geo.json}, {@code <name>.animation.json} and
     * {@code <name>.png} (file names in lower case). Returns null when the files are not a YSM model.
     *
     * @throws IllegalArgumentException when the manifest is broken or the geometry is missing
     */
    public static Result convert(String name, Map<String, byte[]> files) {
        Collection<String> paths = files.keySet();
        if (!isYsm(paths)) {
            return null;
        }
        String stem = stem(name);
        String geometry;
        List<String> animations = new ArrayList<>();
        String texture;
        float heightScale = DEFAULT_SCALE;
        float widthScale = DEFAULT_SCALE;
        String displayName = null;
        String manifest = find(paths, "ysm.json");
        if (manifest != null) {
            JsonObject root = json(files.get(manifest));
            if (root == null) {
                throw new IllegalArgumentException("ysm.json is not valid JSON");
            }
            JsonObject player = player(root);
            geometry = find(paths, string(obj(player, "model"), "main"));
            JsonObject animation = obj(player, "animation");
            animations.add(find(paths, string(animation, "main")));
            animations.add(find(paths, string(animation, "extra")));
            texture = find(paths, defaultTexture2(root, player, paths));
            JsonObject properties = obj(root, "properties");
            heightScale = scale(properties, "height_scale");
            widthScale = scale(properties, "width_scale");
            displayName = string(obj(root, "metadata"), "name");
        } else {
            geometry = find(paths, "main.json");
            animations.add(find(paths, "main.animation.json"));
            animations.add(find(paths, "extra.animation.json"));
            texture = find(paths, defaultTexture1(paths));
        }
        if (geometry == null) {
            throw new IllegalArgumentException("the YSM model has no main geometry file");
        }
        Map<String, byte[]> out = new LinkedHashMap<>();
        out.put(stem + ".geo.json", files.get(geometry));
        byte[] merged = mergeAnimations(animations.stream().filter(p -> p != null).map(files::get).toList());
        if (merged != null) {
            out.put(stem + ".animation.json", merged);
        }
        if (texture != null) {
            out.put(stem + ".png", files.get(texture));
        }
        if (displayName == null) {
            displayName = extraInfoName(files.get(geometry));
        }
        return new Result(out, heightScale, widthScale, displayName);
    }

    /**
     * One Bedrock animation file holding the animations of all the given files. The first file
     * wins when two hold the same animation name. Null when none of them could be read.
     */
    public static byte[] mergeAnimations(List<byte[]> sources) {
        JsonObject all = new JsonObject();
        boolean any = false;
        for (byte[] bytes : sources) {
            JsonObject root = json(bytes);
            JsonObject anims = obj(root, "animations");
            if (anims == null) {
                continue;
            }
            any = true;
            for (Map.Entry<String, JsonElement> e : anims.entrySet()) {
                if (!all.has(e.getKey())) {
                    all.add(e.getKey(), e.getValue());
                }
            }
        }
        if (!any) {
            return null;
        }
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", all);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String stem(String name) {
        String n = name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return n.isEmpty() ? "model" : n;
    }

    private static float scale(JsonObject properties, String key) {
        if (properties != null && properties.has(key) && properties.get(key).isJsonPrimitive()) {
            try {
                float v = properties.get(key).getAsFloat();
                if (Float.isFinite(v) && v > 0) {
                    return Math.max(0.05F, Math.min(4F, v));
                }
            } catch (RuntimeException ignored) {
                // Not a number: default scale.
            }
        }
        return DEFAULT_SCALE;
    }

    /** Version 1: default.png when present, otherwise the first PNG of the folder by name. */
    private static String defaultTexture1(Collection<String> paths) {
        String preferred = find(paths, "default.png");
        if (preferred != null) {
            return preferred;
        }
        TreeSet<String> pngs = new TreeSet<>();
        for (String p : paths) {
            if (p.toLowerCase(Locale.ROOT).endsWith(".png") && !p.contains("/")) {
                pngs.add(p);
            }
        }
        return pngs.isEmpty() ? null : pngs.first();
    }

    /** Version 2: the texture named by properties.default_texture ("default"), otherwise the first one listed. */
    private static String defaultTexture2(JsonObject root, JsonObject player, Collection<String> paths) {
        String wanted = string(obj(root, "properties"), "default_texture");
        if (wanted == null) {
            wanted = "default";
        }
        List<String> textures = new ArrayList<>();
        if (player != null && player.has("texture") && player.get("texture").isJsonArray()) {
            for (JsonElement e : player.getAsJsonArray("texture")) {
                String path = null;
                if (e.isJsonPrimitive()) {
                    path = e.getAsString();
                } else if (e.isJsonObject()) {
                    path = string(e.getAsJsonObject(), "uv");
                }
                if (path != null && find(paths, path) != null) {
                    textures.add(path);
                }
            }
        }
        for (String t : textures) {
            String base = t.substring(t.lastIndexOf('/') + 1);
            if (base.toLowerCase(Locale.ROOT).endsWith(".png")) {
                base = base.substring(0, base.length() - 4);
            }
            if (base.equalsIgnoreCase(wanted)) {
                return t;
            }
        }
        return textures.isEmpty() ? null : textures.get(0);
    }

    private static String extraInfoName(byte[] geometry) {
        JsonObject root = json(geometry);
        if (root == null || !root.has("minecraft:geometry") || !root.get("minecraft:geometry").isJsonArray()) {
            return null;
        }
        for (JsonElement g : root.getAsJsonArray("minecraft:geometry")) {
            if (g.isJsonObject()) {
                String n = string(obj(obj(g.getAsJsonObject(), "description"), "ysm_extra_info"), "name");
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    private static JsonObject player(JsonObject root) {
        return obj(obj(root, "files"), "player");
    }

    private static void addIfPresent(Set<String> out, Collection<String> paths, String path) {
        String found = find(paths, path);
        if (found != null) {
            out.add(found);
        }
    }

    /** The path as listed, matched exactly first, then without case (folders copied from Windows). */
    private static String find(Collection<String> paths, String wanted) {
        if (wanted == null) {
            return null;
        }
        String w = wanted.replace('\\', '/');
        while (w.startsWith("./")) {
            w = w.substring(2);
        }
        if (w.isEmpty() || w.startsWith("/") || w.contains("..")) {
            return null;
        }
        if (paths.contains(w)) {
            return w;
        }
        for (String p : paths) {
            if (p.equalsIgnoreCase(w)) {
                return p;
            }
        }
        return null;
    }

    private static JsonObject json(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) {
                text = text.substring(1);
            }
            JsonElement e = JsonParser.parseString(text);
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : null;
    }

    private static String string(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return null;
        }
        return o.get(key).getAsString();
    }
}
