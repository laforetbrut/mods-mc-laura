package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Sits when told to stay, and while sulking (she refuses to move until you apologize).
 *
 * @author vyrriox
 */
public class LauraSitGoal extends Goal {
    private final LauraEntity laura;

    public LauraSitGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    private boolean shouldSit() {
        if (laura.isInWater() || !laura.onGround() || laura.isAsleep() || laura.isFetching()) {
            return false;
        }
        return laura.getMode() == LauraMode.STAY || laura.brain().isSulking();
    }

    @Override
    public boolean canUse() {
        return shouldSit();
    }

    @Override
    public boolean canContinueToUse() {
        return shouldSit();
    }

    @Override
    public void start() {
        laura.getNavigation().stop();
        laura.setInSittingPose(true);
    }

    @Override
    public void stop() {
        laura.setInSittingPose(false);
    }
}
