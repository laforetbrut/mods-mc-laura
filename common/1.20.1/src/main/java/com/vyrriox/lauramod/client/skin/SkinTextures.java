package com.vyrriox.lauramod.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.skin.AssetCache;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import com.vyrriox.lauramod.skin.SkinRef;
import com.vyrriox.lauramod.util.CacheFiles;
import com.vyrriox.lauramod.util.LruCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Turns Laura's skin reference into a texture: built-in textures, downloaded URLs (validated and
 * cached on disk) and files hosted by the server.
 *
 * @author vyrriox
 */
public final class SkinTextures {
    public static final ResourceLocation DEFAULT = LauraMod.id("textures/entity/laura/laura.png");

    private enum State {
        LOADING, READY, FAILED
    }

    /** {@code since} is when a server skin was last asked for (0 for everything else). */
    private record Entry(State state, ResourceLocation texture, long since) {
    }

    /** A request the server did not answer (or answered "missing") is sent again after this long. */
    private static final long RETRY_MS = 10_000L;
    /** Downloaded skins kept in memory at once (the ones in use are drawn every frame, so they stay). */
    private static final int MAX_TEXTURES = 32;

    private static final LruCache<String, Entry> CACHE = new LruCache<>(MAX_TEXTURES, SkinTextures::release);
    private static final ExecutorService DOWNLOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Laura skin downloader");
        t.setDaemon(true);
        return t;
    });
    private static int counter;

    private SkinTextures() {
    }

    public static ResourceLocation builtin(String name) {
        return LauraMod.id("textures/entity/laura/" + name + ".png");
    }

    /** The texture to use for this reference right now (the default one while loading). */
    public static ResourceLocation get(SkinRef ref) {
        return switch (ref.type()) {
            case BUILTIN -> builtin(ref.value());
            case PLAYER -> DEFAULT;
            case URL -> {
                if (!LauraClientConfig.remoteSkins.get()) {
                    yield DEFAULT;
                }
                yield lookup(ref.cacheKey(), () -> downloadUrl(ref.cacheKey(), ref.value()));
            }
            case SERVER -> {
                String key = ref.cacheKey();
                Entry waiting = CACHE.get(key);
                long now = System.currentTimeMillis();
                if (waiting != null && waiting.state() == State.LOADING && waiting.since() > 0 && now - waiting.since() > RETRY_MS) {
                    // No answer, or the file was missing: ask the server again.
                    CACHE.put(key, new Entry(State.LOADING, null, now));
                    loadServerSkin(ref);
                    yield DEFAULT;
                }
                yield lookup(key, now, () -> loadServerSkin(ref));
            }
        };
    }

    private static ResourceLocation lookup(String key, Runnable loader) {
        return lookup(key, 0, loader);
    }

    private static ResourceLocation lookup(String key, long since, Runnable loader) {
        Entry entry = CACHE.get(key);
        if (entry == null) {
            CACHE.put(key, new Entry(State.LOADING, null, since));
            loader.run();
            return DEFAULT;
        }
        return entry.state() == State.READY ? entry.texture() : DEFAULT;
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lauramod").resolve("cache").resolve("skins");
    }

    /** Frees the texture of an entry that leaves the cache. */
    private static void release(Entry entry) {
        if (entry.texture() != null) {
            Minecraft.getInstance().getTextureManager().release(entry.texture());
        }
    }

    // ------------------------------------------------------------------ server hosted skins

    private static void loadServerSkin(SkinRef ref) {
        String key = ref.cacheKey();
        // The reference is written by the server: a name that could be a path is never used, and a
        // hash that is not a SHA-1 never becomes a file name.
        if (!ServerAssetStore.isSafeName(ref.value())) {
            CACHE.put(key, new Entry(State.FAILED, null, 0));
            return;
        }
        Path cached = AssetCache.file(cacheDir(), ref.hash(), ".png");
        if (cached != null && Files.isRegularFile(cached)) {
            try {
                byte[] bytes = Files.readAllBytes(cached);
                // A file that is not the skin its name promises is ignored and downloaded again.
                if (AssetCache.matchesSkin(ref.hash(), bytes) && ServerAssetStore.isValidSkinPng(bytes)) {
                    register(key, bytes);
                    CacheFiles.touch(cached);
                    return;
                }
            } catch (IOException ignored) {
                // Fall through to the network.
            }
        }
        LauraClientNetwork.requestAsset(AssetKind.SKIN, ref.value());
    }

    /** True while a skin with this name was asked from the server and has not arrived. */
    public static boolean isWaitingFor(String name) {
        String prefix = "server/" + name + "#";
        return CACHE.anyMatch((key, entry) -> entry.state() == State.LOADING && key.startsWith(prefix));
    }

    /** A skin the server sent. The caller checked that it was asked for and that the hash is a SHA-1. */
    public static void onServerSkinReceived(String name, String sha1, byte[] bytes) {
        if (!ServerAssetStore.isValidSkinPng(bytes)) {
            onServerSkinMissing(name);
            return;
        }
        // Kept on disk only when the content is what the hash says: a server cannot plant a file
        // that another server's skin would later be read from.
        Path target = AssetCache.matchesSkin(sha1, bytes) ? AssetCache.file(cacheDir(), sha1, ".png") : null;
        if (target != null) {
            try {
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
            } catch (IOException e) {
                LauraMod.LOGGER.debug("Could not cache skin {}: {}", name, e.getMessage());
            }
        }
        String key = "server/" + name + "#" + sha1;
        register(key, bytes);
        // References to another version of this file will not be answered: stop waiting for them.
        String prefix = "server/" + name + "#";
        CACHE.replaceAll((k, e) -> e.state() == State.LOADING && k.startsWith(prefix) && !k.equals(key) ? new Entry(State.FAILED, null, 0) : e);
    }

    /** The server has no such skin (or sent something unusable): asked again after {@link #RETRY_MS}. */
    public static void onServerSkinMissing(String name) {
        String prefix = "server/" + name + "#";
        long now = System.currentTimeMillis();
        CACHE.replaceAll((k, e) -> e.state() == State.LOADING && k.startsWith(prefix) ? new Entry(State.LOADING, null, now) : e);
    }

    // ------------------------------------------------------------------ URL skins

    private static void downloadUrl(String key, String url) {
        int limit = LauraClientConfig.maxSkinDownloadKb.getInt() * 1024;
        Path cached = cacheDir().resolve(ServerAssetStore.sha1(url.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".png");
        DOWNLOADER.execute(() -> {
            try {
                byte[] bytes;
                if (Files.isRegularFile(cached)) {
                    bytes = Files.readAllBytes(cached);
                    CacheFiles.touch(cached);
                } else {
                    bytes = fetch(url, limit, 0);
                    if (!ServerAssetStore.isValidSkinPng(bytes)) {
                        throw new IOException("not a skin");
                    }
                    Files.createDirectories(cached.getParent());
                    Files.write(cached, bytes);
                }
                byte[] result = bytes;
                Minecraft.getInstance().execute(() -> register(key, result));
            } catch (Exception e) {
                LauraMod.LOGGER.info("Could not download skin {}: {}", url, e.getMessage());
                Minecraft.getInstance().execute(() -> CACHE.put(key, new Entry(State.FAILED, null, 0)));
            }
        });
    }

    /** Downloads with size limit, whitelist check and private address protection on every hop. */
    private static byte[] fetch(String address, int limit, int redirects) throws IOException {
        URI uri = URI.create(address);
        if (!SkinRef.isAllowedUrl(address, ClientState.serverKnown ? ClientState.urlWhitelist : java.util.List.of("*"))) {
            throw new IOException("domain not allowed");
        }
        for (InetAddress ip : InetAddress.getAllByName(uri.getHost())) {
            if (SkinRef.isLocalAddress(ip)) {
                throw new IOException("private address refused");
            }
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "lauramod");
        int code = conn.getResponseCode();
        if (code >= 300 && code < 400) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null || redirects >= 3) {
                throw new IOException("bad redirect");
            }
            return fetch(uri.resolve(location).toString(), limit, redirects + 1);
        }
        if (code != 200) {
            throw new IOException("HTTP " + code);
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int n;
            while ((n = in.read(buffer)) > 0) {
                total += n;
                if (total > limit) {
                    throw new IOException("too big");
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------ texture creation

    private static void register(String key, byte[] png) {
        try {
            NativeImage image = normalize(NativeImage.read(png));
            ResourceLocation location = LauraMod.id("skins/" + (counter++) + "_" + Integer.toHexString(key.hashCode()).toLowerCase(Locale.ROOT));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            Entry previous = CACHE.put(key, new Entry(State.READY, location, 0));
            if (previous != null && previous.texture() != null && !previous.texture().equals(location)) {
                Minecraft.getInstance().getTextureManager().release(previous.texture());
            }
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.info("Invalid skin image: {}", e.getMessage());
            CACHE.put(key, new Entry(State.FAILED, null, 0));
        }
    }

    /** Converts legacy 64x32 skins to the 64x64 layout, like the game does for players. */
    static NativeImage normalize(NativeImage in) {
        int width = in.getWidth();
        int height = in.getHeight();
        if (height * 2 != width) {
            return in;
        }
        int s = width / 64;
        NativeImage out = new NativeImage(width, width, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setPixelRGBA(x, y, in.getPixelRGBA(x, y));
            }
        }
        in.close();
        out.fillRect(0, 32 * s, 64 * s, 32 * s, 0);
        int[][] copies = {
                {4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4}, {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12},
                {8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12}, {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4},
                {40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12}};
        for (int[] c : copies) {
            out.copyRect(c[0] * s, c[1] * s, c[2] * s, c[3] * s, c[4] * s, c[5] * s, true, false);
        }
        opaque(out, 0, 0, 32 * s, 16 * s);
        opaque(out, 0, 16 * s, 64 * s, 32 * s);
        opaque(out, 16 * s, 48 * s, 48 * s, 64 * s);
        return out;
    }

    private static void opaque(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }

    /** Forgets every downloaded texture (disconnect). */
    public static void clear() {
        CACHE.clear();
    }
}
