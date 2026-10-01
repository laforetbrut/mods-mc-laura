package com.vyrriox.lauramod.entity.brain;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.gift.GiftTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Watches what her partner does and makes Laura comment on it: going AFK, eating alone, finding
 * diamonds, flying, swimming in lava, getting hurt, chatting with other players...
 *
 * @author vyrriox
 */
public final class OwnerWatcher {
    private static final List<Item> NOTABLE = List.of(Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT,
            Items.GOLD_INGOT, Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING, Items.CAKE, Items.ECHO_SHARD, Items.HEART_OF_THE_SEA);

    private final LauraBrain brain;
    private final Map<Item, Integer> counts = new HashMap<>();
    private Vec3 lastPos;
    private float lastYaw;
    private long afkSince = -1;
    private boolean afkAnnounced;
    private int lastFood = -1;
    private boolean initialized;

    OwnerWatcher(LauraBrain brain) {
        this.brain = brain;
    }

    void tick(ServerPlayer owner, boolean near, long now) {
        LauraEntity laura = brain.laura();
        if (!initialized) {
            snapshot(owner);
            initialized = true;
            return;
        }
        boolean comments = LauraConfig.commentActivities.get() && !laura.isAsleep();

        // AFK detection
        Vec3 pos = owner.position();
        boolean still = lastPos != null && lastPos.distanceToSqr(pos) < 0.01 && Math.abs(lastYaw - owner.getYRot()) < 0.5F;
        if (still) {
            if (afkSince < 0) {
                afkSince = now;
            }
            if (!afkAnnounced && now - afkSince > LauraConfig.afkMinutes.getInt() * 1200L && near && comments) {
                afkAnnounced = true;
                LauraSpeech.say(laura, owner, "owner.afk", LineFormatter.values());
                brain.poke(owner);
            }
        } else {
            if (afkAnnounced && near && comments) {
                LauraSpeech.say(laura, owner, "owner.back_from_afk", LineFormatter.values());
            }
            afkSince = -1;
            afkAnnounced = false;
        }
        lastPos = pos;
        lastYaw = owner.getYRot();

        // Low health
        boolean hurt = owner.getHealth() / owner.getMaxHealth() < LauraConfig.ownerHealthThreshold.getDouble();
        laura.setWorried(hurt && near);
        if (hurt && near && LauraConfig.worryWhenOwnerHurt.get() && brain.ready("owner_hurt", 45, false)) {
            LauraSpeech.say(laura, owner, "owner.hurt", LineFormatter.values());
            if (LauraConfig.feedOwner.get() && owner.getFoodData().getFoodLevel() < 18) {
                feed(owner);
            }
        }

        // Eating without her
        int food = owner.getFoodData().getFoodLevel();
        if (lastFood >= 0 && food > lastFood && near && comments && brain.ready("owner_ate", 90)) {
            ItemStack eaten = owner.getMainHandItem().isEmpty() ? owner.getOffhandItem() : owner.getMainHandItem();
            LineFormatter.Values values = LineFormatter.values().with("item", eaten.isEmpty() ? net.minecraft.network.chat.Component.literal("...") : eaten.getHoverName());
            LauraSpeech.say(laura, owner, "owner.ate_alone", values);
        }
        lastFood = food;

        // Notable items found
        if (near && comments) {
            for (Item item : NOTABLE) {
                int count = owner.getInventory().countItem(item);
                Integer before = counts.get(item);
                if (before != null && count > before && brain.ready("found." + BuiltInRegistries.ITEM.getKey(item).getPath(), 60)) {
                    String key = "owner.found." + BuiltInRegistries.ITEM.getKey(item).getPath();
                    LineFormatter.Values values = LineFormatter.values().with("item", new ItemStack(item).getHoverName());
                    if (!LauraSpeech.say(laura, owner, key, values)) {
                        LauraSpeech.say(laura, owner, "owner.found.generic", values);
                    }
                    laura.playEmote(Emote.JUMP);
                    if (brain.desire() == null && LauraConfig.desiresEnabled.get() && laura.getRandom().nextInt(3) == 0) {
                        brain.forceDesire(new Desire(DesireType.Kind.ITEM, BuiltInRegistries.ITEM.getKey(item).toString(), false,
                                now, now + LauraConfig.desireDeadlineMinutes.getInt() * 1200L));
                    }
                }
                counts.put(item, count);
            }
        }

        if (!comments || !near) {
            return;
        }
        if (owner.isFallFlying() && brain.ready("owner_flying", 120)) {
            LauraSpeech.say(laura, owner, "owner.flying", LineFormatter.values());
        }
        if (owner.isInLava() && brain.ready("owner_lava", 30, false)) {
            LauraSpeech.say(laura, owner, "owner.lava", LineFormatter.values());
            laura.playEmote(Emote.FACEPALM);
        }
        if (owner.isOnFire() && !owner.isInLava() && brain.ready("owner_fire", 60, false)) {
            LauraSpeech.say(laura, owner, "owner.fire", LineFormatter.values());
        }
        if (owner.isPassenger() && laura.getVehicle() == null && brain.ready("owner_riding", 180)) {
            LauraSpeech.say(laura, owner, "owner.riding", LineFormatter.values());
        }
        if (LauraConfig.jealousOfPlayers.get()) {
            for (ServerPlayer other : owner.serverLevel().players()) {
                if (other != owner && !other.isSpectator() && other.distanceToSqr(owner) < 9 && brain.ready("jealous_player", 240)) {
                    brain.setJealous(60);
                    LauraSpeech.say(laura, owner, "jealous.player", LineFormatter.values().with("other", other.getName()));
                    com.vyrriox.lauramod.world.LauraAdvancements.award(owner, "jealous");
                    laura.playEmote(Emote.POUT);
                    break;
                }
            }
        }
    }

    private void snapshot(ServerPlayer owner) {
        lastPos = owner.position();
        lastYaw = owner.getYRot();
        lastFood = owner.getFoodData().getFoodLevel();
        for (Item item : NOTABLE) {
            counts.put(item, owner.getInventory().countItem(item));
        }
    }

    /** She gives her partner some of her food when they are hurt and hungry. */
    private void feed(ServerPlayer owner) {
        LauraEntity laura = brain.laura();
        var inventory = laura.inventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED && stack.getItem() != Items.CAKE) {
                ItemStack given = stack.split(1);
                inventory.setChanged();
                brain.giveToPlayer(owner, given);
                LauraSpeech.say(laura, owner, "owner.feed", LineFormatter.values().with("item", given.getHoverName()));
                return;
            }
        }
    }

    /** Her partner broke a block near her. */
    public void onBlockBroken(ServerPlayer owner, BlockState state) {
        LauraEntity laura = brain.laura();
        if (!LauraConfig.commentActivities.get() || laura.isAsleep() || laura.distanceToSqr(owner) > 16 * 16) {
            return;
        }
        if (brain.ready("comment_mining", 300) && laura.getRandom().nextInt(4) == 0) {
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            String key = path.contains("diamond_ore") ? "owner.mined.diamond"
                    : path.contains("_ore") ? "owner.mined.ore"
                    : path.contains("log") || path.contains("wood") ? "owner.mined.wood"
                    : path.contains("flower") || path.contains("tulip") || path.contains("rose") ? "owner.mined.flower"
                    : "owner.mined.generic";
            LauraSpeech.sayFirst(laura, owner, LineFormatter.values().with("item", state.getBlock().getName()), key, "owner.mined.generic");
        }
    }

    /** Her partner killed something near her. */
    public void onKill(ServerPlayer owner, LivingEntity victim) {
        LauraEntity laura = brain.laura();
        if (!LauraConfig.commentActivities.get() || laura.isAsleep() || laura.distanceToSqr(owner) > 24 * 24) {
            return;
        }
        String key;
        if (victim instanceof Enemy) {
            key = "owner.killed.monster";
            if (!brain.ready("comment_kill_monster", 120)) {
                return;
            }
        } else if (victim instanceof Animal) {
            key = "owner.killed.animal";
            if (!brain.ready("comment_kill_animal", 120)) {
                return;
            }
        } else if (victim instanceof ServerPlayer) {
            key = "owner.killed.player";
        } else {
            key = "owner.killed.generic";
            if (!brain.ready("comment_kill", 180)) {
                return;
            }
        }
        LauraSpeech.say(laura, owner, key, LineFormatter.values().with("victim", victim.getName()));
    }

    /** Her partner wrote something in chat that was not for her while other players are online. */
    public void onChatWithOthers(ServerPlayer owner) {
        LauraEntity laura = brain.laura();
        if (!LauraConfig.jealousOfPlayers.get() || laura.distanceToSqr(owner) > 32 * 32) {
            return;
        }
        if (brain.ready("jealous_chat", 180) && laura.getRandom().nextInt(3) == 0) {
            brain.setJealous(45);
            LauraSpeech.say(laura, owner, "jealous.chat", LineFormatter.values());
        }
    }
}
