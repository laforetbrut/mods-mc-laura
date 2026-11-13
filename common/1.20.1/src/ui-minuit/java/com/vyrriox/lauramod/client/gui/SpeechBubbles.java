package com.vyrriox.lauramod.client.gui;

import dev.dryaides.minuit.client.WorldBubbles;
import net.minecraft.network.chat.Component;

/**
 * Who draws her speech bubbles. On minuit, its world bubbles do, in the player's theme; her thought
 * bubble (an item in a cloud) stays LauraRenderer's.
 *
 * @author vyrriox
 */
public final class SpeechBubbles {
    private SpeechBubbles() {
    }

    public static boolean drawnByRenderer() {
        return false;
    }

    /** She said something: a new phrase replaces the bubble over her. */
    public static void said(int entityId, Component text, int seconds) {
        WorldBubbles.say(entityId, text.getString(), seconds * 20);
    }
}
