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
            Set<BlockPos> logs = treeLogs(level, candidate);
            if (logs.isEmpty()) {
                rejected.add(candidate);
                continue;
            }
            return candidate;
        }
        return null;
    }

    /** How far a natural tree spreads sideways from the foot of its trunk (large oaks, acacias, 2 x 2 trunks). */
    private static final int NATURAL_SPREAD = 6;

    /**
     * The logs she fells when she chops the trunk at {@code base}, or an empty set when it is not a
     * tree she may cut. With {@code onlyNaturalTrees} a tree stands on soil, is made of one kind of
     * log, stays close to its trunk, and every log she takes leads up to a log crowned by natural
     * leaves: a wall, a beam or a roof made of logs that touches the tree is left standing. A log
     * column of a build that natural leaves grow right on top of cannot be told from a trunk.
     */
    public static Set<BlockPos> treeLogs(ServerLevel level, BlockPos base) {
        boolean natural = LauraConfig.onlyNaturalTrees.get();
        if (natural && !isSoil(level.getBlockState(base.below()))) {
            return Set.of();
        }
        List<BlockPos> order = new ArrayList<>();
        java.util.Map<BlockPos, BlockPos> parents = connectedLogs(level, base, natural, order);
        if (!natural) {
            return new HashSet<>(order);
        }
        // A log is part of the tree when natural leaves grow right above it, when it carries a log
        // of the tree, or when a log of the tree was found through it (bends and branches).
        Set<BlockPos> all = new HashSet<>(order);
        Set<BlockPos> kept = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        for (BlockPos log : order) {
            if (isNaturalLeaf(level.getBlockState(log.above()))) {
                open.add(log);
            }
        }
        while (!open.isEmpty()) {
            BlockPos log = open.poll();
            if (!kept.add(log)) {
                continue;
            }
            BlockPos parent = parents.get(log);
            if (parent != null) {
                open.add(parent);
            }
            if (all.contains(log.below())) {
                open.add(log.below());
            }
        }
        return kept.contains(base) && naturalLeaves(level, kept) >= 4 ? kept : Set.of();
    }

    private static boolean isSoil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS);
    }

    private static boolean isNaturalLeaf(BlockState state) {
        return state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT);
    }

    /**
     * Logs connected to the base, never below it, in the order they were found (the base first,
     * then the log right above each log before its other neighbours, so that a trunk is always
     * followed upwards). Returns, for each log, the log it was found through.
     */
    private static java.util.Map<BlockPos, BlockPos> connectedLogs(ServerLevel level, BlockPos base, boolean natural, List<BlockPos> order) {
        Block kind = level.getBlockState(base).getBlock();
        java.util.Map<BlockPos, BlockPos> parents = new java.util.HashMap<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        BlockPos start = base.immutable();
        open.add(start);
        seen.add(start);
        int limit = LauraConfig.maxLogsPerTree.getInt();
        while (!open.isEmpty() && order.size() < limit) {
            BlockPos pos = open.poll();
            order.add(pos);
            List<BlockPos> around = new ArrayList<>(17);
            around.add(pos.above());
            for (int dy = 1; dy >= 0; dy--) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dz != 0) {
                            around.add(pos.offset(dx, dy, dz));
                        }
                    }
                }
            }
            for (BlockPos next : around) {
                if (seen.contains(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (!state.is(BlockTags.LOGS)) {
                    continue;
                }
                if (natural && (state.getBlock() != kind || Math.abs(next.getX() - start.getX()) > NATURAL_SPREAD
                        || Math.abs(next.getZ() - start.getZ()) > NATURAL_SPREAD)) {
                    continue;
                }
                seen.add(next);
                parents.put(next, pos);
                open.add(next);
            }
        }
        return parents;
    }

    /** Natural leaves next to the upper half of the logs: a tree carries its leaves at the top. */
    private static int naturalLeaves(ServerLevel level, Set<BlockPos> logs) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos log : logs) {
            minY = Math.min(minY, log.getY());
            maxY = Math.max(maxY, log.getY());
        }
        int from = minY + (maxY - minY) / 2;
        int count = 0;
        for (BlockPos log : logs) {
            if (log.getY() < from) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (isNaturalLeaf(level.getBlockState(log.relative(dir)))) {
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
            tool.hurtAndBreak(1, ctx.laura(), holder -> holder.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        Set<BlockPos> logs = treeLogs(level, base);
        if (logs.isEmpty()) {
            // It stopped being a tree she may cut while she was chopping (its leaves were removed...).
            rejected.add(base);
            phase = Phase.FIND;
            return Status.WORKING;
        }
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
            if (ItemStack.isSameItemSameTags(p, stack)) {
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
