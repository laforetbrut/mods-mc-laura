package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;

/**
 * When she feels dirty she walks into the nearest water for a quick bath.
 *
 * @author vyrriox
 */
public class LauraBathGoal extends Goal {
    private final LauraEntity laura;
    private BlockPos water;
    private int ticks;
    private int bathTicks;
    private int cooldown;

    public LauraBathGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 300;
        if (!LauraConfig.needsEnabled.get() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit()
                || laura.brain().needs().get(Needs.Need.HYGIENE) > 25) {
            return false;
        }
        Optional<BlockPos> found = BlockPos.findClosestMatch(laura.blockPosition(), 14, 4,
                pos -> laura.level().getFluidState(pos).is(FluidTags.WATER) && laura.level().getBlockState(pos.above()).isAir());
        water = found.map(BlockPos::immutable).orElse(null);
        return water != null;
    }

    @Override
    public boolean canContinueToUse() {
        return water != null && ticks < 400 && bathTicks < 100;
    }

    @Override
    public void start() {
        ticks = 0;
        bathTicks = 0;
        LauraSpeech.sayToOwner(laura, "need.hygiene.bath", LineFormatter.values());
    }

    @Override
    public void tick() {
        ticks++;
        if (laura.isInWater()) {
            bathTicks++;
            laura.getNavigation().stop();
            if (bathTicks == 80) {
                LauraSpeech.sayToOwner(laura, "need.hygiene.clean", LineFormatter.values());
                com.vyrriox.lauramod.world.LauraAdvancements.award(LauraSpeech.owner(laura), "bath");
            }
            return;
        }
        if (ticks % 20 == 1) {
            laura.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.1);
        }
    }

    @Override
    public void stop() {
        water = null;
        laura.getNavigation().stop();
    }
}
