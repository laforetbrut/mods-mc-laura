package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.network.LauraNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Everything Laura says goes through here: line lookup in the player's language, placeholders,
 * the gag, the chat prefix and the speech bubble.
 *
 * @author vyrriox
 */
public final class LauraSpeech {
    private LauraSpeech() {
    }

    /** Says a dialogue line to a player. Returns false when no line exists for the key. */
    public static boolean say(LauraEntity laura, ServerPlayer to, String key, LineFormatter.Values values) {
        if (to == null) {
            return false;
        }
        String effectiveKey = laura.isGagged() ? "gagged_talk" : key;
        String line = DialogueManager.pick(to.clientInformation().language(), effectiveKey, laura.getRandom());
        if (line == null && laura.isGagged()) {
            line = "Mmmph!";
        }
        if (line == null) {
            return false;
        }
        deliver(laura, to, format(laura, to, line, values));
        if (com.vyrriox.lauramod.test.LauraSelfTest.enabled()) {
            SAID.merge(effectiveKey, 1, Integer::sum);
        }
        return true;
    }

    /** How many times each dialogue key was said. Only filled during the self tests, which read it. */
    private static final java.util.Map<String, Integer> SAID = new java.util.HashMap<>();

    /** Self tests: how many times a dialogue key was said since the server started. */
    public static int timesSaid(String key) {
        return SAID.getOrDefault(key, 0);
    }

    /** Tries the keys in order and says the first one that exists. */
    public static boolean sayFirst(LauraEntity laura, ServerPlayer to, LineFormatter.Values values, String... keys) {
        if (to == null) {
            return false;
        }
        if (laura.isGagged()) {
            return say(laura, to, "gagged_talk", values);
        }
        String line = DialogueManager.pickFirst(to.clientInformation().language(), laura.getRandom(), keys);
        if (line == null) {
            return false;
        }
        deliver(laura, to, format(laura, to, line, values));
        return true;
    }

    public static boolean sayToOwner(LauraEntity laura, String key, LineFormatter.Values values) {
        return say(laura, owner(laura), key, values);
    }

    public static boolean sayFirstToOwner(LauraEntity laura, LineFormatter.Values values, String... keys) {
        return sayFirst(laura, owner(laura), values, keys);
    }

    /** Answers a chat intent. */
    public static boolean respond(LauraEntity laura, ServerPlayer to, String intent, LineFormatter.Values values) {
        if (laura.isGagged()) {
            return say(laura, to, "gagged_talk", values);
        }
        String line = DialogueManager.respond(to.clientInformation().language(), intent, laura.getRandom());
        if (line == null) {
            return false;
        }
        deliver(laura, to, format(laura, to, line, values));
        return true;
    }

    /** Sends an already built message with her prefix. */
    public static void sayRaw(LauraEntity laura, ServerPlayer to, Component message) {
        deliver(laura, to, message.copy());
    }

    public static ServerPlayer owner(LauraEntity laura) {
        LivingEntity owner = laura.getOwner();
        return owner instanceof ServerPlayer player ? player : null;
    }

    private static MutableComponent format(LauraEntity laura, ServerPlayer to, String line, LineFormatter.Values values) {
        LineFormatter.Values all = LineFormatter.values()
                .with("laura", laura.getLauraName())
                .with("player", to.getName().getString())
                .with("days", laura.daysTogether());
        all.putAll(values);
        return LineFormatter.format(line, all);
    }

    /** Proximity chat: same dimension and within the configured range. */
    public static boolean inEarshot(LauraEntity laura, ServerPlayer player) {
        double range = LauraConfig.chatRange.getInt();
        return player.level() == laura.level() && player.distanceToSqr(laura) <= range * range;
    }

    /**
     * Proximity chat: everybody close enough hears her (not only the player she talks to), and
     * nobody further away does.
     */
    private static void deliver(LauraEntity laura, ServerPlayer to, MutableComponent text) {
        LauraNetwork.sendSpeech(laura, text);
        if (!(laura.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        MutableComponent message = Component.literal("<").append(nameComponent(laura)).append("> ").append(text);
        for (ServerPlayer player : level.players()) {
            if (inEarshot(laura, player)) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static MutableComponent nameComponent(LauraEntity laura) {
        return Component.literal(laura.getLauraName()).withStyle(nameStyle());
    }

    private static Style nameStyle() {
        String color = LauraConfig.nameColor.get().trim();
        ChatFormatting formatting = ChatFormatting.getByName(color);
        if (formatting != null && formatting.isColor()) {
            return Style.EMPTY.withColor(formatting);
        }
        TextColor parsed = TextColor.parseColor(color).result().orElse(null);
        return parsed != null ? Style.EMPTY.withColor(parsed) : Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
    }
}
