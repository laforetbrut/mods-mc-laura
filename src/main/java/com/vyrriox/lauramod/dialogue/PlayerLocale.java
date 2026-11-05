package com.vyrriox.lauramod.dialogue;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * The language a player's client reported. Minecraft 1.20.1 has no vanilla accessor for it, so the
 * loader provides it. Kept apart from {@link LocaleUtil}, which is plain Java shared by every target.
 *
 * @author vyrriox
 */
public final class PlayerLocale {
    private PlayerLocale() {
    }

    public static String of(ServerPlayer player) {
        return LauraMod.platform().playerLanguage(player);
    }
}
