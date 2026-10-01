package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.entity.work.CookWork;
import com.vyrriox.lauramod.entity.work.FarmerWork;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.entity.work.LumberjackWork;
import com.vyrriox.lauramod.entity.work.Work;
import com.vyrriox.lauramod.entity.work.WorkArea;
import com.vyrriox.lauramod.entity.work.WorkContext;
import com.vyrriox.lauramod.world.LauraAdvancements;
import com.vyrriox.lauramod.world.LauraWorldChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Runs her errands (chop a tree, harvest, cook) and, when the queue is empty and she is in WORK
 * mode, rotates between her active jobs.
 *
 * @author vyrriox
 */
public class LauraWorkGoal extends Goal {
    private final LauraEntity laura;
    private final WorkContext ctx;
    private Work work;
    private LauraJob workJob = LauraJob.NONE;
    private LauraTask workTask;
    private List<ItemStack> deliveries;
    private int rotateIndex;
    private int idleJobs;
    private int pauseTicks;
    private int deliverTicks;

    public LauraWorkGoal(LauraEntity laura) {
        this.laura = laura;
        this.ctx = new WorkContext(laura);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean blocked() {
        return !LauraConfig.workEnabled.get() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit()
                || laura.brain().isSulking() || laura.isPassenger() || laura.isLeashed() || laura.getTarget() != null;
    }

    private LauraTask errand() {
        LauraTask task = laura.workplace().current();
        return task != null && task.type().isErrand() && task.type() != LauraTask.Type.FETCH ? task : null;
    }

    private boolean jobsActive() {
        if (laura.getMode() != LauraMode.WORK || !laura.workplace().hasJobs()) {
            return false;
        }
        return LauraConfig.workAtNight.get() || !LauraWorldChecks.isNight(laura.level());
    }

    @Override
    public boolean canUse() {
        if (blocked()) {
            return false;
        }
        if (errand() != null || deliveries != null) {
            return true;
        }
        if (pauseTicks > 0) {
            pauseTicks--;
            return false;
        }
        return jobsActive();
    }

    @Override
    public boolean canContinueToUse() {
        if (blocked()) {
            return false;
        }
        return errand() != null || deliveries != null || jobsActive() && pauseTicks <= 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (work != null) {
            work.stop();
        }
        laura.getNavigation().stop();
    }

    /** Drops the running work (the task changed or the jobs were edited). */
    public void reset() {
        if (work != null) {
            work.stop();
        }
        work = null;
        workTask = null;
        workJob = LauraJob.NONE;
        deliveries = null;
        pauseTicks = 0;
    }

    @Override
    public void tick() {
        if (deliveries != null) {
            deliver();
            return;
        }
        LauraTask task = errand();
        if (task != null) {
            runErrand(task);
            return;
        }
        if (workTask != null) {
            // The errand was cancelled from outside.
            reset();
        }
        runJobs();
    }

    private WorkArea areaFor(LauraTask task, int defaultRadius) {
        if (task.area() != null) {
            return task.area();
        }
        return new WorkArea(laura.blockPosition(), defaultRadius, laura.level().dimension());
    }

    private void runErrand(LauraTask task) {
        if (work == null || workTask != task) {
            if (work != null) {
                work.stop();
            }
            workTask = task;
            workJob = LauraJob.NONE;
            boolean self = "self".equals(task.arg());
            work = switch (task.type()) {
                case CHOP_TREE -> new LumberjackWork(ctx, areaFor(task, 16), true);
                case HARVEST -> new FarmerWork(ctx, areaFor(task, 12), true);
                case COOK -> new CookWork(ctx, areaFor(task, 10), true, self);
                default -> null;
            };
            if (work == null) {
                laura.workplace().finishCurrent();
                return;
            }
        }
        Work.Status status = work.tick();
        switch (status) {
            case DONE -> {
                List<ItemStack> products = new ArrayList<>(work.products());
                work.stop();
                work = null;
                workTask = null;
                if (products.isEmpty()) {
                    laura.workplace().finishCurrent();
                } else {
                    deliveries = products;
                    deliverTicks = 0;
                    ctx.resetWalk();
                }
                ServerPlayer owner = LauraSpeech.owner(laura);
                if (owner != null) {
                    LauraAdvancements.award(owner, "task_" + task.type().key());
                }
            }
            case FAILED, IDLE -> {
                LauraSpeech.sayToOwner(laura, work.failKey(), LineFormatter.values());
                laura.playEmote(Emote.SHRUG);
                work.stop();
                work = null;
                workTask = null;
                laura.workplace().finishCurrent();
            }
            case WORKING -> {
            }
        }
    }

    private void deliver() {
        ServerPlayer owner = LauraSpeech.owner(laura);
        deliverTicks++;
        if (owner == null || owner.level() != laura.level() || deliverTicks > 20 * 60) {
            // Nobody to give it to: she keeps everything.
            deliveries = null;
            laura.workplace().finishCurrent();
            return;
        }
        double teleport = LauraConfig.teleportDistance.getDouble();
        if (laura.distanceToSqr(owner) > teleport * teleport) {
            LauraMovement.teleportNear(laura, owner.blockPosition());
        }
        if (!ctx.walkTo(owner.blockPosition(), 2.5)) {
            return;
        }
        int total = 0;
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack wanted : deliveries) {
            List<ItemStack> taken = ctx.take(s -> ItemStack.isSameItemSameComponents(s, wanted), wanted.getCount());
            for (ItemStack s : taken) {
                if (first.isEmpty()) {
                    first = s.copy();
                }
                total += s.getCount();
                laura.brain().giveToPlayer(owner, s);
            }
        }
        deliveries = null;
        laura.workplace().finishCurrent();
        laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        laura.playEmote(Emote.BOW);
        LauraSpeech.say(laura, owner, "work.delivered", LineFormatter.values().with("count", total).with("item", first.isEmpty() ? "" : first.getHoverName()));
    }

    private void runJobs() {
        Map<LauraJob, WorkArea> jobs = laura.workplace().jobs();
        if (jobs.isEmpty()) {
            return;
        }
        List<LauraJob> list = new ArrayList<>(jobs.keySet());
        if (work == null || !jobs.containsKey(workJob)) {
            if (work != null) {
                work.stop();
            }
            workJob = list.get(Math.floorMod(rotateIndex, list.size()));
            WorkArea area = jobs.get(workJob);
            work = switch (workJob) {
                case LUMBERJACK -> new LumberjackWork(ctx, area, false);
                case FARMER -> new FarmerWork(ctx, area, false);
                case COOK -> new CookWork(ctx, area, false, false);
                default -> null;
            };
            if (work == null) {
                rotateIndex++;
                return;
            }
        }
        WorkArea area = jobs.get(workJob);
        if (area != null && !area.isIn(laura.level())) {
            return;
        }
        if (area != null && !area.contains(laura.blockPosition()) && laura.distanceToSqr(area.center().getX(), area.center().getY(), area.center().getZ()) > 48 * 48) {
            LauraMovement.teleportNear(laura, area.center());
        }
        Work.Status status = work.tick();
        switch (status) {
            case WORKING -> idleJobs = 0;
            case IDLE, DONE -> {
                idleJobs++;
                work.stop();
                work = null;
                rotateIndex++;
                if (idleJobs >= list.size()) {
                    idleJobs = 0;
                    pauseTicks = 200;
                    if (laura.brain().readyPublic("work_idle", 900)) {
                        LauraSpeech.sayToOwner(laura, "work.idle", LineFormatter.values());
                    }
                }
            }
            case FAILED -> {
                if (laura.brain().readyPublic("work_fail_" + workJob.key(), 300)) {
                    LauraSpeech.sayToOwner(laura, work.failKey(), LineFormatter.values());
                }
                work.stop();
                work = null;
                rotateIndex++;
                pauseTicks = 100;
            }
        }
        if (laura.tickCount % 1200 == 0 && LauraConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.FUN) < 25
                && laura.brain().readyPublic("work_bored", 600)) {
            LauraSpeech.sayToOwner(laura, "work.bored", LineFormatter.values());
            laura.playEmote(Emote.TAP_FOOT);
        }
    }
}
