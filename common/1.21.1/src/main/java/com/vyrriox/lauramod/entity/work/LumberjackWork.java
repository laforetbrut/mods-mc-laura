package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lumberjack: finds a natural tree, chops its trunk, fells the whole tree (logs and leaves), replants
 * a sapling and stores the wood. Logs used in builds are never touched: a tree must carry natural
 * (non persistent) leaves.
 *
 * @author vyrriox
 */
public class LumberjackWork implements Work {
    private enum Phase {
        FIND, WALK, CHOP, FELL, REPLANT, COLLECT, STORE
    }

    private final WorkContext ctx;
    private final WorkArea area;
    private final boolean oneShot;
    private final List<ItemStack> products = new ArrayList<>();
    private final Set<BlockPos> rejected = new HashSet<>();
    private Phase phase = Phase.FIND;
    private BlockPos base;
    private final Deque<BlockPos> toBreak = new ArrayDeque<>();
    private int progress;
    private int collectTicks;
    private BlockPos storeTarget;
    private String failKey = "work.lumberjack.no_tree";
    private int idleCooldown;

    public LumberjackWork(WorkContext ctx, WorkArea area, boolean oneShot) {
        this.ctx = ctx;
        this.area = area;
        this.oneShot = oneShot;
    }

    @Override
    public String failKey() {
        return failKey;
    }

    @Override
    public List<ItemStack> products() {
        return products;
    }

    @Override
    public void stop() {
        if (base != null) {
            ctx.level().destroyBlockProgress(ctx.laura().getId(), base, -1);
        }
        ctx.laura().getNavigation().stop();
    }

    @Override
    public Status tick() {
        return switch (phase) {
            case FIND -> find();
            case WALK -> walk();
            case CHOP -> chop();
            case FELL -> fell();
            case REPLANT -> replant();
            case COLLECT -> collect();
            case STORE -> storeWood();
        };
    }

    private Status find() {
        if (idleCooldown > 0) {
            idleCooldown--;
            return Status.IDLE;
        }
        if (!oneShot && ctx.inventoryAlmostFull()) {
            phase = Phase.STORE;
            return Status.WORKING;
        }
        base = findTree();
        if (base == null) {
            if (oneShot) {
                return Status.FAILED;
            }
            idleCooldown = 200;
            return hasWoodToStore() ? storeNow() : Status.IDLE;
        }
        ctx.resetWalk();
        phase = Phase.WALK;
        return Status.WORKING;
    }

    private Status storeNow() {
        phase = Phase.STORE;
        return Status.WORKING;
    }

    private BlockPos findTree() {
        ServerLevel level = ctx.level();
        BlockPos center = area.isIn(level) ? area.center() : ctx.laura().blockPosition();
        int radius = area.radius();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 8, radius))) {
            if (!area.contains(pos) || rejected.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) && !level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
                candidates.add(pos.immutable());
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(ctx.laura().blockPosition())));
        for (BlockPos candidate : candidates) {
            Set<BlockPos> logs = collectLogs(candidate);
            if (logs.isEmpty()) {
                rejected.add(candidate);
                continue;
            }
            if (LauraConfig.onlyNaturalTrees.get() && naturalLeaves(logs) < 4) {
                rejected.addAll(logs);
                continue;
            }
            return candidate;
        }
        return null;
    }

    private Set<BlockPos> collectLogs(BlockPos start) {
        ServerLevel level = ctx.level();
        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        int limit = LauraConfig.maxLogsPerTree.getInt();
        while (!open.isEmpty() && logs.size() < limit) {
            BlockPos pos = open.poll();
            if (logs.contains(pos) || !level.getBlockState(pos).is(BlockTags.LOGS) || pos.getY() < start.getY()) {
                continue;
            }
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            BlockPos next = pos.offset(dx, dy, dz);
                            if (!logs.contains(next)) {
                                open.add(next);
                            }
                        }
                    }
                }
            }
        }
        return logs;
    }

    private int naturalLeaves(Set<BlockPos> logs) {
        ServerLevel level = ctx.level();
        int count = 0;
        for (BlockPos log : logs) {
            for (Direction dir : Direction.values()) {
                BlockState state = level.getBlockState(log.relative(dir));
                if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
                    count++;
                }
            }
            if (count >= 4) {
                return count;
            }
        }
        return count;
    }

    private Status walk() {
        if (!ctx.level().getBlockState(base).is(BlockTags.LOGS)) {
            phase = Phase.FIND;
            return Status.WORKING;
        }
        if (ctx.walkTo(base, 2.6)) {
            ctx.laura().getNavigation().stop();
            progress = 0;
            phase = Phase.CHOP;
        } else if (ctx.stuck()) {
            rejected.add(base);
            phase = Phase.FIND;
        }
        return Status.WORKING;
    }

    private Status chop() {
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(base);
        if (!state.is(BlockTags.LOGS)) {
            phase = Phase.FIND;
            return Status.WORKING;
        }
        ctx.laura().getLookControl().setLookAt(base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5);
        ItemStack tool = ctx.laura().getItemBySlot(EquipmentSlot.MAINHAND);
        float speed = tool.is(ItemTags.AXES) ? Math.max(1.0F, tool.getDestroySpeed(state)) : 1.0F;
        int needed = ctx.scaled((int) (Math.max(0.5F, state.getDestroySpeed(level, base)) * 30 / speed) + 10);
        progress++;
        if (progress % 5 == 0) {
            ctx.laura().swing(InteractionHand.MAIN_HAND);
            level.playSound(null, base, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        level.destroyBlockProgress(ctx.laura().getId(), base, Math.min(9, progress * 10 / needed));
        if (progress < needed) {
            return Status.WORKING;
        }
        level.destroyBlockProgress(ctx.laura().getId(), base, -1);
        if (tool.isDamageableItem()) {
            tool.hurtAndBreak(1, ctx.laura(), EquipmentSlot.MAINHAND);
        }
        Set<BlockPos> logs = collectLogs(base);
        List<BlockPos> ordered = new ArrayList<>(logs);
        ordered.sort(Comparator.comparingInt(BlockPos::getY));
        toBreak.clear();
        toBreak.addAll(ordered);
        if (LauraConfig.breakLeaves.get()) {
            toBreak.addAll(collectLeaves(logs));
        }
        phase = Phase.FELL;
        return Status.WORKING;
    }

    private List<BlockPos> collectLeaves(Set<BlockPos> logs) {
        ServerLevel level = ctx.level();
        Set<BlockPos> leaves = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        for (BlockPos log : logs) {
            for (Direction dir : Direction.values()) {
                open.add(log.relative(dir));
            }
        }
        while (!open.isEmpty() && leaves.size() < 600) {
            BlockPos pos = open.poll();
            if (leaves.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof LeavesBlock) || state.getValue(LeavesBlock.PERSISTENT) || state.getValue(LeavesBlock.DISTANCE) > 6) {
                continue;
            }
            leaves.add(pos.immutable());
            for (Direction dir : Direction.values()) {
                open.add(pos.relative(dir));
            }
        }
        List<BlockPos> out = new ArrayList<>(leaves);
        out.sort(Comparator.comparingInt(BlockPos::getY));
        return out;
    }

    private Status fell() {
        ServerLevel level = ctx.level();
        for (int i = 0; i < 4 && !toBreak.isEmpty(); i++) {
            BlockPos pos = toBreak.poll();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), ctx.laura(), ItemStack.EMPTY);
            level.destroyBlock(pos, false, ctx.laura());
            for (ItemStack drop : drops) {
                record(drop.copy());
                ctx.store(drop);
            }
        }
        if (toBreak.isEmpty()) {
            ctx.laura().brain().needs().add(Needs.Need.ENERGY, -3);
            ctx.laura().brain().needs().add(Needs.Need.HYGIENE, -2);
            phase = Phase.REPLANT;
        }
        return Status.WORKING;
    }

    private void record(ItemStack stack) {
        if (stack.is(ItemTags.LOGS)) {
            ctx.credit("logs", stack);
        }
        if (!oneShot || stack.isEmpty()) {
            return;
        }
        for (ItemStack p : products) {
            if (ItemStack.isSameItemSameComponents(p, stack)) {
                p.grow(stack.getCount());
                return;
            }
        }
        products.add(stack);
    }

    private Status replant() {
        ServerLevel level = ctx.level();
        if (LauraConfig.replantSaplings.get() && level.getBlockState(base).isAir()) {
            List<ItemStack> saplings = ctx.take(s -> s.is(ItemTags.SAPLINGS) && s.getItem() instanceof BlockItem, 1);
            for (ItemStack sapling : saplings) {
                BlockState plant = ((BlockItem) sapling.getItem()).getBlock().defaultBlockState();
                if (plant.canSurvive(level, base)) {
                    level.setBlock(base, plant, Block.UPDATE_ALL);
                    level.playSound(null, base, plant.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    ctx.laura().swing(InteractionHand.MAIN_HAND);
                    unrecord(sapling);
                } else {
                    ctx.store(sapling);
                }
            }
        }
        collectTicks = 0;
        phase = Phase.COLLECT;
        return Status.WORKING;
    }

    private void unrecord(ItemStack used) {
        for (ItemStack p : products) {
            if (ItemStack.isSameItem(p, used)) {
                p.shrink(used.getCount());
                break;
            }
        }
        products.removeIf(ItemStack::isEmpty);
    }

    private Status collect() {
        collectTicks++;
        if (collectTicks % 10 == 0) {
            ctx.collectItemsAround(base, 7, s -> s.is(ItemTags.LOGS) || s.is(ItemTags.SAPLINGS) || s.is(net.minecraft.world.item.Items.STICK) || s.is(net.minecraft.world.item.Items.APPLE));
        }
        if (collectTicks < 30) {
            return Status.WORKING;
        }
        if (oneShot) {
            ctx.laura().playEmote(Emote.CELEBRATE);
            return Status.DONE;
        }
        phase = hasWoodToStore() && ctx.count(s -> s.is(ItemTags.LOGS)) >= 16 ? Phase.STORE : Phase.FIND;
        return Status.WORKING;
    }

    private boolean hasWoodToStore() {
        return ctx.has(LumberjackWork::isWoodProduct);
    }

    static boolean isWoodProduct(ItemStack s) {
        return s.is(ItemTags.LOGS) || s.is(ItemTags.SAPLINGS) || s.is(net.minecraft.world.item.Items.STICK) || s.is(net.minecraft.world.item.Items.APPLE);
    }

    private Status storeWood() {
        if (storeTarget == null) {
            ItemStack sample = ctx.peek(LumberjackWork::isWoodProduct);
            if (sample.isEmpty()) {
                phase = Phase.FIND;
                return Status.WORKING;
            }
            storeTarget = ctx.laura().workplace().findDepositTarget(ChestPurpose.WOOD, sample, area);
            if (storeTarget == null) {
                phase = Phase.FIND;
                if (ctx.inventoryAlmostFull()) {
                    failKey = "work.no_chest";
                    idleCooldown = 400;
                    return Status.FAILED;
                }
                return Status.WORKING;
            }
            ctx.resetWalk();
        }
        if (ctx.walkTo(storeTarget, 2.3)) {
            // Keep a few saplings to replant.
            int keepSaplings = 4;
            ctx.depositInto(storeTarget, s -> isWoodProduct(s) && !(s.is(ItemTags.SAPLINGS) && ctx.count(x -> x.is(ItemTags.SAPLINGS)) <= keepSaplings));
            storeTarget = null;
            phase = Phase.FIND;
        } else if (ctx.stuck()) {
            storeTarget = null;
            phase = Phase.FIND;
        }
        return Status.WORKING;
    }
}
