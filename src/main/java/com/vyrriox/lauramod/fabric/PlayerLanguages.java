package com.vyrriox.lauramod.fabric;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The language each connected client reported in its settings packet. Minecraft 1.20.1 applies
 * that packet without keeping the language and Fabric has no event for it, so
 * {@link com.vyrriox.lauramod.fabric.mixin.ServerPlayerMixin} fills this table.
 *
 * @author vyrriox
 */
public final class PlayerLanguages {
    private static final String DEFAULT = "en_us";
    // Kept per player id, not on the player object: a respawn creates a new object and the client does not send its settings again.
    private static final Map<UUID, String> LANGUAGES = new ConcurrentHashMap<>();

    private PlayerLanguages() {
    }

    /** Called by the mixin each time the settings packet of a client is applied. */
    public static void remember(ServerPlayer player, String language) {
        if (language == null || language.isBlank()) {
            LANGUAGES.remove(player.getUUID());
        } else {
            LANGUAGES.put(player.getUUID(), language);
        }
    }

    static String of(ServerPlayer player) {
        return LANGUAGES.getOrDefault(player.getUUID(), DEFAULT);
    }

    static void forget(ServerPlayer player) {
        LANGUAGES.remove(player.getUUID());
    }
}
