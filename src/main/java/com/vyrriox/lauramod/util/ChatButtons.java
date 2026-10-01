package com.vyrriox.lauramod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * Clickable chat buttons. They make answers work in every language: the player clicks instead of
 * typing "yes" in the right language. Isolated here because the click event API changes between
 * Minecraft versions.
 *
 * @author vyrriox
 */
public final class ChatButtons {
    private ChatButtons() {
    }

    public static MutableComponent button(Component label, String command, ChatFormatting color) {
        return Component.literal("[").append(label).append("]").withStyle(style -> style
                .withColor(color)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    public static MutableComponent suggest(Component label, String command, ChatFormatting color) {
        return Component.literal("[").append(label).append("]").withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }
}
