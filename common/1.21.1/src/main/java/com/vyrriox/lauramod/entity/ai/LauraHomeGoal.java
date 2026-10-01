package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * In HOME mode she lives around her home: she walks back when she strays too far, and teleports
 * back when she is really far. Going to another dimension is handled when the order is given.
 *
 * @author vyrriox
 */
public class LauraHomeGoal extends Goal {
    private final LauraEntity laura;
    private int recalc;

    public LauraHomeGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean active() {
        BlockPos home = laura.getHomePos();
        return laura.getMode() == LauraMode.HOME && home != null && laura.getHomeDimension() == laura.level().dimension()
                && !laura.isAsleep() && !laura.isFetching() && !laura.isOrderedToSit() && !laura.isPassenger();
    }

    @Override
    public boolean canUse() {
        if (!active()) {
            return false;
        }
        int radius = LauraConfig.homeRadius.getInt();
        return laura.blockPosition().distSqr(laura.getHomePos()) > (double) radius * radius;
    }

    @Override
    public boolean canContinueToUse() {
        if (!active()) {
            return false;
        }
        double radius = LauraConfig.homeRadius.getInt() * 0.6;
        return laura.blockPosition().distSqr(laura.getHomePos()) > radius * radius;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void tick() {
        BlockPos home = laura.getHomePos();
        if (--recalc > 0) {
            return;
        }
        recalc = adjustedTickDelay(20);
        double dist = Math.sqrt(laura.blockPosition().distSqr(home));
        if (dist > LauraConfig.homeTeleportDistance.getInt() || laura.getNavigation().isDone() && dist > LauraConfig.homeRadius.getInt() * 2) {
            LauraMovement.teleportNear(laura, home);
        } else {
            laura.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        laura.getNavigation().stop();
    }
}
