package com.vyrriox.lauramod.world;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.Emote;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grants the mod's advancements ({@code data/lauramod/advancement/laura/*.json}, all using the
 * impossible trigger so only the mod can award them) and keeps the per-player counters behind the
 * milestone ones (desires fulfilled, items fetched, logs chopped...). Counters are saved in the
 * world data, so progress survives restarts and companion changes.
 *
 * @author vyrriox
 */
public final class LauraAdvancements {
    /** Counter name to (threshold, advancement) milestones. */
    private static final Map<String, int[]> THRESHOLDS = Map.ofEntries(
            Map.entry("hugs", new int[]{1}),
            Map.entry("kisses", new int[]{1}),
            Map.entry("desires", new int[]{1, 10, 50}),
            Map.entry("fetched", new int[]{1, 100}),
            Map.entry("logs", new int[]{1, 100}),
            Map.entry("harvested", new int[]{1, 500}),
            Map.entry("meals", new int[]{1, 100}),
            Map.entry("gifts", new int[]{1, 50}));
    private static final Map<String, String[]> NAMES = Map.ofEntries(
            Map.entry("hugs", new String[]{"first_hug"}),
            Map.entry("kisses", new String[]{"first_kiss"}),
            Map.entry("desires", new String[]{"desire_fulfilled", "desire_10", "desire_50"}),
            Map.entry("fetched", new String[]{"fetch", "fetch_100"}),
            Map.entry("logs", new String[]{"task_chop_tree", "chop_100"}),
            Map.entry("harvested", new String[]{"task_harvest", "harvest_500"}),
            Map.entry("meals", new String[]{"task_cook", "cook_100"}),
            Map.entry("gifts", new String[]{"gift_first", "gift_50"}));
    private static final int[] DAY_MILESTONES = {7, 30, 100, 365};
    private static final String[] DAY_NAMES = {"anniversary", "days_30", "days_100", "days_365"};
    private static final String[] RELATIONS = {"friend", "close", "love", "soulmate"};

    private LauraAdvancements() {
    }

    /** Grants an advancement (all its criteria). Does nothing if it is already done or unknown. */
    public static void award(@Nullable ServerPlayer player, String key) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder advancement = server.getAdvancements().get(LauraMod.id("laura/" + key));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /** Adds to a counter of the player and grants the milestones it reaches. Returns the new total. */
    public static int add(@Nullable ServerPlayer player, String counter, int amount) {
        if (player == null || player.getServer() == null || amount <= 0) {
            return 0;
        }
        LauraWorldData data = LauraWorldData.get(player.getServer());
        LauraWorldData.OwnerMeta meta = data.meta(player.getUUID());
        int total = meta.stats.merge(counter, amount, Integer::sum);
        data.setDirty();
        int[] thresholds = THRESHOLDS.get(counter);
        String[] names = NAMES.get(counter);
        if (thresholds != null && names != null) {
            for (int i = 0; i < thresholds.length; i++) {
                if (total >= thresholds[i]) {
                    award(player, names[i]);
                }
            }
        }
        return total;
    }

    public static int get(ServerPlayer player, String counter) {
        if (player.getServer() == null) {
            return 0;
        }
        return LauraWorldData.get(player.getServer()).meta(player.getUUID()).stats.getOrDefault(counter, 0);
    }

    /** Days spent together with a companion. */
    public static void onDays(@Nullable ServerPlayer owner, int days) {
        for (int i = 0; i < DAY_MILESTONES.length; i++) {
            if (days >= DAY_MILESTONES[i]) {
                award(owner, DAY_NAMES[i]);
            }
        }
    }

    /** Relationship level reached ("hate", "cold", "friend", "close", "love", "soulmate"). */
    public static void onRelation(@Nullable ServerPlayer owner, String relation) {
        int level = -1;
        for (int i = 0; i < RELATIONS.length; i++) {
            if (RELATIONS[i].equals(relation)) {
                level = i;
            }
        }
        for (int i = 0; i <= level; i++) {
            award(owner, "relation_" + RELATIONS[i]);
        }
    }

    /** An emote she played for the player: the first one, then every emote. */
    public static void onEmote(@Nullable ServerPlayer player, Emote emote) {
        if (player == null || !emote.playerTriggered) {
            return;
        }
        award(player, "emote_first");
        add(player, "emote." + emote.animationName(), 1);
        for (Emote e : Emote.values()) {
            if (e.playerTriggered && get(player, "emote." + e.animationName()) == 0) {
                return;
            }
        }
        award(player, "emote_all");
    }
}
