package com.vyrriox.lauramod.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import com.vyrriox.lauramod.skin.SkinRef;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    private record Entry(State state, ResourceLocation texture) {
    }

    private static final Map<String, Entry> CACHE = new HashMap<>();
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
            case SERVER -> lookup(ref.cacheKey(), () -> loadServerSkin(ref));
        };
    }

    private static ResourceLocation lookup(String key, Runnable loader) {
        Entry entry = CACHE.get(key);
        if (entry == null) {
            CACHE.put(key, new Entry(State.LOADING, null));
            loader.run();
            return DEFAULT;
        }
        return entry.state() == State.READY ? entry.texture() : DEFAULT;
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lauramod").resolve("cache").resolve("skins");
    }

    // ------------------------------------------------------------------ server hosted skins

    private static void loadServerSkin(SkinRef ref) {
        String key = ref.cacheKey();
        if (!ref.hash().isEmpty()) {
            Path cached = cacheDir().resolve(ref.hash() + ".png");
            if (Files.isRegularFile(cached)) {
                try {
                    register(key, Files.readAllBytes(cached));
                    return;
                } catch (IOException ignored) {
                    // Fall through to the network.
                }
            }
        }
        LauraClientNetwork.requestAsset(AssetKind.SKIN, ref.value());
    }

    public static void onServerSkinReceived(String name, String sha1, byte[] bytes) {
        if (!ServerAssetStore.isValidSkinPng(bytes)) {
            onServerSkinMissing(name);
            return;
        }
        try {
            Files.createDirectories(cacheDir());
            Files.write(cacheDir().resolve(sha1 + ".png"), bytes);
        } catch (IOException e) {
            LauraMod.LOGGER.debug("Could not cache skin {}: {}", name, e.getMessage());
        }
        register("server/" + name + "#" + sha1, bytes);
    }

    public static void onServerSkinMissing(String name) {
        CACHE.entrySet().removeIf(e -> e.getKey().startsWith("server/" + name + "#") && e.getValue().state() == State.LOADING);
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
                Minecraft.getInstance().execute(() -> CACHE.put(key, new Entry(State.FAILED, null)));
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
            if (ip.isLoopbackAddress() || ip.isSiteLocalAddress() || ip.isLinkLocalAddress() || ip.isAnyLocalAddress() || ip.isMulticastAddress()) {
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
            Entry previous = CACHE.put(key, new Entry(State.READY, location));
            if (previous != null && previous.texture() != null && !previous.texture().equals(location)) {
                Minecraft.getInstance().getTextureManager().release(previous.texture());
            }
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.info("Invalid skin image: {}", e.getMessage());
            CACHE.put(key, new Entry(State.FAILED, null));
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
        for (Entry entry : CACHE.values()) {
            if (entry.texture() != null) {
                Minecraft.getInstance().getTextureManager().release(entry.texture());
            }
        }
        CACHE.clear();
    }
}
