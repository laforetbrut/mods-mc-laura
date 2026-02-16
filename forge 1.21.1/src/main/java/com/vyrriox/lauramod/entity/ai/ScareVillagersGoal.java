package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumSet;
import java.util.List;

public class ScareVillagersGoal extends Goal {
    private final LauraEntity laura;

    public ScareVillagersGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !laura.level().isClientSide;
    }

    @Override
    public void tick() {
        if (laura.tickCount % 20 == 0) {
            List<Villager> villagers = laura.level().getEntitiesOfClass(Villager.class,
                    laura.getBoundingBox().inflate(10.0D));
            for (Villager villager : villagers) {
                // Force villager to run away from Laura
                villager.getNavigation().moveTo(villager.getX() + (villager.getX() - laura.getX()),
                        villager.getY(),
                        villager.getZ() + (villager.getZ() - laura.getZ()), 1.2D);
            }
        }
    }
}
