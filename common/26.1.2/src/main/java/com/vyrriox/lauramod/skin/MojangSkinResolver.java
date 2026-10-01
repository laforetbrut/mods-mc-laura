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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Finds the skin of a Minecraft account from its name, through the public Mojang API. Results are
 * cached for an hour to stay far below the API rate limits.
 *
 * @author vyrriox
 */
public final class MojangSkinResolver {
    public record Result(String name, String url, boolean slim) {
    }

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final long CACHE_MS = 60 * 60 * 1000L;
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static volatile HttpClient client;

    private MojangSkinResolver() {
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    public static CompletableFuture<Optional<Result>> resolve(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Cached cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - cached.time < CACHE_MS) {
            return CompletableFuture.completedFuture(cached.result);
        }
        HttpClient http = client();
        HttpRequest profileRequest = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                .timeout(Duration.ofSeconds(10)).header("User-Agent", "lauramod").GET().build();
        return http.sendAsync(profileRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() != 200) {
                        return CompletableFuture.completedFuture(Optional.<Result>empty());
                    }
                    JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                    String id = profile.get("id").getAsString();
                    String realName = profile.has("name") ? profile.get("name").getAsString() : name;
                    HttpRequest texturesRequest = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id))
                            .timeout(Duration.ofSeconds(10)).header("User-Agent", "lauramod").GET().build();
                    return http.sendAsync(texturesRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                            .thenApply(r -> r.statusCode() == 200 ? parseTextures(realName, r.body()) : Optional.<Result>empty());
                })
                .exceptionally(e -> {
                    LauraMod.LOGGER.warn("Could not resolve the skin of {}: {}", name, e.getMessage());
                    return Optional.empty();
                })
                .thenApply(result -> {
                    CACHE.put(key, new Cached(result, System.currentTimeMillis()));
                    return result;
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

    private record Cached(Optional<Result> result, long time) {
    }
}
