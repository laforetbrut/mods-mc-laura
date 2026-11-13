package com.vyrriox.lauramod.client.gui;

import net.minecraft.network.chat.Component;

/**
 * Who draws her speech bubbles. With the kawaii widgets, LauraRenderer draws them itself from
 * ClientState.
 *
 * @author vyrriox
 */
public final class SpeechBubbles {
    private SpeechBubbles() {
    }

    public static boolean drawnByRenderer() {
        return true;
    }

    /** She said something; the renderer reads it from ClientState on its next frame. */
    public static void said(int entityId, Component text, int seconds) {
    }
}
