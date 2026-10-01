package com.vyrriox.lauramod.skin;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * The skin reference stored on Laura and synced to clients.
 * <ul>
 *     <li>{@code builtin:<name>}: a texture shipped with the mod</li>
 *     <li>{@code url:<address>}: downloaded by each client</li>
 *     <li>{@code server:<name>#<sha1>}: a file from the server's config/lauramod/skins folder</li>
 *     <li>{@code player:<name>}: waiting for the server to resolve a Minecraft account skin</li>
 * </ul>
 *
 * @author vyrriox
 */
public record SkinRef(Type type, String value, String hash) {
    public enum Type {
        BUILTIN, URL, SERVER, PLAYER
    }

    /** Skins shipped in {@code assets/lauramod/textures/entity/laura/}. */
    public static final List<String> BUILTIN = List.of("laura", "laura_summer", "laura_winter", "laura_night", "laura_sporty", "laura_gothic");

    public static final SkinRef DEFAULT = new SkinRef(Type.BUILTIN, "laura", "");

    public static SkinRef parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        int colon = raw.indexOf(':');
        if (colon <= 0) {
            return BUILTIN.contains(raw) ? new SkinRef(Type.BUILTIN, raw, "") : DEFAULT;
        }
        String prefix = raw.substring(0, colon).toLowerCase(Locale.ROOT);
        String rest = raw.substring(colon + 1);
        return switch (prefix) {
            case "builtin" -> BUILTIN.contains(rest) ? new SkinRef(Type.BUILTIN, rest, "") : DEFAULT;
            case "url", "http", "https" -> new SkinRef(Type.URL, prefix.equals("url") ? rest : raw, "");
            case "server", "file" -> {
                int hash = rest.lastIndexOf('#');
                yield hash > 0 ? new SkinRef(Type.SERVER, rest.substring(0, hash), rest.substring(hash + 1)) : new SkinRef(Type.SERVER, rest, "");
            }
            case "player" -> new SkinRef(Type.PLAYER, rest, "");
            default -> DEFAULT;
        };
    }

    public String serialize() {
        return switch (type) {
            case BUILTIN -> "builtin:" + value;
            case URL -> "url:" + value;
            case SERVER -> "server:" + value + (hash.isEmpty() ? "" : "#" + hash);
            case PLAYER -> "player:" + value;
        };
    }

    /** Cache key used by clients for downloaded textures. */
    public String cacheKey() {
        return type == Type.SERVER ? "server/" + value + "#" + hash : serialize();
    }

    /**
     * Rewrites common "page" links into direct image links (NameMC and Imgur pages), like 1.x did.
     */
    public static String normalizeUrl(String url) {
        String u = url.trim();
        if (u.contains("namemc.com/skin/")) {
            String id = u.substring(u.lastIndexOf('/') + 1);
            return "https://s.namemc.com/i/" + id + ".png";
        }
        if (u.contains("namemc.com/texture/")) {
            String id = u.substring(u.lastIndexOf('/') + 1);
            return "https://s.namemc.com/i/" + id + (id.endsWith(".png") ? "" : ".png");
        }
        if (u.contains("imgur.com/") && !u.contains("i.imgur.com") && !u.endsWith(".png")) {
            String id = u.substring(u.lastIndexOf('/') + 1);
            return "https://i.imgur.com/" + id + ".png";
        }
        return u;
    }

    /** True if the URL is http(s) and its host is on the whitelist ("*" allows everything). */
    public static boolean isAllowedUrl(String url, List<String> whitelist) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("https") && !scheme.equals("http")) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return false;
            }
            host = host.toLowerCase(Locale.ROOT);
            for (String allowed : whitelist) {
                String a = allowed.trim().toLowerCase(Locale.ROOT);
                if (a.equals("*") || host.equals(a) || host.endsWith("." + a)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
