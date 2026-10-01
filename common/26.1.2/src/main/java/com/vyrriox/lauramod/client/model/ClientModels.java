package com.vyrriox.lauramod.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.ModelParser;
import com.vyrriox.lauramod.skin.AssetKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private static final Map<String, Optional<Loaded>> CACHE = new HashMap<>();
    private static final Map<String, Boolean> REQUESTED = new HashMap<>();
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
            Path cachedZip = cacheDir().resolve(sha1 + ".zip");
            if (!sha1.isEmpty() && Files.isRegularFile(cachedZip)) {
                try {
                    Loaded loaded = fromZip(name, Files.readAllBytes(cachedZip));
                    CACHE.put(ref, Optional.ofNullable(loaded));
                    return loaded;
                } catch (IOException e) {
                    LauraMod.LOGGER.debug("Cached model {} unreadable", name);
                }
            }
            if (!REQUESTED.containsKey(ref)) {
                REQUESTED.put(ref, true);
                LauraClientNetwork.requestAsset(AssetKind.MODEL, name);
            }
            return null;
        }
        String name = ref.startsWith("pack:") ? ref.substring(5) : ref;
        Loaded loaded = fromResourcePacks(name);
        if (loaded == null) {
            loaded = fromFolder(LauraMod.configDir().resolve("models"), name);
        }
        CACHE.put(ref, Optional.ofNullable(loaded));
        return loaded;
    }

    public static void onServerModelReceived(String name, String sha1, byte[] zip) {
        try {
            Files.createDirectories(cacheDir());
            Files.write(cacheDir().resolve(sha1 + ".zip"), zip);
        } catch (IOException e) {
            LauraMod.LOGGER.debug("Could not cache model {}", name);
        }
        String ref = "server:" + name + "#" + sha1;
        try {
            CACHE.put(ref, Optional.ofNullable(fromZip(name, zip)));
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.warn("Model {} from the server is invalid: {}", name, e.getMessage());
            CACHE.put(ref, Optional.empty());
        }
    }

    public static void onServerModelMissing(String name) {
        REQUESTED.keySet().removeIf(k -> k.startsWith("server:" + name + "#"));
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lauramod").resolve("cache").resolve("models");
    }

    private static Loaded fromZip(String name, byte[] zip) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            long total = 0;
            while ((entry = in.getNextEntry()) != null) {
                byte[] data = in.readAllBytes();
                total += data.length;
                if (total > 64L * 1024 * 1024) {
                    throw new IOException("model too large");
                }
                String file = entry.getName();
                file = file.substring(file.lastIndexOf('/') + 1);
                files.put(file.toLowerCase(Locale.ROOT), data);
            }
        }
        return fromFiles(name, files);
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
        ModelData data = ModelParser.parse(main, new String(files.get(main), StandardCharsets.UTF_8), f -> files.get(f.toLowerCase(Locale.ROOT)));
        if (main.endsWith(".geo.json") && data.animations.isEmpty()) {
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                if (e.getKey().endsWith(".animation.json")) {
                    ModelParser.parseAnimations(data, new String(e.getValue(), StandardCharsets.UTF_8));
                }
            }
        }
        if (data.textures.isEmpty()) {
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                if (e.getKey().endsWith(".png")) {
                    data.textures.add(e.getValue());
                    break;
                }
            }
        }
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
