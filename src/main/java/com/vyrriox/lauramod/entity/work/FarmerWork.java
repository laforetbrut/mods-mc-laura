package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Farmer: harvests ripe crops in her field, replants them, sows empty farmland, uses bone meal and
 * stores the harvest in the field's chest.
 *
 * @author vyrriox
 */
public class FarmerWork implements Work {
    private enum Phase {
        SCAN, WALK, ACT, RESTOCK, STORE
    }

    private enum Action {
        HARVEST, PLANT, BONEMEAL
    }

    private record Job(BlockPos pos, Action action) {
    }

    private static final int KEEP_SEEDS = 32;

    private final WorkContext ctx;
    private final WorkArea area;
    private final boolean oneShot;
    private final List<ItemStack> products = new ArrayList<>();
    private final Set<Item> farmItems = new HashSet<>();
    private final Set<BlockPos> rejected = new HashSet<>();
    private Phase phase = Phase.SCAN;
    private Job job;
    private int progress;
    private BlockPos chest;
    private int idleCooldown;
    private boolean triedRestock;
    private String failKey = "work.farmer.nothing";

    public FarmerWork(WorkContext ctx, WorkArea area, boolean oneShot) {
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
        ctx.laura().getNavigation().stop();
    }

    @Override
    public Status tick() {
        return switch (phase) {
            case SCAN -> scan();
            case WALK -> walk();
            case ACT -> act();
            case RESTOCK -> restock();
            case STORE -> store();
        };
    }

    private Status scan() {
        if (idleCooldown > 0) {
            idleCooldown--;
            return Status.IDLE;
        }
        if (!oneShot && (ctx.inventoryAlmostFull() || ctx.count(this::isStorable) >= 96)) {
            phase = Phase.STORE;
            return Status.WORKING;
        }
        job = findJob();
        if (job != null) {
            ctx.resetWalk();
            phase = Phase.WALK;
            return Status.WORKING;
        }
        if (!oneShot && !triedRestock && hasEmptyFarmland() && !ctx.has(FarmerWork::isSeed)) {
            triedRestock = true;
            phase = Phase.RESTOCK;
            return Status.WORKING;
        }
        if (oneShot) {
            if (products.isEmpty()) {
                return Status.FAILED;
            }
            ctx.laura().playEmote(Emote.CLAP);
            return Status.DONE;
        }
        if (ctx.has(this::isStorable)) {
            phase = Phase.STORE;
            return Status.WORKING;
        }
        idleCooldown = 100;
        triedRestock = false;
        return Status.IDLE;
    }

    private Job findJob() {
        ServerLevel level = ctx.level();
        BlockPos center = area.center();
        int r = area.radius();
        Job best = null;
        double bestDist = Double.MAX_VALUE;
        boolean hasSeeds = ctx.has(FarmerWork::isSeed);
        boolean hasBoneMeal = LauraConfig.useBoneMeal.get() && ctx.has(s -> s.is(Items.BONE_MEAL));
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -4, -r), center.offset(r, 4, r))) {
            if (!area.contains(pos) || rejected.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            Action action = null;
            if (isRipe(level, pos, state)) {
                action = Action.HARVEST;
            } else if (!oneShot && hasSeeds && LauraConfig.plantEmptyFarmland.get() && canPlantOn(level, pos, state)) {
                action = Action.PLANT;
            } else if (!oneShot && hasBoneMeal && state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                action = Action.BONEMEAL;
            }
            if (action == null) {
                continue;
            }
            double d = pos.distSqr(ctx.laura().blockPosition()) + (action == Action.HARVEST ? 0 : action == Action.PLANT ? 16 : 64);
            if (d < bestDist) {
                bestDist = d;
                best = new Job(pos.immutable(), action);
            }
        }
        return best;
    }

    private boolean hasEmptyFarmland() {
        ServerLevel level = ctx.level();
        BlockPos center = area.center();
        int r = area.radius();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -4, -r), center.offset(r, 4, r))) {
            if (area.contains(pos) && canPlantOn(level, pos, level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /** True if pos is air right above farmland or soul sand. */
    private static boolean canPlantOn(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.isAir()) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof FarmBlock || below.is(Blocks.SOUL_SAND);
    }

    static boolean isRipe(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockState side = level.getBlockState(pos.relative(dir));
                if (side.getBlock() instanceof AttachedStemBlock && side.getValue(AttachedStemBlock.FACING) == dir.getOpposite()) {
                    return true;
                }
            }
            return false;
        }
        if (block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.BAMBOO) {
            // Only segments standing on another segment: the bottom one stays and grows back.
            return level.getBlockState(pos.below()).is(block);
        }
        return false;
    }

    static boolean isSeed(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return false;
        }
        Block block = item.getBlock();
        return block instanceof CropBlock || block instanceof NetherWartBlock;
    }

    private boolean isStorable(ItemStack stack) {
        if (!farmItems.contains(stack.getItem())) {
            return false;
        }
        if (isSeed(stack)) {
            return ctx.count(s -> s.is(stack.getItem())) > KEEP_SEEDS;
        }
        return !stack.is(Items.BONE_MEAL);
    }

    private Status walk() {
        if (ctx.walkTo(job.pos(), 2.2)) {
            ctx.laura().getNavigation().stop();
            progress = 0;
            phase = Phase.ACT;
        } else if (ctx.stuck()) {
            rejected.add(job.pos());
            phase = Phase.SCAN;
        }
        return Status.WORKING;
    }

    private Status act() {
        ServerLevel level = ctx.level();
        BlockPos pos = job.pos();
        ctx.laura().getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        if (++progress < ctx.scaled(job.action() == Action.HARVEST ? 12 : 6)) {
            return Status.WORKING;
        }
        ctx.laura().swing(InteractionHand.MAIN_HAND);
        BlockState state = level.getBlockState(pos);
        switch (job.action()) {
            case HARVEST -> {
                if (isRipe(level, pos, state)) {
                    harvest(level, pos, state);
                }
            }
            case PLANT -> {
                if (canPlantOn(level, pos, state)) {
                    plant(level, pos);
                }
            }
            case BONEMEAL -> {
                List<ItemStack> meal = ctx.take(s -> s.is(Items.BONE_MEAL), 1);
                for (ItemStack stack : meal) {
                    if (BoneMealItem.growCrop(stack, level, pos)) {
                        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
                    }
                    ctx.store(stack);
                }
            }
        }
        ctx.laura().brain().needs().add(Needs.Need.ENERGY, -0.3F);
        phase = Phase.SCAN;
        return Status.WORKING;
    }

    private void harvest(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        List<ItemStack> drops;
        if (block instanceof SweetBerryBushBlock) {
            int count = 1 + level.random.nextInt(2) + (state.getValue(SweetBerryBushBlock.AGE) == 3 ? 1 : 0);
            drops = List.of(new ItemStack(Items.SWEET_BERRIES, count));
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            drops = new ArrayList<>(Block.getDrops(state, level, pos, null, ctx.laura(), ItemStack.EMPTY));
            level.destroyBlock(pos, false, ctx.laura());
            BlockState replant = null;
            if (block instanceof CropBlock crop) {
                replant = crop.getStateForAge(0);
            } else if (block instanceof NetherWartBlock) {
                replant = block.defaultBlockState();
            } else if (block instanceof CocoaBlock) {
                replant = block.defaultBlockState().setValue(CocoaBlock.FACING, state.getValue(CocoaBlock.FACING));
            }
            if (replant != null) {
                Item seed = block.asItem();
                boolean paid = false;
                for (ItemStack drop : drops) {
                    if (drop.is(seed) && !drop.isEmpty()) {
                        drop.shrink(1);
                        paid = true;
                        break;
                    }
                }
                if (!paid) {
                    paid = !ctx.take(s -> s.is(seed), 1).isEmpty();
                }
                if (paid && replant.canSurvive(level, pos)) {
                    level.setBlock(pos, replant, Block.UPDATE_ALL);
                }
            }
        }
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            farmItems.add(drop.getItem());
            if (oneShot) {
                recordProduct(drop.copy());
            }
            ctx.store(drop);
        }
    }

    private void plant(ServerLevel level, BlockPos pos) {
        boolean soulSand = level.getBlockState(pos.below()).is(Blocks.SOUL_SAND);
        List<ItemStack> seeds = ctx.take(s -> isSeed(s) && (soulSand == (((BlockItem) s.getItem()).getBlock() instanceof NetherWartBlock)), 1);
        for (ItemStack seed : seeds) {
            BlockState crop = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
            if (crop.canSurvive(level, pos)) {
                level.setBlock(pos, crop, Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                farmItems.add(seed.getItem());
            } else {
                ctx.store(seed);
                rejected.add(pos);
            }
        }
        if (seeds.isEmpty()) {
            rejected.add(pos);
        }
    }

    private void recordProduct(ItemStack stack) {
        ctx.credit("harvested", stack);
        for (ItemStack p : products) {
            if (ItemStack.isSameItemSameComponents(p, stack)) {
                p.grow(stack.getCount());
                return;
            }
        }
        products.add(stack);
    }

    private Status restock() {
        if (chest == null) {
            chest = ctx.laura().workplace().findSource(ChestPurpose.SEEDS, s -> isSeed(s) || s.is(Items.BONE_MEAL), area);
            if (chest == null) {
                phase = Phase.SCAN;
                idleCooldown = 60;
                return Status.WORKING;
            }
            ctx.resetWalk();
        }
        if (ctx.walkTo(chest, 2.3)) {
            ctx.withdrawFrom(chest, s -> isSeed(s) || s.is(Items.BONE_MEAL), 64);
            chest = null;
            phase = Phase.SCAN;
        } else if (ctx.stuck()) {
            chest = null;
            phase = Phase.SCAN;
        }
        return Status.WORKING;
    }

    private Status store() {
        if (chest == null) {
            ItemStack sample = ctx.peek(this::isStorable);
            if (sample.isEmpty()) {
                phase = Phase.SCAN;
                return Status.WORKING;
            }
            chest = ctx.laura().workplace().findDepositTarget(ChestPurpose.HARVEST, sample, area);
            if (chest == null) {
                phase = Phase.SCAN;
                idleCooldown = 200;
                if (ctx.inventoryAlmostFull()) {
                    failKey = "work.no_chest";
                    return Status.FAILED;
                }
                return Status.WORKING;
            }
            ctx.resetWalk();
        }
        if (ctx.walkTo(chest, 2.3)) {
            ctx.depositInto(chest, this::isStorable);
            chest = null;
            phase = Phase.SCAN;
        } else if (ctx.stuck()) {
            chest = null;
            phase = Phase.SCAN;
        }
        return Status.WORKING;
    }
}
