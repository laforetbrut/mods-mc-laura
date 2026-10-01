package com.vyrriox.lauramod.world;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.cooking.CookingSupport;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.dialogue.TextMatcher;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraBags;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.ai.FetchGoal;
import com.vyrriox.lauramod.entity.ai.LauraMovement;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.entity.work.LauraWorkplace;
import com.vyrriox.lauramod.entity.work.WorkArea;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every order Laura can receive, whatever its source (menu, chat, command). Permission checks,
 * refusals (she can be moody), and the dialogue answers all happen here.
 *
 * @author vyrriox
 */
public final class LauraActions {
    public enum Source {
        MENU, CHAT, COMMAND
    }

    private LauraActions() {
    }

    public static boolean canCommand(ServerPlayer player, LauraEntity laura) {
        return laura.isOwnedBy(player) || LauraConfig.othersCanInteract.get() || player.permissions().hasPermission(LauraMod.permission(2));
    }

    /** Orders she might refuse when she is in a bad mood. */
    private static boolean isRefusable(LauraAction action) {
        return switch (action) {
            case FOLLOW, STAY, WANDER, HOME, COME, FETCH, EMOTE, HUG, KISS, SLEEP, EAT -> true;
            default -> false;
        };
    }

    public static void perform(ServerPlayer player, LauraEntity laura, LauraAction action, String arg, Source source) {
        if (!canCommand(player, laura)) {
            LauraSpeech.refuse(laura, player);
            return;
        }
        String a = arg == null ? "" : arg.trim();
        if (source == Source.MENU && (action == LauraAction.TASK || action == LauraAction.FETCH) && laura.workplace().isRepeatedRequest(action.name() + "|" + a)) {
            // The same button twice within half a second is one click: one task, one answer.
            return;
        }
        if (isRefusable(action) && laura.brain().refuses(player, action.name())) {
            return;
        }
        switch (action) {
            case FOLLOW -> {
                clearMovementTasks(laura);
                laura.wakeUp();
                laura.setMode(LauraMode.FOLLOW);
                LauraSpeech.say(laura, player, "order.follow", LineFormatter.values());
            }
            case STAY -> {
                laura.setMode(LauraMode.STAY);
                LauraSpeech.say(laura, player, "order.stay", LineFormatter.values());
            }
            case WANDER -> {
                laura.wakeUp();
                laura.setMode(LauraMode.WANDER);
                LauraSpeech.say(laura, player, "order.wander", LineFormatter.values());
            }
            case HOME -> goHome(player, laura);
            case SET_HOME -> setHome(player, laura);
            case CLEAR_HOME -> {
                laura.setHome(null, null);
                if (laura.getMode() == LauraMode.HOME) {
                    laura.setMode(LauraMode.FOLLOW);
                }
                LauraSpeech.say(laura, player, "home.cleared", LineFormatter.values());
            }
            case COME -> come(player, laura);
            case FETCH -> {
                // "item", or "item|count" and "item|count|queue" from the menu. The limit keeps the
                // empty parts, so an argument made of separators only still has a first part.
                String[] parts = a.split("\\|", -1);
                fetch(player, laura, parts[0], parts.length > 1 ? parseInt(parts[1], 0) : 0, parts.length > 2 && parts[2].equalsIgnoreCase("queue"));
            }
            case JOB -> {
                String[] parts = a.split(":", -1);
                LauraJob job = LauraJob.byName(parts[0]);
                boolean on = parts.length > 1 && parts[1].equalsIgnoreCase("on");
                if (on && job != null && job != LauraJob.NONE) {
                    enableJob(player, laura, job, parts.length > 2 ? parseInt(parts[2], 0) : 0);
                } else {
                    disableJob(player, laura, job == null || job == LauraJob.NONE ? null : job);
                }
            }
            case WORK -> backToWork(player, laura);
            case TASK -> {
                String[] parts = a.split(":", -1);
                LauraTask.Type type = LauraTask.Type.byName(parts[0]);
                if (type == null || !type.isErrand() || type == LauraTask.Type.FETCH) {
                    LauraSpeech.say(laura, player, "confused", LineFormatter.values());
                } else {
                    task(player, laura, type, parts.length > 2 ? parseInt(parts[2], 0) : 0, parts.length > 1 && parts[1].equalsIgnoreCase("queue"));
                }
            }
            case QUEUE -> {
                if (a.equalsIgnoreCase("clear")) {
                    laura.workplace().clearQueue();
                    laura.workGoal().reset();
                    player.sendSystemMessage(Component.translatable("lauramod.queue.cleared"));
                } else if (a.startsWith("remove:")) {
                    boolean ok = laura.workplace().removeQueued(parseInt(a.substring(7), 0) - 1);
                    player.sendSystemMessage(Component.translatable(ok ? "lauramod.queue.removed" : "lauramod.queue.invalid"));
                }
            }
            case CHEST -> {
                if (a.equalsIgnoreCase("remove")) {
                    assignChest(player, laura, null);
                } else {
                    ChestPurpose purpose = ChestPurpose.byName(a);
                    if (purpose == null) {
                        LauraSpeech.say(laura, player, "confused", LineFormatter.values());
                    } else {
                        assignChest(player, laura, purpose);
                    }
                }
            }
            case EMOTE -> emote(player, laura, Emote.byName(a));
            case HUG -> hug(player, laura);
            case KISS -> kiss(player, laura);
            case COMPLIMENT -> {
                laura.brain().onCompliment();
                LauraSpeech.respond(laura, player, "beautiful", LineFormatter.values());
            }
            case UNGAG -> ungag(player, laura, false);
            case INVENTORY -> laura.openInventory(player);
            case COMBAT -> {
                CombatMode mode = parseCombat(a, laura.getCombatMode());
                laura.setCombatMode(mode);
                LauraSpeech.say(laura, player, "combat." + mode.key(), LineFormatter.values());
            }
            case PICKUP -> {
                boolean on = a.isEmpty() ? !laura.isPickingUpItems() : a.equalsIgnoreCase("on") || a.equalsIgnoreCase("true");
                laura.setPickingUpItems(on);
                LauraSpeech.say(laura, player, on ? "pickup.on" : "pickup.off", LineFormatter.values());
            }
            case INFO -> sendInfo(player, laura);
            case ANSWER -> {
                if (!laura.brain().answer(player, a.equalsIgnoreCase("yes") || a.equalsIgnoreCase("oui"))) {
                    LauraSpeech.say(laura, player, "confused", LineFormatter.values());
                }
            }
            case EAT -> {
                if (!laura.brain().hasFood()) {
                    LauraSpeech.say(laura, player, "need.hunger.no_food", LineFormatter.values());
                } else {
                    eatBestFood(player, laura);
                }
            }
            case STOP -> {
                laura.fetchGoal().cancel(true);
                laura.workplace().clearQueue();
                laura.workGoal().reset();
                laura.playEmote(Emote.NONE);
                laura.getNavigation().stop();
                LauraSpeech.say(laura, player, "order.stop", LineFormatter.values());
            }
            case RENAME -> rename(player, laura, a);
            case SLEEP -> {
                if (LauraConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.ENERGY) > 85 && !LauraWorldChecks.isNight(laura.level())) {
                    LauraSpeech.say(laura, player, "sleep.not_tired", LineFormatter.values());
                } else {
                    laura.goToSleep(laura.findFreeBed(laura.blockPosition(), 8));
                    // An order: the sleep goal ends it when she is rested, whatever her mode.
                    laura.markSleepOrdered();
                    LauraSpeech.say(laura, player, "sleep.good_night", LineFormatter.values());
                }
            }
            case WAKE_UP -> {
                if (laura.isAsleep()) {
                    laura.wakeUp();
                    LauraSpeech.say(laura, player, "sleep.woken_up", LineFormatter.values());
                    laura.brain().changeAffection(-2);
                }
            }
        }
        LauraNetwork.sendStatus(player, laura);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void clearMovementTasks(LauraEntity laura) {
        laura.fetchGoal().cancel(true);
    }

    private static CombatMode parseCombat(String a, CombatMode current) {
        for (CombatMode mode : CombatMode.values()) {
            if (mode.name().equalsIgnoreCase(a)) {
                return mode;
            }
        }
        return CombatMode.byId((current.ordinal() + 1) % CombatMode.values().length);
    }

    // ------------------------------------------------------------------ movement orders

    public static void setHome(ServerPlayer player, LauraEntity laura) {
        laura.setHome(player.blockPosition(), player.level().dimension());
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.6, 0.4, 0.6, 0.0);
        }
        LauraSpeech.say(laura, player, "home.set", LineFormatter.values());
    }

    public static void goHome(ServerPlayer player, LauraEntity laura) {
        BlockPos home = laura.getHomePos();
        if (home == null) {
            LauraSpeech.say(laura, player, "home.none", LineFormatter.values());
            return;
        }
        laura.wakeUp();
        laura.fetchGoal().cancel(true);
        laura.setMode(LauraMode.HOME);
        LauraSpeech.say(laura, player, "home.go", LineFormatter.values());
        ServerLevel homeLevel = player.level().getServer().getLevel(laura.getHomeDimension());
        if (homeLevel == null) {
            return;
        }
        boolean otherDimension = homeLevel != laura.level();
        double far = LauraConfig.homeTeleportDistance.getInt();
        if (otherDimension || laura.blockPosition().distSqr(home) > far * far) {
            LauraManager.teleport(laura, homeLevel, home);
        }
    }

    public static void come(ServerPlayer player, LauraEntity laura) {
        laura.wakeUp();
        laura.setMode(LauraMode.FOLLOW);
        double far = LauraConfig.teleportDistance.getDouble();
        if (laura.level() != player.level() || laura.distanceToSqr(player) > far * far) {
            LauraManager.teleport(laura, player.level(), player.blockPosition());
        } else {
            // A call is an order: when no path leads to the player at all, she comes anyway.
            // Beyond her path range a partial path is normal: she walks it and searches again.
            double range = laura.getAttributeValue(Attributes.FOLLOW_RANGE);
            Path path = laura.getNavigation().createPath(player, 1);
            boolean walkable = path != null && (path.canReach() || laura.distanceToSqr(player) > range * range);
            if (walkable) {
                laura.getNavigation().moveTo(path, 1.2);
            } else {
                LauraManager.teleport(laura, player.level(), player.blockPosition());
            }
        }
        LauraSpeech.say(laura, player, "order.come", LineFormatter.values());
    }

    // ------------------------------------------------------------------ fetch

    /**
     * Starts (or queues) a fetch. {@code what} is an item id, a #tag, "held" (the item in the player's
     * hand), or free text matched against the item keywords of the dialogue files.
     */
    public static void fetch(ServerPlayer player, LauraEntity laura, String what, int count, boolean queue) {
        ItemSpec spec = resolveItem(player, what);
        if (spec == null) {
            LauraSpeech.say(laura, player, "fetch.what", LineFormatter.values());
            return;
        }
        int amount = count > 0 ? count : spec.item() != null ? Math.min(spec.icon().getMaxStackSize(), 16) : 16;
        if (queue || laura.workplace().current() != null) {
            if (laura.workplace().enqueue(new LauraTask(LauraTask.Type.FETCH, spec.raw(), amount, null))) {
                LauraSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", Component.translatable("lauramod.task.fetch")));
            } else {
                LauraSpeech.say(laura, player, "task.queue_full", LineFormatter.values());
            }
            return;
        }
        startFetch(player, laura, spec, amount);
    }

    private static void startFetch(ServerPlayer player, LauraEntity laura, ItemSpec spec, int amount) {
        FetchGoal.Result result = laura.fetchGoal().request(player, spec, amount);
        switch (result) {
            case STARTED -> LauraSpeech.say(laura, player, "fetch.start", LineFormatter.values().with("item", spec.displayName()).with("count", amount));
            case DISABLED -> LauraSpeech.say(laura, player, "fetch.disabled", LineFormatter.values());
            case BUSY -> LauraSpeech.say(laura, player, "fetch.busy", LineFormatter.values());
            case INVALID -> LauraSpeech.say(laura, player, "fetch.what", LineFormatter.values());
        }
    }

    @Nullable
    public static ItemSpec resolveItem(ServerPlayer player, String what) {
        String w = what == null ? "" : what.trim();
        if (w.isEmpty() || w.equalsIgnoreCase("held") || w.equalsIgnoreCase("this")) {
            ItemStack held = player.getMainHandItem();
            return held.isEmpty() ? null : ItemSpec.of(held.getItem());
        }
        Optional<ItemSpec> direct = ItemSpec.parse(w.toLowerCase(Locale.ROOT).replace(' ', '_'));
        if (direct.isPresent()) {
            return direct.get();
        }
        List<String> keywords = DialogueManager.matchItems(TextMatcher.Prepared.of(w), player.clientInformation().language());
        for (String keyword : keywords) {
            Optional<ItemSpec> spec = ItemSpec.parse(keyword);
            if (spec.isPresent()) {
                return spec.get();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ tasks and jobs

    /** Adds an errand. With {@code queue} false it replaces the current errand. */
    public static void task(ServerPlayer player, LauraEntity laura, LauraTask.Type type, int radius, boolean queue) {
        if (type.isErrand() && !LauraConfig.workEnabled.get()) {
            LauraSpeech.say(laura, player, "work.disabled", LineFormatter.values());
            return;
        }
        if (type.isErrand() && !mobGriefingAllowed(laura) && type != LauraTask.Type.COOK) {
            LauraSpeech.say(laura, player, "work.no_griefing", LineFormatter.values());
            return;
        }
        int r = Math.max(3, Math.min(LauraConfig.workMaxRadius.getInt(), radius > 0 ? radius : LauraConfig.workRadius.getInt()));
        WorkArea area = new WorkArea(player.blockPosition(), r, player.level().dimension());
        LauraTask task = new LauraTask(type, "", 0, area);
        LauraWorkplace work = laura.workplace();
        if (queue && (work.current() != null || laura.fetchGoal().isActive())) {
            if (work.enqueue(task)) {
                LauraSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", task.describe()));
                if (work.queued().size() >= 5) {
                    LauraAdvancements.award(player, "queue_5");
                }
            } else {
                LauraSpeech.say(laura, player, "task.queue_full", LineFormatter.values());
            }
            return;
        }
        laura.workGoal().reset();
        // A direct order is tried right away, even when the last one of its kind found nothing.
        laura.workGoal().forgetFailures(type);
        laura.wakeUp();
        laura.setOrderedToSit(false);
        work.replaceCurrent(task);
        LauraSpeech.say(laura, player, "task.start." + type.key(), LineFormatter.values());
    }

    public static void enableJob(ServerPlayer player, LauraEntity laura, LauraJob job, int radius) {
        if (!LauraConfig.workEnabled.get()) {
            LauraSpeech.say(laura, player, "work.disabled", LineFormatter.values());
            return;
        }
        if (job != LauraJob.COOK && !mobGriefingAllowed(laura)) {
            LauraSpeech.say(laura, player, "work.no_griefing", LineFormatter.values());
            return;
        }
        int r = Math.max(3, Math.min(LauraConfig.workMaxRadius.getInt(), radius > 0 ? radius : LauraConfig.workRadius.getInt()));
        laura.workplace().enableJob(job, new WorkArea(player.blockPosition(), r, player.level().dimension()));
        laura.workGoal().reset();
        laura.wakeUp();
        laura.setMode(LauraMode.WORK);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.3, player.getZ(), 20, r * 0.4, 0.2, r * 0.4, 0.0);
        }
        LauraSpeech.say(laura, player, "job.start." + job.key(), LineFormatter.values().with("radius", r));
        LauraAdvancements.award(player, "job");
    }

    public static void disableJob(ServerPlayer player, LauraEntity laura, @Nullable LauraJob job) {
        if (job == null) {
            laura.workplace().clearJobs();
        } else {
            laura.workplace().disableJob(job);
        }
        laura.workGoal().reset();
        if (!laura.workplace().hasJobs() && laura.getMode() == LauraMode.WORK) {
            laura.setMode(LauraMode.FOLLOW);
        }
        LauraSpeech.say(laura, player, "job.stop", LineFormatter.values());
    }

    public static void backToWork(ServerPlayer player, LauraEntity laura) {
        if (!laura.workplace().hasJobs()) {
            LauraSpeech.say(laura, player, "job.none", LineFormatter.values());
            return;
        }
        laura.wakeUp();
        laura.setMode(LauraMode.WORK);
        LauraSpeech.say(laura, player, "job.resume", LineFormatter.values());
    }

    private static boolean mobGriefingAllowed(LauraEntity laura) {
        return laura.level() instanceof ServerLevel level && level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
    }

    /** Called every 5 ticks: starts the next queued task and runs instant ones. */
    public static void processQueue(LauraEntity laura) {
        LauraWorkplace work = laura.workplace();
        LauraTask current = work.current();
        if (current == null) {
            if (laura.fetchGoal().isActive() || work.queued().isEmpty()) {
                return;
            }
            // An errand whose kind just found nothing to do waits its turn without blocking the others.
            current = work.advance(task -> !laura.workGoal().isWaiting(task));
            if (current == null) {
                return;
            }
            laura.workGoal().reset();
            ServerPlayer owner = LauraSpeech.owner(laura);
            if (current.type() == LauraTask.Type.FETCH) {
                ItemSpec spec = ItemSpec.parse(current.arg()).orElse(null);
                if (owner != null && spec != null) {
                    startFetch(owner, laura, spec, current.count());
                }
                work.finishCurrent();
                return;
            }
            // A new attempt at an errand that already said it found nothing starts without a word.
            if (owner != null && current.type().isErrand() && !laura.workGoal().isQuiet(current)) {
                LauraSpeech.say(laura, owner, "task.start." + current.type().key(), LineFormatter.values());
            }
        }
        if (!current.type().isErrand()) {
            ServerPlayer owner = LauraSpeech.owner(laura);
            work.finishCurrent();
            if (owner == null) {
                return;
            }
            switch (current.type()) {
                case COME -> come(owner, laura);
                case GO_HOME -> goHome(owner, laura);
                case FOLLOW -> {
                    laura.setMode(LauraMode.FOLLOW);
                    LauraSpeech.say(laura, owner, "order.follow", LineFormatter.values());
                }
                case STAY -> {
                    laura.setMode(LauraMode.STAY);
                    LauraSpeech.say(laura, owner, "order.stay", LineFormatter.values());
                }
                case WORK -> backToWork(owner, laura);
                default -> {
                }
            }
        }
    }

    // ------------------------------------------------------------------ chests

    /** Assigns the container the player is looking at. {@code purpose} null removes the assignment. */
    public static void assignChest(ServerPlayer player, LauraEntity laura, @Nullable ChestPurpose purpose) {
        BlockPos pos = lookedAtContainer(player);
        if (pos == null) {
            LauraSpeech.say(laura, player, "chest.look_at_one", LineFormatter.values());
            return;
        }
        if (purpose == null) {
            if (laura.workplace().unassign(pos, player.level().dimension())) {
                LauraSpeech.say(laura, player, "chest.removed", LineFormatter.values());
            } else {
                LauraSpeech.say(laura, player, "chest.not_assigned", LineFormatter.values());
            }
            return;
        }
        if (com.vyrriox.lauramod.platform.LauraInventories.isLocked(laura, pos)
                || !com.vyrriox.lauramod.platform.LauraInventories.playerMayUse(player, player.level(), pos)) {
            // A chest the player could not open (locked, protected, claimed by someone else) is not hers to use.
            LauraSpeech.say(laura, player, "chest.not_assigned", LineFormatter.values());
            return;
        }
        if (!laura.workplace().assign(pos, player.level().dimension(), purpose)) {
            LauraSpeech.say(laura, player, "chest.too_many", LineFormatter.values());
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.0);
        }
        LauraSpeech.say(laura, player, "chest.assigned", LineFormatter.values().with("purpose", Component.translatable("lauramod.chest." + purpose.key())));
        java.util.Set<ChestPurpose> known = java.util.EnumSet.noneOf(ChestPurpose.class);
        laura.workplace().chests().forEach(c -> known.add(c.purpose()));
        if (known.size() == ChestPurpose.values().length) {
            LauraAdvancements.award(player, "chests_all");
        }
    }

    @Nullable
    public static BlockPos lookedAtContainer(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(6.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return com.vyrriox.lauramod.platform.LauraInventories.at(player.level(), pos) != null ? pos.immutable() : null;
    }

    // ------------------------------------------------------------------ affection actions

    public static void emote(ServerPlayer player, LauraEntity laura, Emote emote) {
        if (emote == Emote.NONE || !emote.playerTriggered) {
            LauraSpeech.say(laura, player, "confused", LineFormatter.values());
            return;
        }
        if (laura.isAsleep()) {
            laura.wakeUp();
        }
        laura.playEmote(emote);
        LauraAdvancements.onEmote(player, emote);
        com.vyrriox.lauramod.api.LauraAPI.fire("emote", laura, player, emote.animationName());
        if (emote == Emote.DANCE) {
            laura.brain().onDance();
            LauraSpeech.say(laura, player, "dance", LineFormatter.values());
        } else {
            laura.brain().needs().add(Needs.Need.FUN, 3);
        }
    }

    public static void hug(ServerPlayer player, LauraEntity laura) {
        if (laura.brain().isSulking() && laura.getRandom().nextBoolean()) {
            LauraSpeech.say(laura, player, "hug.refused", LineFormatter.values());
            laura.playEmote(Emote.POUT);
            return;
        }
        laura.getLookControl().setLookAt(player);
        laura.playEmote(Emote.HUG);
        laura.brain().onHug(player);
        LauraSpeech.say(laura, player, "hug", LineFormatter.values());
        LauraAdvancements.add(player, "hugs", 1);
    }

    public static void kiss(ServerPlayer player, LauraEntity laura) {
        if (laura.isGagged()) {
            LauraSpeech.say(laura, player, "gagged_talk", LineFormatter.values());
            return;
        }
        if (laura.brain().isSulking() || laura.getAffection() < 200) {
            LauraSpeech.say(laura, player, "kiss.refused", LineFormatter.values());
            laura.playEmote(Emote.SLAP);
            return;
        }
        laura.getLookControl().setLookAt(player);
        laura.playEmote(Emote.KISS);
        laura.brain().onKiss(player);
        LauraSpeech.say(laura, player, "kiss", LineFormatter.values());
        LauraAdvancements.add(player, "kisses", 1);
    }

    public static void rename(ServerPlayer player, LauraEntity laura, String name) {
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        if (clean.isEmpty() || clean.length() > 32) {
            LauraSpeech.say(laura, player, "rename.invalid", LineFormatter.values());
            return;
        }
        laura.setCustomName(Component.literal(clean));
        LauraSpeech.say(laura, player, "rename.done", LineFormatter.values().with("name", clean));
        LauraAdvancements.award(player, "rename");
    }

    // ------------------------------------------------------------------ gag

    public static void gag(ServerPlayer player, LauraEntity laura, ItemStack stack) {
        if (laura.isGagged()) {
            LauraSpeech.say(laura, player, "gagged_talk", LineFormatter.values());
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        laura.setGagged(true, LauraConfig.gagSeconds.getInt() * 20);
        laura.brain().changeAffection(-LauraConfig.gagAffectionPenalty.getInt());
        laura.brain().needs().add(Needs.Need.FUN, -10);
        laura.playSound(SoundEvents.GRASS_PLACE, 1.0F, 0.8F);
        laura.playLauraSound(LauraRegistries.Sound.MUFFLED, 1.0F, 1.0F);
        laura.playEmote(Emote.STOMP);
        laura.spawnParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState()), 12, 0.3);
        LauraSpeech.say(laura, player, "gag.on", LineFormatter.values());
        LauraAdvancements.award(player, "gag");
    }

    /** Removes the gag. {@code self}: she took it off herself after the timer. */
    public static void ungag(@Nullable ServerPlayer player, LauraEntity laura, boolean self) {
        if (!laura.isGagged()) {
            return;
        }
        laura.setGagged(false, 0);
        laura.playSound(SoundEvents.GRASS_BREAK, 1.0F, 1.0F);
        laura.spawnItemParticles(new ItemStack(net.minecraft.world.item.Items.WHEAT), 10);
        ServerPlayer target = player != null ? player : LauraSpeech.owner(laura);
        if (target != null) {
            LauraSpeech.say(laura, target, self ? "gag.off_self" : "gag.off", LineFormatter.values());
        }
        if (!self && player != null) {
            laura.brain().changeAffection(2);
        } else {
            laura.brain().makeHappy(0);
            laura.playEmote(Emote.STOMP);
        }
    }

    // ------------------------------------------------------------------ items

    public static void giveItem(ServerPlayer player, LauraEntity laura, ItemStack stack, InteractionHand hand) {
        if (LauraBags.isWearableOnBack(stack) && laura.getBackItem().isEmpty()) {
            laura.setBackItem(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            laura.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            laura.playEmote(Emote.TWIRL);
            laura.brain().changeAffection(5);
            LauraSpeech.say(laura, player, laura.bags().hasBackpack() ? "backpack.equip" : "backpack.equip_small", LineFormatter.values().with("item", stack.getHoverName()));
            LauraAdvancements.award(player, "backpack");
            return;
        }
        if (GiftTable.food(stack) == null && LauraMod.platform().equipTrinket(laura, stack, false)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            laura.playSound(SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.0F, 1.2F);
            laura.playEmote(Emote.BLUSH);
            laura.hearts(4);
            laura.brain().changeAffection(8);
            LauraSpeech.say(laura, player, "trinket.equip", LineFormatter.values().with("item", stack.getHoverName()));
            LauraAdvancements.award(player, "curio");
            return;
        }
        GiftTable.FoodInfo food = GiftTable.food(stack);
        boolean hungry = LauraConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.HUNGER) < 90;
        ItemSpec wanted = laura.brain().desiredItem();
        boolean desiredFood = wanted != null && wanted.test(stack) && laura.brain().desire().mustEat();
        boolean isRaw = player.level() instanceof ServerLevel level && CookingSupport.isRawCookable(level, stack);
        if (food != null && (hungry || desiredFood || food.preference() == GiftTable.Preference.FAVORITE) && !(isRaw && !desiredFood && laura.brain().needs().get(Needs.Need.HUNGER) > 20)) {
            if (laura.brain().eat(stack, player)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return;
        }
        if (laura.brain().receiveGift(player, stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return;
        }
        if (food != null) {
            // Food she does not need right now goes to her pantry (her inventory).
            ItemStack rest = laura.bags().add(stack.copyWithCount(1));
            if (rest.isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                LauraSpeech.say(laura, player, "eat.later", LineFormatter.values().with("item", stack.getHoverName()));
            } else {
                LauraSpeech.say(laura, player, "inventory_full", LineFormatter.values());
            }
        }
    }

    private static void eatBestFood(ServerPlayer player, LauraEntity laura) {
        var inventory = laura.inventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED && laura.brain().eat(stack, null)) {
                stack.shrink(1);
                inventory.setChanged();
                return;
            }
        }
        LauraSpeech.say(laura, player, "need.hunger.no_food", LineFormatter.values());
    }

    // ------------------------------------------------------------------ info

    public static void sendInfo(ServerPlayer player, LauraEntity laura) {
        MutableComponent header = Component.literal("=== ").append(LauraSpeech.nameComponent(laura)).append(" ===").withStyle(ChatFormatting.LIGHT_PURPLE);
        player.sendSystemMessage(header);
        player.sendSystemMessage(line("lauramod.info.mood", Component.translatable("lauramod.mood." + laura.getMood().key())));
        player.sendSystemMessage(line("lauramod.info.relation", Component.translatable("lauramod.relation." + laura.brain().relationKey()), laura.getAffection()));
        player.sendSystemMessage(line("lauramod.info.health", Math.round(laura.getHealth()), Math.round(laura.getMaxHealth())));
        if (LauraConfig.needsEnabled.get()) {
            MutableComponent needs = Component.empty();
            for (Needs.Need need : Needs.Need.values()) {
                int v = Math.round(laura.brain().needs().get(need));
                ChatFormatting color = v < 20 ? ChatFormatting.RED : v < 50 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
                needs.append(Component.translatable("lauramod.need." + need.key())).append(Component.literal(" " + v + "%  ").withStyle(color));
            }
            player.sendSystemMessage(needs);
        }
        if (laura.brain().desire() != null) {
            long seconds = Math.max(0, (laura.brain().desire().deadline() - laura.level().getGameTime()) / 20);
            player.sendSystemMessage(line("lauramod.info.desire", laura.brain().desire().describe(), seconds / 60, seconds % 60));
        }
        player.sendSystemMessage(line("lauramod.info.mode", Component.translatable("lauramod.mode." + laura.getMode().key()), Component.translatable("lauramod.combat." + laura.getCombatMode().key())));
        if (!laura.workplace().jobs().isEmpty()) {
            MutableComponent jobs = Component.empty();
            laura.workplace().jobs().forEach((job, area) -> jobs.append(Component.translatable("lauramod.job." + job.key())).append(" (" + area.describe() + ")  "));
            player.sendSystemMessage(line("lauramod.info.jobs", jobs));
        }
        if (laura.workplace().current() != null || !laura.workplace().queued().isEmpty()) {
            MutableComponent tasks = Component.empty();
            if (laura.workplace().current() != null) {
                tasks.append(laura.workplace().current().describe().copy().withStyle(ChatFormatting.YELLOW)).append("  ");
            }
            for (LauraTask t : laura.workplace().queued()) {
                tasks.append(t.describe()).append("  ");
            }
            player.sendSystemMessage(line("lauramod.info.tasks", tasks));
        }
        if (laura.getHomePos() != null) {
            BlockPos h = laura.getHomePos();
            player.sendSystemMessage(line("lauramod.info.home", h.getX() + " " + h.getY() + " " + h.getZ()));
        }
        player.sendSystemMessage(line("lauramod.info.days", laura.daysTogether()));
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** Teleport used by several orders; kept here to share the particle effect. */
    static void puff(LauraEntity laura) {
        laura.spawnParticles(ParticleTypes.PORTAL, 20, 0.5);
    }

    public static boolean teleportNear(LauraEntity laura, BlockPos pos) {
        boolean ok = LauraMovement.teleportNear(laura, pos);
        if (ok) {
            puff(laura);
        }
        return ok;
    }
}
