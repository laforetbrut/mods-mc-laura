package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.ai.LauraMovement;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.LauraInventories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Walking and inventory helpers shared by the jobs.
 *
 * @author vyrriox
 */
public class WorkContext {
    protected final LauraEntity laura;
    private BlockPos walkTarget;
    private int walkTicks;
    private double lastDistance = Double.MAX_VALUE;
    private int noProgress;

    public WorkContext(LauraEntity laura) {
        this.laura = laura;
    }

    /** Adds produced items to the partner's advancement counter (logs, harvested, meals). */
    public void credit(String counter, ItemStack stack) {
        if (!stack.isEmpty()) {
            com.vyrriox.lauramod.world.LauraAdvancements.add(com.vyrriox.lauramod.entity.LauraSpeech.owner(laura), counter, stack.getCount());
        }
    }

    public LauraEntity laura() {
        return laura;
    }

    public ServerLevel level() {
        return (ServerLevel) laura.level();
    }

    /** Scales a tick duration by the configured work speed. */
    public int scaled(int ticks) {
        return Math.max(1, ticks * 100 / Math.max(10, LauraConfig.workSpeedPercent.getInt()));
    }

    /** Walks towards the block. Returns true once within reach. */
    public boolean walkTo(BlockPos pos, double reach) {
        double dist = laura.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (dist <= reach * reach) {
            return true;
        }
        if (!pos.equals(walkTarget)) {
            walkTarget = pos;
            walkTicks = 0;
            noProgress = 0;
            lastDistance = Double.MAX_VALUE;
        }
        if (walkTicks % 20 == 0) {
            laura.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.05);
            if (dist >= lastDistance - 0.5) {
                noProgress++;
            } else {
                noProgress = 0;
            }
            lastDistance = dist;
        }
        walkTicks++;
        if (walkTicks > 100 && noProgress >= 4 && dist > 64) {
            // Hopelessly stuck far away: jump next to the target.
            LauraMovement.teleportNear(laura, pos);
            noProgress = 0;
        }
        return false;
    }

    /** True when the current walk target seems unreachable. */
    public boolean stuck() {
        return walkTicks > 400 || walkTicks > 60 && noProgress >= 5;
    }

    public void resetWalk() {
        walkTarget = null;
        walkTicks = 0;
        noProgress = 0;
        lastDistance = Double.MAX_VALUE;
    }

    public SimpleContainer inventory() {
        return laura.inventory();
    }

    /** Her bag and her backpack. */
    public com.vyrriox.lauramod.entity.LauraBags bags() {
        return laura.bags();
    }

    public int count(Predicate<ItemStack> filter) {
        return bags().count(filter);
    }

    public boolean has(Predicate<ItemStack> filter) {
        return count(filter) > 0;
    }

    /** A copy of the first matching stack in her inventory, or EMPTY. */
    public ItemStack peek(Predicate<ItemStack> filter) {
        return bags().peek(filter);
    }

    /** Removes up to {@code max} matching items from her inventory. */
    public List<ItemStack> take(Predicate<ItemStack> filter, int max) {
        return bags().take(filter, max);
    }

    /** Adds to her inventory; what does not fit is dropped at her feet. */
    public void store(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack rest = bags().add(stack);
        if (!rest.isEmpty()) {
            laura.spawnAtLocation(rest);
        }
    }

    public boolean inventoryAlmostFull() {
        return bags().almostFull();
    }

    /** Inserts into a container. Returns what did not fit. */
    public static ItemStack insert(Container container, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
            ItemStack in = container.getItem(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, rest) && container.canPlaceItem(i, rest)) {
                int move = Math.min(rest.getCount(), Math.min(in.getMaxStackSize(), container.getMaxStackSize()) - in.getCount());
                if (move > 0) {
                    in.grow(move);
                    rest.shrink(move);
                }
            }
        }
        for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
            if (container.getItem(i).isEmpty() && container.canPlaceItem(i, rest)) {
                int move = Math.min(rest.getCount(), Math.min(rest.getMaxStackSize(), container.getMaxStackSize()));
                container.setItem(i, rest.split(move));
            }
        }
        container.setChanged();
        return rest;
    }

    /** Moves matching items from her inventory into the inventory at pos (with the lid animation). */
    public int depositInto(BlockPos pos, Predicate<ItemStack> filter) {
        InventoryAccess target = LauraInventories.at(level(), pos);
        if (target == null) {
            return 0;
        }
        animateContainer(pos);
        int moved = bags().moveInto(target, filter);
        laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return moved;
    }

    /** Inserts a stack into the inventory at pos. Returns what did not fit. */
    public ItemStack insertAt(BlockPos pos, ItemStack stack) {
        InventoryAccess target = LauraInventories.at(level(), pos);
        return target == null ? stack : target.insert(stack);
    }

    /** Takes up to {@code max} matching items from the inventory at pos into her inventory. */
    public int withdrawFrom(BlockPos pos, Predicate<ItemStack> filter, int max) {
        InventoryAccess source = LauraInventories.at(level(), pos);
        if (source == null) {
            return 0;
        }
        animateContainer(pos);
        int taken = 0;
        for (int i = 0; i < source.size() && taken < max; i++) {
            ItemStack s = source.get(i);
            if (!s.isEmpty() && filter.test(s) && bags().canAdd(s)) {
                ItemStack part = source.extract(i, Math.min(s.getCount(), max - taken));
                taken += part.getCount();
                store(part);
            }
        }
        laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return taken;
    }

    private void animateContainer(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            level().blockEvent(pos, state.getBlock(), 1, 1);
            level().playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.4F, 1.0F);
            level().getServer().schedule(new net.minecraft.server.TickTask(level().getServer().getTickCount() + 12, () -> {
                if (level().getBlockState(pos).getBlock() instanceof ChestBlock) {
                    level().blockEvent(pos, state.getBlock(), 1, 0);
                    level().playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.4F, 1.0F);
                }
            }));
        } else {
            level().playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.4F, 1.0F);
        }
    }

    /** Picks up item entities around a position into her inventory. */
    public void collectItemsAround(BlockPos center, double radius, Predicate<ItemStack> filter) {
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(center).inflate(radius),
                e -> e.isAlive() && filter.test(e.getItem()))) {
            ItemStack stack = item.getItem().copy();
            int before = stack.getCount();
            ItemStack rest = bags().add(stack);
            int taken = before - rest.getCount();
            if (taken > 0) {
                laura.take(item, taken);
                if (rest.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(rest);
                }
            }
        }
    }
}
