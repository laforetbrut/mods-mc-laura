package com.vyrriox.lauramod.world;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.dialogue.TextMatcher;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.LauraBrain;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.network.LauraAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Understands what players write in chat, in any language the dialogue files know, and turns it
 * into orders, answers and reactions.
 * <ul>
 *     <li>Several orders can be chained with "then" (or "puis", "danach", "luego"...): the first one
 *     runs now, the others are queued.</li>
 *     <li>With several companions, the message goes to the one whose name is written, to all of them
 *     with "everyone" (or "les filles"...), otherwise to the selected or nearest one.</li>
 * </ul>
 *
 * @author vyrriox
 */
public final class LauraChat {
    private static final Set<String> ANSWERS = Set.of("yes", "no");

    private LauraChat() {
    }

    /** Called by the loader for every chat message. Handling is deferred one tick so her answer comes after the message. */
    public static void onChat(ServerPlayer player, String message) {
        MinecraftServer server = player.getServer();
        if (server == null || message == null || message.isBlank() || message.startsWith("/")) {
            return;
        }
        LauraManager.schedule(server, 1, () -> handle(player, message));
    }

    /** Companions the message is for. */
    static List<LauraEntity> targets(ServerPlayer player, String message) {
        double range = LauraConfig.chatRange.getInt();
        List<LauraEntity> nearby = new ArrayList<>();
        for (LauraEntity laura : LauraManager.findAll(player)) {
            if (laura.level() == player.level() && laura.distanceToSqr(player) <= range * range) {
                nearby.add(laura);
            }
        }
        if (nearby.isEmpty()) {
            return nearby;
        }
        if (DialogueManager.mentionsEveryone(message, player.clientInformation().language())) {
            return nearby;
        }
        List<LauraEntity> named = new ArrayList<>();
        for (LauraEntity laura : nearby) {
            if (DialogueManager.mentions(message, laura.getLauraName())) {
                named.add(laura);
            }
        }
        if (!named.isEmpty()) {
            return named;
        }
        if (LauraConfig.requireNameInChat.get()) {
            return new ArrayList<>();
        }
        LauraEntity preferred = LauraManager.findNear(player, range);
        return preferred == null ? new ArrayList<>() : new ArrayList<>(List.of(preferred));
    }

    private static void handle(ServerPlayer player, String message) {
        if (player.hasDisconnected()) {
            return;
        }
        List<String> segments = DialogueManager.splitOrders(message, player.clientInformation().language());
        // The summoning words work from anywhere; everything else is proximity chat.
        for (String segment : segments) {
            DialogueManager.IntentMatch match = DialogueManager.match(segment, player.clientInformation().language());
            if (match != null && match.intent().equals("summon")) {
                LauraManager.summon(player, true);
                return;
            }
        }
        List<LauraEntity> targets = targets(player, message);
        if (targets.isEmpty()) {
            return;
        }
        for (LauraEntity laura : targets) {
            if (!laura.isOwnedBy(player) && !LauraConfig.othersCanInteract.get()) {
                continue;
            }
            if (laura.isOwnedBy(player)) {
                laura.brain().onChatFromOwner();
            }
            boolean first = true;
            boolean understood = false;
            for (String segment : segments) {
                DialogueManager.IntentMatch match = DialogueManager.match(segment, player.clientInformation().language());
                if (match == null) {
                    continue;
                }
                understood = true;
                handleIntent(player, laura, match, segment, !first);
                first = false;
            }
            if (!understood) {
                notUnderstood(player, laura, message);
            }
        }
        if (targets.size() == 1) {
            LauraManager.select(player, targets.get(0));
        }
    }

    private static void notUnderstood(ServerPlayer player, LauraEntity laura, String message) {
        LauraBrain brain = laura.brain();
        if (brain.pendingQuestion() == LauraBrain.Question.LOVE) {
            DialogueManager.IntentMatch answer = DialogueManager.match(message, ANSWERS, player.clientInformation().language());
            if (answer != null) {
                brain.answer(player, answer.intent().equals("yes"));
                return;
            }
        }
        if (DialogueManager.mentions(message, laura.getLauraName())) {
            LauraSpeech.say(laura, player, "confused", LineFormatter.values());
        } else if (player.getServer().getPlayerCount() > 1 && laura.isOwnedBy(player)) {
            brain.watcher().onChatWithOthers(player);
        }
    }

    private static final java.util.regex.Pattern NUMBER = java.util.regex.Pattern.compile("\\d{1,4}");

    /**
     * The quantity written in a fetch order ("bring me 3 apples", "apporte 32 pains"), from 1 to
     * 576, or 0 when the message has no number (the default amount is then used). Digits only, in
     * any language, including scripts written without spaces.
     */
    static int countIn(TextMatcher.Prepared message) {
        java.util.regex.Matcher m = NUMBER.matcher(message.text());
        return m.find() ? Math.max(1, Math.min(576, Integer.parseInt(m.group()))) : 0;
    }

    private static void handleIntent(ServerPlayer player, LauraEntity laura, DialogueManager.IntentMatch match, String segment, boolean queue) {
        LauraBrain brain = laura.brain();
        String intent = match.intent();
        TextMatcher.Prepared prepared = TextMatcher.Prepared.of(segment);
        LineFormatter.Values none = LineFormatter.values();
        if (com.vyrriox.lauramod.api.LauraAPI.fire("chat", laura, player, intent)) {
            // A script handled this message itself.
            return;
        }
        switch (intent) {
            case "yes", "no" -> {
                // Only an answer to her question: "no" alone is a filler word in several languages.
                brain.answer(player, intent.equals("yes"));
            }
            case "where" -> LauraSpeech.say(laura, player, "where.here", none);
            case "follow" -> orderOrQueue(player, laura, LauraAction.FOLLOW, LauraTask.Type.FOLLOW, queue);
            case "stay" -> orderOrQueue(player, laura, LauraAction.STAY, LauraTask.Type.STAY, queue);
            case "come" -> orderOrQueue(player, laura, LauraAction.COME, LauraTask.Type.COME, queue);
            case "home" -> orderOrQueue(player, laura, LauraAction.HOME, LauraTask.Type.GO_HOME, queue);
            case "wander" -> LauraActions.perform(player, laura, LauraAction.WANDER, "", LauraActions.Source.CHAT);
            case "set_home" -> LauraActions.perform(player, laura, LauraAction.SET_HOME, "", LauraActions.Source.CHAT);
            case "stop" -> LauraActions.perform(player, laura, LauraAction.STOP, "", LauraActions.Source.CHAT);
            case "fetch" -> {
                List<String> items = DialogueManager.matchItems(prepared, player.clientInformation().language());
                if (items.isEmpty() && player.getMainHandItem().isEmpty()) {
                    LauraSpeech.say(laura, player, "fetch.what", none);
                    return;
                }
                LauraActions.fetch(player, laura, items.isEmpty() ? "held" : items.get(0), countIn(prepared), queue);
            }
            case "chop_tree" -> LauraActions.task(player, laura, LauraTask.Type.CHOP_TREE, 0, queue);
            case "harvest" -> LauraActions.task(player, laura, LauraTask.Type.HARVEST, 0, queue);
            case "cook" -> LauraActions.task(player, laura, LauraTask.Type.COOK, 0, queue);
            case "job_lumberjack" -> LauraActions.enableJob(player, laura, LauraJob.LUMBERJACK, 0);
            case "job_farmer" -> LauraActions.enableJob(player, laura, LauraJob.FARMER, 0);
            case "job_cook" -> LauraActions.enableJob(player, laura, LauraJob.COOK, 0);
            case "job_stop" -> LauraActions.disableJob(player, laura, null);
            case "back_to_work" -> orderOrQueue(player, laura, null, LauraTask.Type.WORK, queue);
            case "assign_chest" -> {
                String purpose = DialogueManager.matchChestPurpose(prepared, player.clientInformation().language());
                ChestPurpose p = purpose == null ? null : ChestPurpose.byName(purpose);
                if (p == null) {
                    LauraSpeech.say(laura, player, "chest.which_purpose", none);
                } else {
                    LauraActions.assignChest(player, laura, p);
                }
            }
            case "hug" -> LauraActions.perform(player, laura, LauraAction.HUG, "", LauraActions.Source.CHAT);
            case "kiss" -> LauraActions.perform(player, laura, LauraAction.KISS, "", LauraActions.Source.CHAT);
            case "apology" -> brain.apologize(player);
            case "eat" -> LauraActions.perform(player, laura, LauraAction.EAT, "", LauraActions.Source.CHAT);
            case "sleep" -> LauraActions.perform(player, laura, LauraAction.SLEEP, "", LauraActions.Source.CHAT);
            case "wake_up" -> LauraActions.perform(player, laura, LauraAction.WAKE_UP, "", LauraActions.Source.CHAT);
            case "beautiful", "love_you", "miss_you", "thanks" -> {
                brain.onCompliment();
                LauraSpeech.respond(laura, player, intent, none);
            }
            case "insult" -> {
                brain.onInsult(player);
                LauraSpeech.respond(laura, player, intent, none);
            }
            case "what_do_you_want" -> {
                if (brain.desire() != null) {
                    var d = brain.desire();
                    LauraSpeech.say(laura, player, "desire.tell", LineFormatter.values().with("item", d.describe()).with("place", d.describe()).with("activity", d.describe()));
                } else {
                    LauraSpeech.say(laura, player, "desire.none", none);
                }
            }
            case "marry_me" -> {
                LauraSpeech.respond(laura, player, intent, none);
                LauraAdvancements.award(player, "marry");
            }
            case "how_are_you" -> LauraSpeech.sayFirst(laura, player, none, "status." + laura.getMood().key(), "status");
            case "player_hungry" -> {
                if (!giveFood(player, laura)) {
                    LauraSpeech.respond(laura, player, intent, none);
                }
            }
            default -> {
                if (intent.startsWith("emote_")) {
                    LauraActions.emote(player, laura, Emote.byName(intent.substring("emote_".length())));
                    LauraSpeech.respond(laura, player, intent, none);
                } else if (!LauraSpeech.respond(laura, player, intent, none)) {
                    LauraSpeech.say(laura, player, "confused", none);
                }
            }
        }
    }

    private static void orderOrQueue(ServerPlayer player, LauraEntity laura, LauraAction action, LauraTask.Type type, boolean queue) {
        if (queue && (laura.workplace().current() != null || laura.fetchGoal().isActive())) {
            LauraTask task = new LauraTask(type, "", 0, null);
            if (laura.workplace().enqueue(task)) {
                LauraSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", task.describe()));
            }
            return;
        }
        if (action == null) {
            LauraActions.backToWork(player, laura);
        } else {
            LauraActions.perform(player, laura, action, "", LauraActions.Source.CHAT);
        }
    }

    /** "I'm hungry": she gives her partner some of her food. */
    private static boolean giveFood(ServerPlayer player, LauraEntity laura) {
        var inv = laura.inventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED) {
                ItemStack given = stack.split(1);
                inv.setChanged();
                laura.brain().giveToPlayer(player, given);
                LauraSpeech.say(laura, player, "owner.feed", LineFormatter.values().with("item", given.getHoverName()));
                return true;
            }
        }
        return false;
    }
}
