package com.vyrriox.lauramod.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vyrriox.lauramod.LauraMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Finds the skin of a Minecraft account from its name, through the public Mojang API. Found skins
 * are remembered for an hour and unknown names for a few minutes, in a bounded cache, and only a
 * couple of lookups run at once, to stay far below the API rate limits.
 *
 * @author vyrriox
 */
public final class MojangSkinResolver {
    public record Result(String name, String url, boolean slim) {
    }

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    /** How long a found skin is remembered. */
    private static final long CACHE_MS = 60 * 60 * 1000L;
    /** How long "no such account" is remembered. Errors and rate limits are never remembered. */
    private static final long NOT_FOUND_MS = 5 * 60 * 1000L;
    /** Names remembered at once; the least recently used ones are forgotten beyond that. */
    public static final int CACHE_SIZE = 256;
    /** Lookups running at the same time, whoever asks. */
    private static final int MAX_LOOKUPS = 2;
    private static final Object LOCK = new Object();
    private static final Map<String, Cached> CACHE = new LinkedHashMap<>(64, 0.75F, true);
    private static final Map<String, CompletableFuture<Optional<Result>>> IN_FLIGHT = new HashMap<>();
    private static volatile HttpClient client;

    private MojangSkinResolver() {
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** The cached answer for a name, or null. Expired answers are removed on the way. Call with the lock held. */
    private static Cached cached(String key) {
        Cached cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() >= cached.expires) {
            CACHE.remove(key);
            return null;
        }
        return cached;
    }

    /** True when a lookup for this name would be refused right now: too many are already running. */
    public static boolean isBusy(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        synchronized (LOCK) {
            return cached(key) == null && !IN_FLIGHT.containsKey(key) && IN_FLIGHT.size() >= MAX_LOOKUPS;
        }
    }

    /** Stores an answer as if it came from the API (also used by the self tests, which never go online). */
    public static void remember(String name, Optional<Result> result, long ttlMs) {
        synchronized (LOCK) {
            CACHE.put(name.toLowerCase(Locale.ROOT), new Cached(result, System.currentTimeMillis() + ttlMs));
            Iterator<String> oldest = CACHE.keySet().iterator();
            while (CACHE.size() > CACHE_SIZE && oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
    }

    /** Number of names remembered. */
    public static int cachedCount() {
        synchronized (LOCK) {
            return CACHE.size();
        }
    }

    /**
     * The skin of an account. One lookup runs per name (a second request for the same name waits for
     * the first), at most {@link #MAX_LOOKUPS} run at once: beyond that the answer is empty, see
     * {@link #isBusy}.
     */
    public static CompletableFuture<Optional<Result>> resolve(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        CompletableFuture<Optional<Result>> shared;
        synchronized (LOCK) {
            Cached cached = cached(key);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached.result);
            }
            CompletableFuture<Optional<Result>> running = IN_FLIGHT.get(key);
            if (running != null) {
                return running;
            }
            if (IN_FLIGHT.size() >= MAX_LOOKUPS) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            shared = new CompletableFuture<>();
            IN_FLIGHT.put(key, shared);
        }
        CompletableFuture<Answer> lookup;
        try {
            lookup = lookup(name);
        } catch (RuntimeException e) {
            lookup = CompletableFuture.failedFuture(e);
        }
        lookup.whenComplete((answer, error) -> {
            if (error != null) {
                LauraMod.LOGGER.warn("Could not resolve the skin of {}: {}", name, error.getMessage());
            }
            Optional<Result> result = answer == null ? Optional.empty() : answer.result();
            synchronized (LOCK) {
                IN_FLIGHT.remove(key);
            }
            if (answer != null && answer.ttlMs() > 0) {
                remember(key, result, answer.ttlMs());
            }
            shared.complete(result);
        });
        return shared;
    }

    /** What a lookup found and how long it may be remembered (0: not at all). */
    private record Answer(Optional<Result> result, long ttlMs) {
    }

    private static CompletableFuture<Answer> lookup(String name) {
        HttpClient http = client();
        HttpRequest profileRequest = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                .timeout(Duration.ofSeconds(10)).header("User-Agent", "lauramod").GET().build();
        return http.sendAsync(profileRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    int status = response.statusCode();
                    if (status == 204 || status == 404) {
                        // No such account: a real answer, remembered for a short time.
                        return CompletableFuture.completedFuture(new Answer(Optional.empty(), NOT_FOUND_MS));
                    }
                    if (status != 200) {
                        // Rate limit (429) or server error: not an answer about the account.
                        LauraMod.LOGGER.warn("Could not resolve the skin of {}: HTTP {}", name, status);
                        return CompletableFuture.completedFuture(new Answer(Optional.empty(), 0));
                    }
                    JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                    String id = profile.get("id").getAsString();
                    String realName = profile.has("name") ? profile.get("name").getAsString() : name;
                    HttpRequest texturesRequest = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id))
                            .timeout(Duration.ofSeconds(10)).header("User-Agent", "lauramod").GET().build();
                    return http.sendAsync(texturesRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                            .thenApply(r -> {
                                if (r.statusCode() != 200) {
                                    LauraMod.LOGGER.warn("Could not resolve the skin of {}: HTTP {}", name, r.statusCode());
                                    return new Answer(Optional.empty(), 0);
                                }
                                Optional<Result> result = parseTextures(realName, r.body());
                                return new Answer(result, result.isPresent() ? CACHE_MS : NOT_FOUND_MS);
                            });
                });
    }

    private static Optional<Result> parseTextures(String name, String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray properties = root.getAsJsonArray("properties");
        if (properties == null) {
            return Optional.empty();
        }
        for (JsonElement e : properties) {
            JsonObject property = e.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString())) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) {
                return Optional.empty();
            }
            JsonObject skin = textures.getAsJsonObject("SKIN");
            String url = skin.get("url").getAsString().replace("http://", "https://");
            boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                    && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
            return Optional.of(new Result(name, url, slim));
        }
        return Optional.empty();
    }

    private static HttpClient client() {
        HttpClient c = client;
        if (c == null) {
            synchronized (MojangSkinResolver.class) {
                c = client;
                if (c == null) {
                    c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
                    client = c;
                }
            }
        }
        return c;
    }

    private record Cached(Optional<Result> result, long expires) {
    }
}
