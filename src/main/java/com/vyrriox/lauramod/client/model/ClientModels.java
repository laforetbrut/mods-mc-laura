package com.vyrriox.lauramod.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.ModelParser;
import com.vyrriox.lauramod.skin.AssetCache;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import com.vyrriox.lauramod.util.CacheFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Custom Blockbench models on the client: from the server ({@code server:name#sha1}), from resource
 * packs ({@code assets/<namespace>/laura_models/}) or from the local {@code config/lauramod/models}
 * folder. Parsed once, textures registered once.
 *
 * @author vyrriox
 */
public final class ClientModels {
    /** A model ready to render. */
    public record Loaded(ModelData data, List<Identifier> textures) {
    }

    /** Largest model once unpacked. */
    private static final long MAX_UNPACKED_BYTES = 64L * 1024 * 1024;
    /** A request the server did not answer (or answered "missing") is sent again after this long. */
    private static final long RETRY_MS = 10_000L;

    private static final Map<String, Optional<Loaded>> CACHE = new HashMap<>();
    /** Server references asked for, with the time of the last request. */
    private static final Map<String, Long> REQUESTED = new HashMap<>();
    private static int counter;

    private ClientModels() {
    }

    /** The model for a synced reference, or null while loading, when missing or disabled. */
    public static Loaded get(String ref) {
        if (ref == null || ref.isEmpty() || !LauraClientConfig.customModels.get()) {
            return null;
        }
        Optional<Loaded> cached = CACHE.get(ref);
        if (cached != null) {
            return cached.orElse(null);
        }
        if (ref.startsWith("server:")) {
            String rest = ref.substring(7);
            int hash = rest.lastIndexOf('#');
            String name = hash > 0 ? rest.substring(0, hash) : rest;
            String sha1 = hash > 0 ? rest.substring(hash + 1) : "";
            // The reference is written by the server: its name and hash are checked before they
            // are used for anything, and a hash that is not a SHA-1 never becomes a file name.
            if (!ServerAssetStore.isSafeName(name)) {
                CACHE.put(ref, Optional.empty());
                return null;
            }
            Long asked = REQUESTED.get(ref);
            long now = System.currentTimeMillis();
            if (asked == null) {
                Path cachedZip = AssetCache.file(cacheDir(), sha1, ".zip");
                if (cachedZip != null && Files.isRegularFile(cachedZip)) {
                    Map<String, byte[]> files = null;
                    try {
                        files = AssetCache.unzip(Files.readAllBytes(cachedZip), MAX_UNPACKED_BYTES);
                    } catch (IOException | RuntimeException e) {
                        LauraMod.LOGGER.debug("Cached model {} unreadable", name);
                    }
                    // A file that is not the model its name promises is ignored and downloaded again.
                    if (files != null && AssetCache.matchesModel(sha1, files)) {
                        try {
                            Loaded loaded = fromFiles(name, lowerCaseNames(files));
                            CACHE.put(ref, Optional.ofNullable(loaded));
                            CacheFiles.touch(cachedZip);
                            return loaded;
                        } catch (IOException | RuntimeException e) {
                            // The cached file is the one the server sent: refused once, refused again, not asked for again.
                            LauraMod.LOGGER.warn("Model {} from the server is invalid: {}", name, e.getMessage());
                            CACHE.put(ref, Optional.empty());
                            return null;
                        }
                    }
                }
            }
            if (asked == null || now - asked > RETRY_MS) {
                REQUESTED.put(ref, now);
                LauraClientNetwork.requestAsset(AssetKind.MODEL, name);
            }
            return null;
        }
        String name = ref.startsWith("pack:") ? ref.substring(5) : ref;
        Loaded loaded = null;
        // The name becomes a path in the models folder: only plain names, never "..", a drive or a root.
        if (ServerAssetStore.isSafeName(name)) {
            loaded = fromResourcePacks(name);
            if (loaded == null) {
                loaded = fromFolder(LauraMod.configDir().resolve("models"), name);
            }
        }
        CACHE.put(ref, Optional.ofNullable(loaded));
        return loaded;
    }

    /** True while a model with this name was asked from the server and has not arrived. */
    public static boolean isWaitingFor(String name) {
        String prefix = "server:" + name + "#";
        for (String ref : REQUESTED.keySet()) {
            if (ref.startsWith(prefix) && !CACHE.containsKey(ref)) {
                return true;
            }
        }
        return false;
    }

    /** A model the server sent. The caller checked that it was asked for and that the hash is a SHA-1. */
    public static void onServerModelReceived(String name, String sha1, byte[] zip) {
        String ref = "server:" + name + "#" + sha1;
        try {
            Map<String, byte[]> files = AssetCache.unzip(zip, MAX_UNPACKED_BYTES);
            // Kept on disk only when the content is what the hash says: a server cannot plant a
            // file that another server's model would later be read from.
            Path target = AssetCache.matchesModel(sha1, files) ? AssetCache.file(cacheDir(), sha1, ".zip") : null;
            if (target != null) {
                try {
                    Files.createDirectories(target.getParent());
                    Files.write(target, zip);
                } catch (IOException e) {
                    LauraMod.LOGGER.debug("Could not cache model {}", name);
                }
            }
            CACHE.put(ref, Optional.ofNullable(fromFiles(name, lowerCaseNames(files))));
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.warn("Model {} from the server is invalid: {}", name, e.getMessage());
            CACHE.put(ref, Optional.empty());
        }
        // References to another version of this file will not be answered: stop asking.
        String prefix = "server:" + name + "#";
        for (String asked : REQUESTED.keySet()) {
            if (asked.startsWith(prefix)) {
                CACHE.putIfAbsent(asked, Optional.empty());
            }
        }
    }

    /** The server has no such model (or sent something unusable): asked again after {@link #RETRY_MS}. */
    public static void onServerModelMissing(String name) {
        String prefix = "server:" + name + "#";
        long now = System.currentTimeMillis();
        REQUESTED.replaceAll((ref, asked) -> ref.startsWith(prefix) ? Long.valueOf(now) : asked);
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lauramod").resolve("cache").resolve("models");
    }

    /** Files of an archive by lower case file name, without any folder part. */
    private static Map<String, byte[]> lowerCaseNames(Map<String, byte[]> archive) {
        Map<String, byte[]> files = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : archive.entrySet()) {
            String file = entry.getKey().replace('\\', '/');
            file = file.substring(file.lastIndexOf('/') + 1);
            files.put(file.toLowerCase(Locale.ROOT), entry.getValue());
        }
        return files;
    }

    private static Loaded fromFolder(Path root, String name) {
        try {
            Path direct = root.resolve(name + ".bbmodel");
            Path geo = root.resolve(name + ".geo.json");
            Path dir = root.resolve(name);
            Map<String, byte[]> files = new HashMap<>();
            if (Files.isRegularFile(direct)) {
                files.put(direct.getFileName().toString().toLowerCase(Locale.ROOT), Files.readAllBytes(direct));
            } else if (Files.isRegularFile(geo)) {
                String stem = geo.getFileName().toString();
                stem = stem.substring(0, stem.length() - ".geo.json".length());
                for (String suffix : new String[]{".geo.json", ".animation.json", ".png"}) {
                    Path p = root.resolve(stem + suffix);
                    if (Files.isRegularFile(p)) {
                        files.put(p.getFileName().toString().toLowerCase(Locale.ROOT), Files.readAllBytes(p));
                    }
                }
            } else if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    for (Path p : stream.toList()) {
                        if (Files.isRegularFile(p)) {
                            files.put(p.getFileName().toString().toLowerCase(Locale.ROOT), Files.readAllBytes(p));
                        }
                    }
                }
            } else {
                return null;
            }
            return fromFiles(name, files);
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.warn("Could not load model {}: {}", name, e.getMessage());
            return null;
        }
    }

    private static Loaded fromResourcePacks(String name) {
        var manager = Minecraft.getInstance().getResourceManager();
        for (String namespace : manager.getNamespaces()) {
            Map<String, byte[]> files = new HashMap<>();
            for (String suffix : new String[]{".bbmodel", ".geo.json", ".animation.json", ".png"}) {
                Identifier id = Identifier.tryBuild(namespace, "laura_models/" + name + suffix);
                if (id == null) {
                    continue;
                }
                Optional<Resource> resource = manager.getResource(id);
                if (resource.isPresent()) {
                    try (InputStream in = resource.get().open()) {
                        files.put((name + suffix).toLowerCase(Locale.ROOT), in.readAllBytes());
                    } catch (IOException ignored) {
                        // Skip unreadable file.
                    }
                }
            }
            if (!files.isEmpty()) {
                try {
                    return fromFiles(name, files);
                } catch (IOException | RuntimeException e) {
                    LauraMod.LOGGER.warn("Model {} from resource pack {} is invalid: {}", name, namespace, e.getMessage());
                }
            }
        }
        return null;
    }

    private static Loaded fromFiles(String name, Map<String, byte[]> files) throws IOException {
        String main = null;
        for (String file : files.keySet()) {
            if (file.endsWith(".bbmodel")) {
                main = file;
                break;
            }
        }
        if (main == null) {
            for (String file : files.keySet()) {
                if (file.endsWith(".geo.json")) {
                    main = file;
                    break;
                }
            }
        }
        if (main == null) {
            throw new IOException("no .bbmodel or .geo.json file");
        }
        ModelData data;
        try {
            // The file may come from a server or a pack: nothing it holds may crash the game.
            data = ModelParser.parseChecked(main, new String(files.get(main), StandardCharsets.UTF_8), f -> files.get(f.toLowerCase(Locale.ROOT)));
            if (main.endsWith(".geo.json") && data.animations.isEmpty()) {
                for (Map.Entry<String, byte[]> e : files.entrySet()) {
                    if (e.getKey().endsWith(".animation.json")) {
                        ModelParser.parseAnimationsChecked(data, new String(e.getValue(), StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (ModelParser.InvalidModelException e) {
            throw new IOException(e.getMessage(), e);
        }
        if (data.textures.isEmpty()) {
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                if (e.getKey().endsWith(".png")) {
                    data.textures.add(e.getValue());
                    break;
                }
            }
        }
        // Again here: the texture found next to the model was not part of the parser's check, and
        // models of the config folder and of resource packs never went through the server.
        ModelParser.checkTextures(data.textures);
        List<Identifier> textures = new ArrayList<>();
        for (byte[] png : data.textures) {
            NativeImage image = NativeImage.read(png);
            Identifier location = LauraMod.id("custom_models/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, image));
            textures.add(location);
        }
        if (textures.isEmpty()) {
            textures.add(LauraMod.id("textures/entity/laura/laura.png"));
        }
        LauraMod.LOGGER.info("Loaded Laura model {} ({} bones, {} cubes, {} animations)", name, data.bones.size(), data.cubeCount(), data.animations.size());
        return new Loaded(data, textures);
    }

    /** Local models (config folder) for the model picker. */
    public static List<String> localModelNames() {
        List<String> out = new ArrayList<>();
        Path root = LauraMod.configDir().resolve("models");
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (var stream = Files.list(root)) {
            for (Path p : stream.toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith(".bbmodel")) {
                    out.add(n.substring(0, n.length() - 8));
                } else if (n.endsWith(".geo.json")) {
                    out.add(n.substring(0, n.length() - 9));
                } else if (Files.isDirectory(p) && !n.equals("uploads")) {
                    out.add(n);
                }
            }
        } catch (IOException ignored) {
            // Empty list.
        }
        return out;
    }

    public static void clear() {
        for (Optional<Loaded> loaded : CACHE.values()) {
            loaded.ifPresent(l -> l.textures().forEach(t -> {
                if (t.getPath().startsWith("custom_models/")) {
                    Minecraft.getInstance().getTextureManager().release(t);
                }
            }));
        }
        CACHE.clear();
        REQUESTED.clear();
    }
}
