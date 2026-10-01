package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * When item pickup is enabled, she collects items lying around into her own inventory.
 *
 * @author vyrriox
 */
public class LauraPickupGoal extends Goal {
    private final LauraEntity laura;
    private ItemEntity target;
    private int ticks;
    private int cooldown;

    public LauraPickupGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0 || !laura.isPickingUpItems() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit()) {
            return false;
        }
        cooldown = 20;
        List<ItemEntity> items = laura.level().getEntitiesOfClass(ItemEntity.class, laura.getBoundingBox().inflate(8, 3, 8),
                e -> e.isAlive() && !e.hasPickUpDelay() && canFit(e.getItem()));
        target = items.stream().min(Comparator.comparingDouble(laura::distanceToSqr)).orElse(null);
        return target != null;
    }

    private boolean canFit(ItemStack stack) {
        return laura.bags().canAdd(stack);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && ticks < 200;
    }

    @Override
    public void start() {
        ticks = 0;
    }

    @Override
    public void tick() {
        ticks++;
        if (laura.distanceToSqr(target) > 1.5 * 1.5) {
            if (ticks % 10 == 1) {
                laura.getNavigation().moveTo(target, 1.1);
            }
            return;
        }
        ItemStack stack = target.getItem().copy();
        int before = stack.getCount();
        ItemStack rest = laura.bags().add(stack);
        int taken = before - rest.getCount();
        if (taken > 0) {
            laura.take(target, taken);
            if (rest.isEmpty()) {
                target.discard();
            } else {
                target.setItem(rest);
            }
        }
        target = null;
    }

    @Override
    public void stop() {
        target = null;
        laura.getNavigation().stop();
    }
}
