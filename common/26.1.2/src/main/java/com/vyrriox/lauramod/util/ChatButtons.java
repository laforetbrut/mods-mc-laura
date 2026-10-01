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
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }

    public static MutableComponent suggest(Component label, String command, ChatFormatting color) {
        return Component.literal("[").append(label).append("]").withStyle(style -> style
                .withColor(color)
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }
}
