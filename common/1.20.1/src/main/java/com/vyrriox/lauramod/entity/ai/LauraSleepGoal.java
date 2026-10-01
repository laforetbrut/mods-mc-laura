package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Sleeps when she is tired: in a free bed near her home, next to her sleeping partner, or right
 * where she stands when she cannot keep her eyes open.
 *
 * @author vyrriox
 */
public class LauraSleepGoal extends Goal {
    private final LauraEntity laura;
    private BlockPos bed;
    private int walkTicks;
    private int checkCooldown;

    public LauraSleepGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (--checkCooldown > 0) {
            return false;
        }
        checkCooldown = 40;
        if (laura.isFetching() || laura.getTarget() != null || laura.isInWaterOrBubble() || laura.isEmoting()) {
            return false;
        }
        return laura.brain().wantsToSleep();
    }

    @Override
    public boolean canContinueToUse() {
        if (laura.getTarget() != null || laura.isInWaterOrBubble()) {
            return false;
        }
        if (laura.isAsleep()) {
            return !laura.brain().canWakeUp() && ownerStillClose();
        }
        return walkTicks < 400;
    }

    private boolean ownerStillClose() {
        if (laura.getMode() != LauraMode.FOLLOW) {
            return true;
        }
        LivingEntity owner = laura.getOwner();
        return owner == null || owner.level() == laura.level() && owner.distanceToSqr(laura) < 20 * 20;
    }

    @Override
    public void start() {
        walkTicks = 0;
        bed = null;
        LivingEntity owner = laura.getOwner();
        BlockPos center = laura.getMode() == LauraMode.HOME && laura.getHomePos() != null ? laura.getHomePos() : laura.blockPosition();
        if (owner instanceof Player player && player.isSleeping() && laura.getMode() == LauraMode.FOLLOW) {
            center = player.blockPosition();
        }
        bed = laura.findFreeBed(center, 10);
        if (bed == null) {
            laura.goToSleep(null);
        } else {
            laura.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.9);
        }
    }

    @Override
    public void tick() {
        if (laura.isAsleep()) {
            laura.getNavigation().stop();
            return;
        }
        walkTicks++;
        if (bed != null) {
            if (laura.blockPosition().distSqr(bed) <= 4.5) {
                laura.goToSleep(bed);
            } else if (laura.getNavigation().isDone() && walkTicks % 20 == 0) {
                if (walkTicks > 200) {
                    laura.goToSleep(null);
                } else {
                    laura.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.9);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (laura.isAsleep()) {
            laura.wakeUp();
        }
        bed = null;
    }
}
