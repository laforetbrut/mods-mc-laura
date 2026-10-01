package com.vyrriox.lauramod.entity.brain;

import com.vyrriox.lauramod.world.LauraAdvancements;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.desire.DesireTable;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.Mood;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.util.ChatButtons;
import com.vyrriox.lauramod.util.ItemSpec;
import com.vyrriox.lauramod.world.LauraWorldChecks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Laura's personality: needs, mood, desires, nagging and reactions. Ticked once per second on the
 * server by {@link LauraEntity}.
 *
 * @author vyrriox
 */
public final class LauraBrain {
    public enum Question {
        NONE, LOVE
    }

    private final LauraEntity laura;
    private final Needs needs = new Needs();
    private final OwnerWatcher watcher;
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Kind of gift or food, to the game time until which it gives no affection again. Saved. */
    private final Map<String, Long> rewardCooldowns = new HashMap<>();
    private Desire desire;
    private long nextDesireTime = -1;
    private long sulkUntil;
    private long angryUntil;
    private long happyUntil;
    private long jealousUntil;
    private int aggression;
    private long lastOwnerHit;
    private long lastAte;
    private Question question = Question.NONE;
    private long questionExpires;
    private long nextChatter = -1;
    private long nextLoveCheck = -1;
    private long nextFart = -1;
    private long nextPlayfulHit = -1;
    private long nextSpontaneous = -1;
    private int lastAnniversary = -1;
    private String lastRefused = "";
    private long lastRefusedTime;
    private Vec3 lastPos;
    private boolean wasRaining;
    private boolean wasThundering;
    private boolean wasNight;

    public LauraBrain(LauraEntity laura) {
        this.laura = laura;
        this.watcher = new OwnerWatcher(this);
    }

    public LauraEntity laura() {
        return laura;
    }

    public Needs needs() {
        return needs;
    }

    public Desire desire() {
        return desire;
    }

    public Question pendingQuestion() {
        return question;
    }

    long now() {
        return laura.level().getGameTime();
    }

    ServerPlayer owner() {
        return LauraSpeech.owner(laura);
    }

    // ------------------------------------------------------------------ cooldown helpers

    /** True (and arms the cooldown) if the key is ready. Seconds are scaled by the nagging level. */
    boolean ready(String key, double seconds, boolean scaleWithNagging) {
        long now = now();
        Long until = cooldowns.get(key);
        if (until != null && now < until) {
            return false;
        }
        double scaled = scaleWithNagging ? seconds / Math.max(0.1, LauraConfig.annoyance.get().nagging) : seconds;
        cooldowns.put(key, now + (long) (scaled * 20));
        return true;
    }

    boolean ready(String key, double seconds) {
        return ready(key, seconds, true);
    }

    /** Cooldown check for other packages (goals, actions). Scaled by the nagging level. */
    public boolean readyPublic(String key, double seconds) {
        return ready(key, seconds, true);
    }

    /** Cooldown check that ignores the nagging level. */
    public boolean readyFixed(String key, double seconds) {
        return ready(key, seconds, false);
    }

    private long randomInterval(int averageMinutes) {
        double avg = averageMinutes * 60 * 20;
        return (long) (avg * (0.5 + laura.getRandom().nextDouble()));
    }

    // ------------------------------------------------------------------ main tick

    public void tickSecond() {
        long now = now();
        ServerPlayer owner = owner();
        boolean ownerNear = owner != null && owner.level() == laura.level() && laura.distanceToSqr(owner) < 16 * 16;
        boolean needsOn = LauraConfig.needsEnabled.get();
        if (needsOn) {
            decayNeeds(owner, ownerNear);
        }
        regenerate(needsOn);
        if (needsOn && LauraConfig.autoEat.get() && needs.get(Needs.Need.HUNGER) < 35 && now - lastAte > 200 && !laura.isGagged()) {
            eatFromInventory();
        }
        if (needsOn && LauraConfig.stealFood.get() && owner != null && needs.get(Needs.Need.HUNGER) < 8
                && laura.distanceToSqr(owner) < 9 && !laura.isGagged() && ready("steal_food", 150)) {
            stealFood(owner);
        }
        updateMood(now);
        if (owner != null) {
            if (needsOn && LauraConfig.desiresEnabled.get() && !laura.isAsleep()) {
                tickDesire(now, owner, ownerNear);
            }
            if (ownerNear && !laura.isAsleep()) {
                nag(owner);
                timedEvents(now, owner);
            }
            watcher.tick(owner, ownerNear, now);
            if (!laura.keptOwnerItems().isEmpty() && owner.isAlive() && owner.level() == laura.level() && laura.distanceToSqr(owner) < 16) {
                int count = 0;
                for (ItemStack stack : laura.keptOwnerItems()) {
                    giveToPlayer(owner, stack);
                    count++;
                }
                laura.keptOwnerItems().clear();
                laura.playEmote(Emote.HUG);
                LauraSpeech.say(laura, owner, "items_returned", LineFormatter.values().with("count", count));
            }
        }
        if (question == Question.LOVE && now > questionExpires) {
            question = Question.NONE;
            if (owner != null) {
                LauraSpeech.say(laura, owner, "love_check.timeout", LineFormatter.values());
                changeAffection(-5);
                needs.add(Needs.Need.ATTENTION, -10);
            }
        }
        lastPos = laura.position();
        syncToEntity();
    }

    private void decayNeeds(ServerPlayer owner, boolean ownerNear) {
        double decay = LauraConfig.annoyance.get().decay;
        boolean asleep = laura.isAsleep();
        boolean fighting = laura.getTarget() != null;
        boolean moving = lastPos != null && lastPos.distanceToSqr(laura.position()) > 0.04;
        boolean night = LauraWorldChecks.isNight(laura.level());

        double hunger = perSecond(LauraConfig.hungerMinutes.getInt()) * decay * (fighting ? 1.8 : moving ? 1.2 : 1.0) * (asleep ? 0.4 : 1.0);
        needs.add(Needs.Need.HUNGER, (float) -hunger);

        if (asleep) {
            needs.add(Needs.Need.ENERGY, 100F / (3 * 60));
        } else {
            double energy = perSecond(LauraConfig.energyMinutes.getInt()) * decay * (night ? 1.4 : 1.0) * (fighting ? 1.5 : 1.0);
            needs.add(Needs.Need.ENERGY, (float) -energy);
        }

        double fun = perSecond(LauraConfig.funMinutes.getInt()) * decay;
        if (laura.isDancing()) {
            needs.add(Needs.Need.FUN, 1.2F);
        } else if (LauraWorldChecks.isMusicPlaying(laura)) {
            needs.add(Needs.Need.FUN, 0.4F);
        } else if (ownerNear && moving && laura.getMode() == LauraMode.FOLLOW) {
            needs.add(Needs.Need.FUN, (float) -(fun * 0.4));
        } else {
            needs.add(Needs.Need.FUN, (float) -(fun * (asleep ? 0.2 : 1.0)));
        }

        double attention = perSecond(LauraConfig.attentionMinutes.getInt()) * decay;
        if (owner == null || owner.level() != laura.level() || laura.distanceToSqr(owner) > 24 * 24) {
            attention *= 1.5;
        } else if (laura.distanceToSqr(owner) < 5 * 5) {
            attention *= 0.35;
        }
        needs.add(Needs.Need.ATTENTION, (float) -(attention * (asleep ? 0.1 : 1.0)));

        if (laura.isInWater()) {
            needs.add(Needs.Need.HYGIENE, 5F);
        } else if (laura.isInWaterOrRain()) {
            needs.add(Needs.Need.HYGIENE, 1F);
        } else {
            double hygiene = perSecond(LauraConfig.hygieneMinutes.getInt()) * decay * (fighting ? 2.0 : 1.0);
            needs.add(Needs.Need.HYGIENE, (float) -hygiene);
        }
        if (needs.get(Needs.Need.HYGIENE) < 15 && laura.getRandom().nextInt(4) == 0) {
            laura.spawnParticles(ParticleTypes.MYCELIUM, 3, 0.4);
        }
    }

    private static double perSecond(int minutes) {
        return 100.0 / (Math.max(1, minutes) * 60.0);
    }

    private void regenerate(boolean needsOn) {
        if (laura.isDeadOrDying()) {
            return;
        }
        float heal = LauraConfig.regenPerSecond.getFloat();
        if (needsOn) {
            float hunger = needs.get(Needs.Need.HUNGER);
            if (hunger >= 60) {
                heal += LauraConfig.healWhenFedPerSecond.getFloat();
            }
            if (hunger <= 0 && LauraConfig.starvationHurts.get() && laura.tickCount % 80 < 20) {
                if (laura.getHealth() > 1.0F || LauraConfig.starvationCanKill.get()) {
                    laura.hurt(laura.damageSources().starve(), 1.0F);
                }
            }
        } else if (heal <= 0) {
            heal = 0.5F;
        }
        if (heal > 0 && laura.getHealth() < laura.getMaxHealth()) {
            laura.heal(heal);
        }
    }

    // ------------------------------------------------------------------ mood

    private void updateMood(long now) {
        Mood mood;
        if (now < sulkUntil) {
            mood = Mood.SULKING;
        } else if (now < angryUntil) {
            mood = Mood.ANGRY;
        } else if (now < jealousUntil) {
            mood = Mood.JEALOUS;
        } else if (LauraConfig.needsEnabled.get() && needs.get(Needs.Need.HUNGER) < 15) {
            mood = Mood.HUNGRY;
        } else if (LauraConfig.needsEnabled.get() && needs.get(Needs.Need.ENERGY) < 15) {
            mood = Mood.TIRED;
        } else if (LauraConfig.needsEnabled.get() && (needs.get(needs.lowest()) < 20 || (desire != null && desire.elapsed(now) > 0.85))) {
            mood = Mood.SAD;
        } else if (LauraConfig.needsEnabled.get() && needs.get(Needs.Need.FUN) < 30) {
            mood = Mood.BORED;
        } else if (now < happyUntil || needs.average() > 75) {
            mood = laura.getAffection() >= 750 ? Mood.IN_LOVE : Mood.HAPPY;
        } else {
            mood = Mood.NEUTRAL;
        }
        laura.setMood(mood);
    }

    public void startSulking(int ticks) {
        sulkUntil = now() + Math.max(20, ticks);
        laura.setMood(Mood.SULKING);
        LauraAdvancements.award(owner(), "sulk");
    }

    public void stopSulking() {
        sulkUntil = 0;
        aggression = 0;
        updateMood(now());
    }

    public boolean isSulking() {
        return now() < sulkUntil;
    }

    public void makeHappy(int seconds) {
        happyUntil = Math.max(happyUntil, now() + seconds * 20L);
        angryUntil = 0;
    }

    public void changeAffection(int delta) {
        laura.setAffection(laura.getAffection() + delta);
        if (delta > 0) {
            LauraAdvancements.onRelation(owner(), relationKey());
        }
    }

    public void syncToEntity() {
        laura.syncNeeds(needs.pack());
        String thought = "";
        if (desire != null) {
            thought = desire.syncString();
        } else if (LauraConfig.needsEnabled.get()) {
            Needs.Need lowest = needs.lowest();
            if (needs.get(lowest) < 20) {
                thought = "need:" + lowest.key();
            }
        }
        laura.setThought(thought);
    }

    // ------------------------------------------------------------------ eating

    /** Eats one item of the stack if she can. Returns true if she ate it (the caller consumes it). */
    public boolean eat(ItemStack stack, ServerPlayer from) {
        return eat(stack, from, true);
    }

    /**
     * Eats one item of the stack. She says one line per item, never more: the wish it fulfils, or
     * what she thinks of the food. With {@code speak} false she says nothing (the caller does).
     */
    private boolean eat(ItemStack stack, ServerPlayer from, boolean speak) {
        GiftTable.FoodInfo food = GiftTable.food(stack);
        if (food == null) {
            return false;
        }
        ServerPlayer talkTo = from != null ? from : owner();
        if (laura.isGagged()) {
            if (speak) {
                LauraSpeech.say(laura, talkTo, "gagged_talk", LineFormatter.values());
            }
            return false;
        }
        boolean wanted = desire != null && desire.kind() == DesireType.Kind.ITEM && desire.itemSpec() != null && desire.itemSpec().test(stack);
        if (needs.get(Needs.Need.HUNGER) > 92 && food.preference() != GiftTable.Preference.FAVORITE && !wanted) {
            if (speak) {
                LauraSpeech.say(laura, talkTo, "eat.not_hungry", LineFormatter.values().with("item", stack.getHoverName()));
            }
            return false;
        }
        ItemStack eaten = stack.copyWithCount(1);
        lastAte = now();
        laura.playEmote(Emote.EAT);
        laura.setCarried(eaten);
        laura.playSound(SoundEvents.GENERIC_EAT, 0.8F, 0.9F + laura.getRandom().nextFloat() * 0.2F);
        laura.spawnItemParticles(eaten, 8);
        LineFormatter.Values values = LineFormatter.values().with("item", eaten.getHoverName());
        String line;
        if (food.preference() == GiftTable.Preference.DISLIKED) {
            needs.add(Needs.Need.HUNGER, food.nutrition() * 2F);
            changeAffection(from != null ? -10 : 0);
            angryUntil = now() + 20 * 20;
            line = "eat.disliked";
            if (from != null) {
                LauraAdvancements.award(from, "disliked_food");
            }
        } else {
            needs.add(Needs.Need.HUNGER, food.nutrition() * 6F + food.saturation() * 2F);
            laura.heal(food.nutrition() * 2F);
            if (food.preference() == GiftTable.Preference.FAVORITE) {
                // She always eats her favorite food, but being fed the same treat again and again
                // only makes her fonder once per cooldown.
                boolean rewarded = from != null && rewardReady(rewardKey(eaten, GiftTable.find(eaten)));
                if (from == null || rewarded) {
                    needs.add(Needs.Need.FUN, 10);
                }
                if (rewarded) {
                    changeAffection(5);
                    laura.hearts(3);
                    LauraAdvancements.award(from, "favorite_food");
                }
                line = "eat.favorite";
            } else {
                line = from != null ? "eat.fed" : "eat.self";
            }
        }
        if (from != null) {
            needs.add(Needs.Need.ATTENTION, 8);
            LauraAdvancements.award(from, "feed");
        }
        // The item she asked for counts as soon as her partner hands it over, even when she eats it
        // on the spot instead of keeping it. Food she takes from her own bag only counts for wishes
        // that are about eating.
        if (wanted && (desire.mustEat() || from != null)) {
            fulfillDesire(speak);
        } else if (speak) {
            LauraSpeech.say(laura, talkTo, line, values);
        }
        laura.level().getServer().tell(new net.minecraft.server.TickTask(laura.level().getServer().getTickCount() + 30, () -> laura.setCarried(ItemStack.EMPTY)));
        return true;
    }

    private void eatFromInventory() {
        var inventory = laura.inventory();
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food == null || food.preference() == GiftTable.Preference.DISLIKED && needs.get(Needs.Need.HUNGER) > 10) {
                continue;
            }
            int score = food.nutrition() * 10 + (food.preference() == GiftTable.Preference.FAVORITE ? 25 : 0);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) {
            ItemStack stack = inventory.getItem(best);
            if (eat(stack, null)) {
                stack.shrink(1);
                inventory.setChanged();
            }
        } else if (ready("hungry_no_food", 120)) {
            LauraSpeech.sayToOwner(laura, "need.hunger.no_food", LineFormatter.values());
        }
    }

    public boolean hasFood() {
        var inventory = laura.inventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty() && GiftTable.food(inventory.getItem(i)) != null) {
                return true;
            }
        }
        return false;
    }

    private void stealFood(ServerPlayer owner) {
        var inv = owner.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED) {
                ItemStack stolen = stack.copyWithCount(1);
                laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (eat(stolen, null, false)) {
                    stack.shrink(1);
                    LauraSpeech.say(laura, owner, "steal_food", LineFormatter.values().with("item", stolen.getHoverName()));
                    LauraAdvancements.award(owner, "steal_food");
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------------ gifts

    /**
     * Returns true if the item was accepted (the caller consumes it). She says one line per item,
     * never more: the gift she gives back, the wish it fulfils, her full bag, or what she thinks of it.
     */
    public boolean receiveGift(ServerPlayer from, ItemStack stack) {
        GiftTable.Gift gift = GiftTable.find(stack);
        boolean wanted = desire != null && desire.kind() == DesireType.Kind.ITEM && desire.itemSpec() != null && desire.itemSpec().test(stack);
        boolean flowerWanted = desire != null && desire.activity() == DesireType.Activity.FLOWERS
                && stack.is(net.minecraft.tags.ItemTags.SMALL_FLOWERS);
        if (gift == null && !wanted && !flowerWanted) {
            return false;
        }
        ItemStack given = stack.copyWithCount(1);
        LineFormatter.Values values = LineFormatter.values().with("item", given.getHoverName());
        // The same kind of gift only counts once per cooldown: it is still stored and still fulfils a
        // wish, but gives no affection, no fun and no gift back (she can hand it back and be given it
        // again). A gift she dislikes always counts.
        boolean rewarded = gift == null || gift.affection() <= 0 || rewardReady(rewardKey(given, gift));
        needs.add(Needs.Need.ATTENTION, 10);
        String line;
        if (gift != null) {
            if (rewarded) {
                changeAffection(gift.affection());
                needs.add(Needs.Need.FUN, gift.fun());
            }
            if (gift.tier() == GiftTable.Tier.GROSS) {
                angryUntil = now() + 30 * 20;
            } else if (rewarded && gift.tier().ordinal() >= GiftTable.Tier.GREAT.ordinal()) {
                makeHappy(120);
                laura.hearts(5);
            } else {
                laura.hearts(2);
            }
            line = "gift." + gift.tier().key();
            com.vyrriox.lauramod.api.LauraAPI.fire("gift", laura, from, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(given.getItem()).toString());
            if (rewarded) {
                LauraAdvancements.add(from, "gifts", 1);
                if (gift.tier() == GiftTable.Tier.AMAZING) {
                    LauraAdvancements.award(from, "gift_amazing");
                }
            }
        } else {
            laura.hearts(3);
            line = "gift.nice";
        }
        boolean bagFull = false;
        if (gift == null || gift.tier() != GiftTable.Tier.GROSS) {
            ItemStack rest = laura.bags().add(given.copy());
            if (!rest.isEmpty()) {
                // No room left: the gift is not destroyed, it lands at her feet.
                laura.spawnAtLocation(rest);
                bagFull = true;
            }
        }
        ItemStack back = ItemStack.EMPTY;
        if (rewarded && gift != null && gift.returnChance() > 0 && laura.getRandom().nextDouble() < gift.returnChance() * affectionFactor()) {
            back = giveReturnGift(from);
        }
        boolean fulfils = wanted || flowerWanted;
        if (fulfils) {
            fulfillDesire(back.isEmpty());
        }
        if (!back.isEmpty()) {
            LauraSpeech.say(laura, from, "gift.return", LineFormatter.values().with("item", back.getHoverName()).with("count", back.getCount()));
        } else if (!fulfils) {
            LauraSpeech.say(laura, from, bagFull ? "inventory_full" : line, values);
        }
        return true;
    }

    /**
     * Hands one of her return gifts to the player. Returns a copy of what she gave (EMPTY when the
     * table has none): the name and the count she announces come from that copy, never from a stack
     * the player's inventory has already taken.
     */
    public ItemStack giveReturnGift(ServerPlayer to) {
        ItemStack back = GiftTable.rollReturnGift(laura.getRandom());
        if (back.isEmpty()) {
            return ItemStack.EMPTY;
        }
        laura.playEmote(Emote.BLUSH);
        giveToPlayer(to, back);
        LauraAdvancements.award(to, "gift_return");
        return back.copy();
    }

    private double affectionFactor() {
        return 0.5 + laura.getAffection() / 1000.0;
    }

    /** One cooldown per gift entry (a tag entry covers all its items), or per item for plain food. */
    private static String rewardKey(ItemStack stack, GiftTable.Gift gift) {
        return gift != null ? gift.match().raw() : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** True (and arms the cooldown) when this kind of gift or food may give affection again. */
    private boolean rewardReady(String key) {
        int seconds = LauraConfig.giftCooldownSeconds.getInt();
        if (seconds <= 0) {
            return true;
        }
        long now = now();
        Long until = rewardCooldowns.get(key);
        if (until != null && now < until) {
            return false;
        }
        if (rewardCooldowns.size() >= 256) {
            rewardCooldowns.values().removeIf(t -> t <= now);
            if (rewardCooldowns.size() >= 256) {
                rewardCooldowns.clear();
            }
        }
        rewardCooldowns.put(key, now + seconds * 20L);
        return true;
    }

    /**
     * Gives the stack to the player; what does not fit in their inventory is dropped at their feet.
     * The stack passed in is left untouched, so callers can still read its name and its count.
     */
    public void giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack moving = stack.copy();
        player.getInventory().add(moving);
        if (!moving.isEmpty()) {
            player.drop(moving, false);
        }
    }

    // ------------------------------------------------------------------ desires

    private void tickDesire(long now, ServerPlayer owner, boolean ownerNear) {
        if (desire == null) {
            if (nextDesireTime < 0) {
                nextDesireTime = now + randomInterval(LauraConfig.desireMinutes.getInt());
            }
            if (now >= nextDesireTime && ownerNear) {
                long duration = LauraConfig.desireDeadlineMinutes.getInt() * 60L * 20L;
                desire = Desire.roll(laura.getRandom(), now, duration);
                if (desire != null) {
                    announceDesire(owner, "desire.new." + desire.kind().key());
                    laura.playEmote(Emote.THINK);
                } else {
                    nextDesireTime = now + randomInterval(LauraConfig.desireMinutes.getInt());
                }
            }
            return;
        }
        checkPolledDesire(owner);
        if (desire == null) {
            return;
        }
        if (now > desire.deadline()) {
            failDesire(owner);
            return;
        }
        double elapsed = desire.elapsed(now);
        int expectedReminders = elapsed > 0.9 ? 3 : elapsed > 0.7 ? 2 : elapsed > 0.4 ? 1 : 0;
        if (LauraConfig.annoyance.get().nagging >= 1.5 && elapsed > 0.2) {
            expectedReminders = Math.max(expectedReminders, (int) (elapsed * 8));
        }
        if (desire.reminders() < expectedReminders && ownerNear) {
            desire.addReminder();
            String key = elapsed > 0.85 ? "desire.remind.urgent" : "desire.remind";
            announceDesire(owner, key);
        }
    }

    private void announceDesire(ServerPlayer owner, String key) {
        LineFormatter.Values values = LineFormatter.values()
                .with("item", desire.describe())
                .with("place", desire.describe())
                .with("activity", desire.describe())
                .with("minutes", Math.max(1, (desire.deadline() - now()) / 1200));
        String specific = key + "." + desire.target().replace(':', '_');
        if (!LauraSpeech.sayFirst(laura, owner, values, specific, key)) {
            LauraSpeech.sayRaw(laura, owner, desire.describe());
        }
    }

    private void checkPolledDesire(ServerPlayer owner) {
        if (desire.kind() == DesireType.Kind.PLACE) {
            if (LauraWorldChecks.isInPlace(laura, DesireTable.place(desire.target()))) {
                fulfillDesire();
            }
            return;
        }
        DesireType.Activity activity = desire.activity();
        if (activity == null) {
            return;
        }
        if (activity == DesireType.Activity.FIREWORKS) {
            // Any rocket in the sky near her counts, whoever launched it.
            if (!laura.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.FireworkRocketEntity.class,
                    laura.getBoundingBox().inflate(32, 64, 32)).isEmpty()) {
                onFirework();
            }
            return;
        }
        boolean holds = switch (activity) {
            case SUNSET -> LauraWorldChecks.isSunset(laura.level()) && LauraWorldChecks.canSeeSky(laura) && owner.distanceToSqr(laura) < 100;
            case STARGAZE -> LauraWorldChecks.isNight(laura.level()) && !laura.level().isRaining() && LauraWorldChecks.canSeeSky(laura) && owner.distanceToSqr(laura) < 100;
            case MUSIC -> LauraWorldChecks.isMusicPlaying(laura);
            case BOAT_RIDE -> laura.getVehicle() instanceof Boat;
            case SWIM -> laura.isInWater();
            case PET -> LauraWorldChecks.ownerHasPetNear(laura, owner);
            case CAMPFIRE -> LauraWorldChecks.isNearLitCampfire(laura) && LauraWorldChecks.isNight(laura.level());
            case SLEEP_TOGETHER -> owner.isSleeping() && owner.distanceToSqr(laura) < 64;
            case WALK -> false;
            default -> false;
        };
        if (activity == DesireType.Activity.WALK) {
            if (lastPos != null && owner.distanceToSqr(laura) < 144 && laura.getMode() == LauraMode.FOLLOW) {
                desire.addWalked(Math.sqrt(lastPos.distanceToSqr(laura.position())));
                if (desire.walked() >= 250) {
                    fulfillDesire();
                }
            }
            return;
        }
        if (activity.seconds == 0) {
            if (holds) {
                fulfillDesire();
            }
            return;
        }
        if (holds) {
            desire.setProgress(desire.progress() + 1);
            if (desire.progress() >= activity.seconds) {
                fulfillDesire();
            }
        } else if (desire.progress() > 0) {
            desire.setProgress(Math.max(0, desire.progress() - 2));
        }
    }

    /** Called by actions (hug, kiss, dance, compliment...) to fulfil activity desires. */
    public void onActivity(DesireType.Activity activity) {
        if (desire != null && desire.activity() == activity) {
            fulfillDesire();
        }
    }

    private void fulfillDesire() {
        fulfillDesire(true);
    }

    /** With {@code speak} false the wish is fulfilled without her line (the caller says one). */
    private void fulfillDesire(boolean speak) {
        if (desire == null) {
            return;
        }
        ServerPlayer owner = owner();
        Desire done = desire;
        desire = null;
        nextDesireTime = now() + randomInterval(LauraConfig.desireMinutes.getInt());
        changeAffection(20 + laura.getRandom().nextInt(21));
        needs.add(Needs.Need.FUN, 30);
        needs.add(Needs.Need.ATTENTION, 25);
        makeHappy(300);
        stopSulking();
        laura.playEmote(Emote.CELEBRATE);
        laura.hearts(8);
        laura.playLauraSound(LauraRegistries.Sound.HAPPY, 1.0F, 1.1F);
        if (owner != null) {
            if (speak) {
                LineFormatter.Values values = LineFormatter.values().with("item", done.describe()).with("place", done.describe()).with("activity", done.describe());
                LauraSpeech.sayFirst(laura, owner, values, "desire.fulfilled." + done.kind().key(), "desire.fulfilled");
            }
            LauraAdvancements.add(owner, "desires", 1);
            com.vyrriox.lauramod.api.LauraAPI.fire("desire_fulfilled", laura, owner, done.syncString());
            if (done.kind() == DesireType.Kind.PLACE) {
                LauraAdvancements.award(owner, "desire_place");
            } else if (done.kind() == DesireType.Kind.ACTIVITY) {
                LauraAdvancements.award(owner, "desire_activity");
            }
        }
    }

    private void failDesire(ServerPlayer owner) {
        Desire failed = desire;
        desire = null;
        nextDesireTime = now() + randomInterval(Math.max(1, LauraConfig.desireMinutes.getInt() / 2));
        changeAffection(-20);
        needs.add(Needs.Need.FUN, -15);
        startSulking(20 * 60 * (1 + laura.getRandom().nextInt(3)));
        laura.playEmote(Emote.CRY);
        laura.playLauraSound(LauraRegistries.Sound.SAD, 1.0F, 1.0F);
        LineFormatter.Values values = LineFormatter.values().with("item", failed.describe()).with("place", failed.describe()).with("activity", failed.describe());
        LauraSpeech.say(laura, owner, "desire.failed", values);
        LauraAdvancements.award(owner, "desire_failed");
        com.vyrriox.lauramod.api.LauraAPI.fire("desire_failed", laura, owner, failed.syncString());
    }

    /** Clears the current desire (admin or reset). */
    public void clearDesire() {
        desire = null;
        nextDesireTime = -1;
    }

    /** True when the stack is what she currently wishes for (an item, or flowers for the flower wish). */
    public boolean wants(ItemStack stack) {
        if (desire == null || stack.isEmpty()) {
            return false;
        }
        if (desire.kind() == DesireType.Kind.ITEM && desire.itemSpec() != null && desire.itemSpec().test(stack)) {
            return true;
        }
        return desire.activity() == DesireType.Activity.FLOWERS && stack.is(net.minecraft.tags.ItemTags.SMALL_FLOWERS);
    }

    /** Replaces the current desire and announces it (used by reactions and tests). */
    public void forceDesire(Desire newDesire) {
        desire = newDesire;
        ServerPlayer owner = owner();
        if (owner != null && newDesire != null) {
            announceDesire(owner, "desire.new." + newDesire.kind().key());
        }
        syncToEntity();
    }

    // ------------------------------------------------------------------ nagging

    private void nag(ServerPlayer owner) {
        if (!LauraConfig.needsEnabled.get() || laura.isGagged() && !ready("gag_nag", 45)) {
            return;
        }
        Needs.Need lowest = needs.lowest();
        float value = needs.get(lowest);
        if (value < 15 && ready("nag.critical." + lowest.key(), 90)) {
            LauraSpeech.say(laura, owner, "need." + lowest.key() + ".critical", LineFormatter.values());
            if (lowest == Needs.Need.HUNGER) {
                LauraAdvancements.award(owner, "starving");
            }
            if (lowest == Needs.Need.ATTENTION) {
                poke(owner);
            }
        } else if (value < 30 && ready("nag.low." + lowest.key(), 200)) {
            LauraSpeech.say(laura, owner, "need." + lowest.key() + ".low", LineFormatter.values());
        }
        if (lowest == Needs.Need.ENERGY && value < 25 && ready("yawn", 60)) {
            laura.playEmote(Emote.YAWN);
            laura.playLauraSound(LauraRegistries.Sound.YAWN, 0.8F, 1.0F);
        }
    }

    /** Pokes her partner to get attention: a push and a sound, no damage. */
    public void poke(ServerPlayer owner) {
        if (laura.distanceToSqr(owner) > 9) {
            return;
        }
        laura.getLookControl().setLookAt(owner);
        laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        laura.playEmote(Emote.POKE);
        Vec3 push = owner.position().subtract(laura.position()).normalize().scale(0.25);
        owner.push(push.x, 0.05, push.z);
        owner.hurtMarked = true;
        laura.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 0.6F, 1.4F);
    }

    // ------------------------------------------------------------------ timed events

    private void timedEvents(long now, ServerPlayer owner) {
        if (nextChatter < 0) {
            nextChatter = now + randomInterval(Math.max(1, LauraConfig.chatterMinutes.getInt()));
            nextLoveCheck = now + randomInterval(Math.max(1, LauraConfig.loveCheckMinutes.getInt()));
            nextFart = now + randomInterval(LauraConfig.fartMinutes.getInt());
            nextPlayfulHit = now + LauraConfig.playfulHitMinutes.getInt() * 1200L;
        }
        if (LauraConfig.chatterMinutes.getInt() > 0 && now >= nextChatter) {
            nextChatter = now + randomInterval(LauraConfig.chatterMinutes.getInt());
            chatter(owner);
        }
        if (LauraConfig.loveCheckMinutes.getInt() > 0 && now >= nextLoveCheck && question == Question.NONE && !isSulking()) {
            nextLoveCheck = now + randomInterval(LauraConfig.loveCheckMinutes.getInt());
            askLove(owner);
        }
        if (LauraConfig.farts.get() && now >= nextFart) {
            nextFart = now + randomInterval(LauraConfig.fartMinutes.getInt());
            fart(owner);
        }
        if (LauraConfig.playfulHit.get() && now >= nextPlayfulHit) {
            nextPlayfulHit = now + LauraConfig.playfulHitMinutes.getInt() * 1200L;
            if (laura.distanceToSqr(owner) < 9 && !isSulking()) {
                laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                owner.hurt(laura.damageSources().mobAttack(laura), 1.0F);
                LauraSpeech.say(laura, owner, "oops_sorry", LineFormatter.values());
            }
        }
        int anniversaryDays = LauraConfig.anniversaryDays.getInt();
        int days = laura.daysTogether();
        if (anniversaryDays > 0 && days > 0 && days % anniversaryDays == 0 && days != lastAnniversary) {
            lastAnniversary = days;
            laura.playEmote(Emote.CELEBRATE);
            laura.hearts(10);
            laura.spawnParticles(ParticleTypes.FIREWORK, 30, 0.8);
            changeAffection(10);
            LauraSpeech.say(laura, owner, "anniversary", LineFormatter.values().with("days", days));
        }
        if (owner != null && ready("days_check", 600)) {
            LauraAdvancements.onDays(owner, days);
        }
        if (LauraConfig.weatherComplaints.get()) {
            weather(owner);
        }
        if (LauraConfig.spontaneousEmotes.get()) {
            if (nextSpontaneous < 0) {
                nextSpontaneous = now + spontaneousInterval();
            } else if (now >= nextSpontaneous) {
                nextSpontaneous = now + spontaneousInterval();
                spontaneousEmote();
            }
        }
    }

    private long spontaneousInterval() {
        double avg = LauraConfig.spontaneousEmoteSeconds.getInt() * 20.0;
        return (long) (avg * (0.5 + laura.getRandom().nextDouble()));
    }

    /** Picks an animation that fits her mood and surroundings and plays it. */
    public Emote spontaneousEmote() {
        if (laura.isEmoting() || laura.isAsleep() || laura.isFetching() || laura.getTarget() != null) {
            return Emote.NONE;
        }
        java.util.List<Emote> pool = new java.util.ArrayList<>();
        java.util.function.BiConsumer<Emote, Integer> add = (emote, weight) -> {
            for (int i = 0; i < weight; i++) {
                pool.add(emote);
            }
        };
        LauraWorldChecks.Climate climate = LauraWorldChecks.climate(laura);
        if (climate == LauraWorldChecks.Climate.COLD) {
            add.accept(Emote.SHIVER, 6);
        } else if (climate == LauraWorldChecks.Climate.HOT) {
            add.accept(Emote.FAN, 6);
        }
        if (laura.level().isRaining() && LauraWorldChecks.canSeeSky(laura)) {
            add.accept(Emote.SNEEZE, 4);
        }
        switch (laura.getMood()) {
            case HAPPY, IN_LOVE -> {
                add.accept(Emote.CLAP, 4);
                add.accept(Emote.SNAP, 3);
                add.accept(Emote.TWIRL, 4);
                add.accept(Emote.HUM, 4);
                add.accept(Emote.BLOW_KISS, laura.getMood() == com.vyrriox.lauramod.entity.Mood.IN_LOVE ? 5 : 2);
                add.accept(Emote.LAUGH, 2);
                add.accept(Emote.HAIR_FLIP, 3);
                add.accept(Emote.AIR_GUITAR, 2);
                add.accept(Emote.JUMP, 2);
                add.accept(Emote.WAVE, 2);
            }
            case NEUTRAL -> {
                add.accept(Emote.STRETCH, 3);
                add.accept(Emote.HUM, 3);
                add.accept(Emote.SNAP, 2);
                add.accept(Emote.HAIR_FLIP, 3);
                add.accept(Emote.CHECK_NAILS, 2);
                add.accept(Emote.CLAP, 1);
                add.accept(Emote.SNEEZE, 1);
                add.accept(Emote.HICCUP, 1);
                add.accept(Emote.WAVE, 1);
            }
            case BORED -> {
                add.accept(Emote.TAP_FOOT, 4);
                add.accept(Emote.CHECK_NAILS, 4);
                add.accept(Emote.STRETCH, 2);
                add.accept(Emote.SNAP, 2);
                add.accept(Emote.HUM, 1);
                add.accept(Emote.HICCUP, 1);
            }
            case TIRED -> {
                add.accept(Emote.YAWN, 5);
                add.accept(Emote.STRETCH, 4);
            }
            case HUNGRY -> {
                add.accept(Emote.TAP_FOOT, 3);
                add.accept(Emote.POUT, 3);
            }
            case SAD, SULKING -> {
                add.accept(Emote.POUT, 4);
                add.accept(Emote.CRY, 2);
            }
            case ANGRY, JEALOUS -> {
                add.accept(Emote.POUT, 3);
                add.accept(Emote.STOMP, 3);
                add.accept(Emote.TAP_FOOT, 3);
            }
        }
        if (pool.isEmpty()) {
            return Emote.NONE;
        }
        Emote emote = pool.get(laura.getRandom().nextInt(pool.size()));
        laura.playEmote(emote);
        if (emote == Emote.CLAP || emote == Emote.TWIRL || emote == Emote.HUM || emote == Emote.AIR_GUITAR) {
            needs.add(Needs.Need.FUN, 4);
        }
        return emote;
    }

    private void chatter(ServerPlayer owner) {
        Mood mood = laura.getMood();
        String relation = relationKey();
        String place = LauraWorldChecks.isNight(laura.level()) ? "ambient.night" : laura.level().isRaining() ? "ambient.rain" : "ambient.day";
        LauraSpeech.sayFirst(laura, owner, LineFormatter.values(), "ambient." + mood.key(), "ambient." + relation, place, "ambient");
        if (laura.getRandom().nextInt(3) == 0) {
            laura.playLauraSound(LauraRegistries.Sound.AMBIENT, 0.8F, 1.0F);
        }
    }

    /** cold, friend, close, love, soulmate: her feelings towards her partner. */
    public String relationKey() {
        int a = laura.getAffection();
        if (a < 150) {
            return "hate";
        } else if (a < 350) {
            return "cold";
        } else if (a < 550) {
            return "friend";
        } else if (a < 750) {
            return "close";
        } else if (a < 900) {
            return "love";
        }
        return "soulmate";
    }

    public void askLove(ServerPlayer owner) {
        if (laura.isGagged()) {
            return;
        }
        question = Question.LOVE;
        questionExpires = now() + LauraConfig.loveAnswerSeconds.getInt() * 20L;
        LauraSpeech.say(laura, owner, "love_check", LineFormatter.values());
        owner.sendSystemMessage(Component.literal("  ")
                .append(ChatButtons.button(Component.translatable("lauramod.answer.yes"), "/laura answer yes", ChatFormatting.GREEN))
                .append(Component.literal("  "))
                .append(ChatButtons.button(Component.translatable("lauramod.answer.no"), "/laura answer no", ChatFormatting.RED)));
    }

    /** The player's answer to her current question. Returns false when nothing was asked. */
    public boolean answer(ServerPlayer player, boolean yes) {
        if (question != Question.LOVE) {
            return false;
        }
        question = Question.NONE;
        if (yes) {
            laura.playLauraSound(LauraRegistries.Sound.HAPPY, 1.0F, 1.0F);
            laura.hearts(7);
            laura.playEmote(Emote.BLUSH);
            changeAffection(10);
            needs.add(Needs.Need.ATTENTION, 15);
            makeHappy(180);
            LauraSpeech.say(laura, player, "love_yes", LineFormatter.values());
        } else {
            laura.playLauraSound(LauraRegistries.Sound.SAD, 1.0F, 1.0F);
            laura.playEmote(Emote.CRY);
            changeAffection(-40);
            startSulking(LauraConfig.sulkMinutes.getInt() * 1200);
            LauraSpeech.say(laura, player, "love_no", LineFormatter.values());
            LauraAdvancements.award(player, "love_no");
        }
        return true;
    }

    private void fart(ServerPlayer owner) {
        laura.playLauraSound(LauraRegistries.Sound.FART, 1.0F, 0.9F + laura.getRandom().nextFloat() * 0.2F);
        if (laura.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, laura.getX(), laura.getY() + 0.5, laura.getZ(), 10, 0.3, 0.2, 0.3, 0.01);
        }
        needs.add(Needs.Need.HYGIENE, -8);
        if (laura.distanceToSqr(owner) < 64) {
            LauraSpeech.say(laura, owner, "fart", LineFormatter.values());
        }
    }

    private void weather(ServerPlayer owner) {
        boolean raining = laura.level().isRaining() && LauraWorldChecks.canSeeSky(laura);
        boolean thundering = laura.level().isThundering();
        boolean night = LauraWorldChecks.isNight(laura.level());
        if (raining && !wasRaining && ready("complain.rain", 240, false)) {
            LauraSpeech.say(laura, owner, "complain.rain", LineFormatter.values());
        }
        if (thundering && !wasThundering && ready("complain.thunder", 240, false)) {
            LauraSpeech.say(laura, owner, "complain.thunder", LineFormatter.values());
            laura.playEmote(Emote.CRY);
        }
        if (night && !wasNight && ready("complain.night", 400, false)) {
            LauraSpeech.say(laura, owner, "complain.night", LineFormatter.values());
        }
        if (!night && wasNight && ready("good_morning", 400, false)) {
            LauraSpeech.say(laura, owner, "good_morning", LineFormatter.values());
            laura.playEmote(Emote.WAVE);
        }
        wasRaining = raining;
        wasThundering = thundering;
        wasNight = night;
    }

    // ------------------------------------------------------------------ reactions

    public void onHitByOwner(ServerPlayer player) {
        long now = now();
        if (lastOwnerHit != 0 && now - lastOwnerHit < 20) {
            return;
        }
        if (now - lastOwnerHit > 600) {
            aggression = 0;
        }
        lastOwnerHit = now;
        aggression++;
        needs.add(Needs.Need.ATTENTION, -5);
        if (aggression >= LauraConfig.hitsBeforeSulking.getInt()) {
            aggression = 0;
            changeAffection(-30);
            startSulking(LauraConfig.sulkMinutes.getInt() * 1200);
            laura.playLauraSound(LauraRegistries.Sound.SAD, 1.0F, 1.0F);
            laura.playEmote(Emote.CRY);
            LauraSpeech.say(laura, player, "sad", LineFormatter.values());
        } else {
            changeAffection(-10);
            angryUntil = now + 30 * 20;
            laura.playLauraSound(LauraRegistries.Sound.ANGRY, 1.0F, 1.0F);
            LauraSpeech.say(laura, player, "angry", LineFormatter.values());
        }
        laura.spawnParticles(ParticleTypes.ANGRY_VILLAGER, 4, 0.4);
    }

    public void onHitByOther(ServerPlayer attacker) {
        ServerPlayer owner = owner();
        if (owner != null && ready("hit_by_other", 20, false)) {
            LauraSpeech.say(laura, owner, "hit_by_other", LineFormatter.values().with("attacker", attacker.getName()));
        }
    }

    public void onHurtByMob(LivingEntity mob) {
        ServerPlayer owner = owner();
        if (owner != null && owner.distanceToSqr(laura) < 32 * 32 && ready("help", 30, false)) {
            LauraSpeech.say(laura, owner, "help", LineFormatter.values().with("attacker", mob.getName()));
        }
    }

    /** Right click with an empty hand (server side). */
    public void onInteraction(ServerPlayer player) {
        if (ready("interaction", 15, false)) {
            needs.add(Needs.Need.ATTENTION, 3);
        }
    }

    public void onLookChanged() {
        ServerPlayer owner = owner();
        onActivity(DesireType.Activity.NEW_OUTFIT);
        if (owner != null && ready("new_look", 10, false)) {
            LauraSpeech.say(laura, owner, "new_look", LineFormatter.values());
            laura.playEmote(Emote.BLUSH);
        }
    }

    /** Owner apologized. */
    public void apologize(ServerPlayer player) {
        if (!isSulking() && now() >= angryUntil) {
            LauraSpeech.say(laura, player, "apology_confused", LineFormatter.values());
            return;
        }
        if (LauraConfig.annoyance.get().refusal > 0 && laura.getRandom().nextDouble() < LauraConfig.annoyance.get().refusal && ready("apology_refuse", 20, false)) {
            LauraSpeech.say(laura, player, "apology_refused", LineFormatter.values());
            return;
        }
        stopSulking();
        angryUntil = 0;
        laura.playLauraSound(LauraRegistries.Sound.HAPPY, 1.0F, 1.0F);
        laura.playEmote(Emote.HUG);
        laura.hearts(3);
        LauraSpeech.say(laura, player, "apology_accept", LineFormatter.values());
        LauraAdvancements.award(player, "apology");
    }

    /**
     * Unhappy Laura sometimes refuses an order. Asking twice in a row always works. Returns true if
     * she refuses (she already said so).
     */
    public boolean refuses(ServerPlayer player, String order) {
        if (!LauraConfig.refuseOrders.get() || !laura.getMood().isGrumpy()) {
            return false;
        }
        double chance = LauraConfig.annoyance.get().refusal;
        if (isSulking()) {
            chance = Math.min(0.9, chance * 2);
        }
        long now = now();
        if (order.equals(lastRefused) && now - lastRefusedTime < 20 * 20) {
            lastRefused = "";
            LauraSpeech.say(laura, player, "order.fine", LineFormatter.values());
            return false;
        }
        if (laura.getRandom().nextDouble() >= chance) {
            return false;
        }
        lastRefused = order;
        lastRefusedTime = now;
        LauraSpeech.sayFirst(laura, player, LineFormatter.values(), "order.refused." + laura.getMood().key(), "order.refused");
        return true;
    }

    /** Talking to her in chat (any message from her partner). */
    public void onChatFromOwner() {
        if (ready("chat_attention", 8, false)) {
            needs.add(Needs.Need.ATTENTION, 4);
        }
    }

    public void onCompliment() {
        needs.add(Needs.Need.ATTENTION, 12);
        needs.add(Needs.Need.FUN, 5);
        changeAffection(3);
        laura.playEmote(Emote.BLUSH);
        onActivity(DesireType.Activity.COMPLIMENT);
    }

    public void onInsult(ServerPlayer player) {
        changeAffection(-15);
        angryUntil = now() + 60 * 20;
        laura.playLauraSound(LauraRegistries.Sound.ANGRY, 1.0F, 1.0F);
        laura.playEmote(Emote.STOMP);
    }

    public void onHug(ServerPlayer player) {
        needs.add(Needs.Need.ATTENTION, 20);
        needs.add(Needs.Need.FUN, 5);
        changeAffection(isSulking() ? 1 : 4);
        onActivity(DesireType.Activity.HUG);
    }

    public void onKiss(ServerPlayer player) {
        needs.add(Needs.Need.ATTENTION, 20);
        needs.add(Needs.Need.FUN, 8);
        changeAffection(isSulking() ? 1 : 5);
        onActivity(DesireType.Activity.KISS);
    }

    public void onDance() {
        needs.add(Needs.Need.FUN, 15);
        onActivity(DesireType.Activity.DANCE);
    }

    public void onFirework() {
        needs.add(Needs.Need.FUN, 10);
        onActivity(DesireType.Activity.FIREWORKS);
    }

    public void onOwnerSleeping() {
        onActivity(DesireType.Activity.SLEEP_TOGETHER);
    }

    public OwnerWatcher watcher() {
        return watcher;
    }

    void setJealous(int seconds) {
        jealousUntil = now() + seconds * 20L;
    }

    /** Resets everything to "freshly summoned". */
    public void resetForSummon() {
        needs.fillAll();
        needs.set(Needs.Need.HUNGER, 80);
        desire = null;
        nextDesireTime = -1;
        sulkUntil = 0;
        angryUntil = 0;
        jealousUntil = 0;
        makeHappy(120);
    }

    // ------------------------------------------------------------------ persistence

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Needs", needs.save());
        if (desire != null) {
            tag.put("Desire", desire.save());
        }
        tag.putLong("NextDesire", nextDesireTime);
        long now = now();
        tag.putLong("SulkLeft", Math.max(0, sulkUntil - now));
        tag.putLong("AngryLeft", Math.max(0, angryUntil - now));
        tag.putLong("HappyLeft", Math.max(0, happyUntil - now));
        tag.putInt("LastAnniversary", lastAnniversary);
        tag.putLong("NextPlayfulHit", nextPlayfulHit < 0 ? -1 : Math.max(0, nextPlayfulHit - now));
        CompoundTag rewards = new CompoundTag();
        rewardCooldowns.forEach((key, until) -> {
            if (until > now) {
                rewards.putLong(key, until - now);
            }
        });
        tag.put("GiftCooldowns", rewards);
        return tag;
    }

    public void load(CompoundTag tag) {
        needs.load(tag.getCompound("Needs"));
        desire = tag.contains("Desire") ? Desire.load(tag.getCompound("Desire")) : null;
        nextDesireTime = tag.contains("NextDesire") ? tag.getLong("NextDesire") : -1;
        long now = now();
        sulkUntil = now + tag.getLong("SulkLeft");
        angryUntil = now + tag.getLong("AngryLeft");
        happyUntil = now + tag.getLong("HappyLeft");
        lastAnniversary = tag.contains("LastAnniversary") ? tag.getInt("LastAnniversary") : -1;
        long hit = tag.contains("NextPlayfulHit") ? tag.getLong("NextPlayfulHit") : -1;
        nextPlayfulHit = hit < 0 ? -1 : now + hit;
        rewardCooldowns.clear();
        CompoundTag rewards = tag.getCompound("GiftCooldowns");
        for (String key : rewards.getAllKeys()) {
            rewardCooldowns.put(key, now + rewards.getLong(key));
        }
    }

    /** Used by chat and the menu: which need is the most urgent, and how urgent. */
    public Component describeState(Player viewer) {
        Needs.Need lowest = needs.lowest();
        return Component.translatable("lauramod.need." + lowest.key()).append(": " + Math.round(needs.get(lowest)) + "%");
    }

    /** True when she really wants to sleep now. */
    public boolean wantsToSleep() {
        if (!LauraConfig.needsEnabled.get()) {
            return LauraWorldChecks.isNight(laura.level()) && laura.getMode() == LauraMode.HOME && LauraConfig.sleepAtNight.get();
        }
        float energy = needs.get(Needs.Need.ENERGY);
        if (energy < 8) {
            return true;
        }
        boolean night = LauraWorldChecks.isNight(laura.level());
        if (!night) {
            return false;
        }
        ServerPlayer owner = owner();
        if (owner != null && owner.isSleeping() && owner.distanceToSqr(laura) < 24 * 24) {
            return true;
        }
        return laura.getMode() == LauraMode.HOME && LauraConfig.sleepAtNight.get() && energy < 95
                || laura.getMode() == LauraMode.STAY && energy < 40;
    }

    /** True when she can wake up. */
    public boolean canWakeUp() {
        ServerPlayer owner = owner();
        if (owner != null && owner.isSleeping()) {
            return false;
        }
        float energy = LauraConfig.needsEnabled.get() ? needs.get(Needs.Need.ENERGY) : 100;
        boolean night = LauraWorldChecks.isNight(laura.level());
        if (energy >= 98) {
            return true;
        }
        return !night && energy > 40;
    }

    /**
     * True when a sleep she was ordered to take is over. Unlike {@link #canWakeUp()}, daylight alone
     * does not end it: an ordered nap lasts until she is rested (or, when needs are off, until it is
     * day and she has slept three minutes).
     */
    public boolean orderedSleepOver() {
        ServerPlayer owner = owner();
        if (owner != null && owner.isSleeping()) {
            return false;
        }
        if (LauraConfig.needsEnabled.get()) {
            return needs.get(Needs.Need.ENERGY) >= 98;
        }
        return !LauraWorldChecks.isNight(laura.level()) && now() - laura.sleepOrderedAt() >= 20 * 180;
    }

    public ItemSpec desiredItem() {
        return desire == null ? null : desire.itemSpec();
    }
}
