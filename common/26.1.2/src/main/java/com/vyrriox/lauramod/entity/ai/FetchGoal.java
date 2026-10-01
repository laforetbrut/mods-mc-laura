package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.LauraInventories;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.util.ItemSpec;
import com.vyrriox.lauramod.world.LauraAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * "Laura, bring me some wood!" She looks for the item on the ground, then in nearby containers,
 * then harvests matching blocks (logs, flowers, mature crops...), and brings everything back.
 *
 * @author vyrriox
 */
public class FetchGoal extends Goal {
    public enum Result {
        STARTED, DISABLED, BUSY, INVALID
    }

    private enum Phase {
        SEARCH, TO_ITEM, TO_CONTAINER, TO_BLOCK, BREAKING, RETURN
    }

    private final LauraEntity laura;
    private ItemSpec spec;
    private int amount;
    private UUID requester;
    private long startTime;
    private Phase phase = Phase.SEARCH;
    private final List<ItemStack> bag = new ArrayList<>();
    private int collected;
    private ItemEntity targetItem;
    private BlockPos targetPos;
    private int progress;
    private int moveTicks;
    private final Set<BlockPos> rejected = new HashSet<>();
    private final Set<Integer> rejectedItems = new HashSet<>();
    private boolean usedContainers;
    private boolean usedBlocks;

    public FetchGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public Result request(ServerPlayer player, ItemSpec wanted, int count) {
        if (!LauraConfig.fetchEnabled.get()) {
            return Result.DISABLED;
        }
        if (wanted == null) {
            return Result.INVALID;
        }
        if (spec != null) {
            return Result.BUSY;
        }
        this.spec = wanted;
        this.amount = Math.max(1, Math.min(count, LauraConfig.fetchMaxItems.getInt()));
        this.requester = player.getUUID();
        this.startTime = laura.level().getGameTime();
        this.phase = Phase.SEARCH;
        this.bag.clear();
        this.collected = 0;
        this.rejected.clear();
        this.rejectedItems.clear();
        this.usedContainers = false;
        this.usedBlocks = false;
        laura.wakeUp();
        laura.setOrderedToSit(false);
        laura.setFetching(true);
        return Result.STARTED;
    }

    public boolean isActive() {
        return spec != null;
    }

    public ItemSpec currentSpec() {
        return spec;
    }

    /** What she carries for the fetch in progress. Saved with her so that nothing is lost on the way. */
    public List<ItemStack> carried() {
        return java.util.Collections.unmodifiableList(bag);
    }

    /** Stops the errand; with {@code deliver} she still brings back what she has. */
    public void cancel(boolean deliver) {
        if (spec == null) {
            return;
        }
        if (deliver && !bag.isEmpty()) {
            phase = Phase.RETURN;
            amount = collected;
            return;
        }
        finish(false);
    }

    @Override
    public boolean canUse() {
        return spec != null && !laura.isPassenger();
    }

    @Override
    public boolean canContinueToUse() {
        return spec != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        laura.getNavigation().stop();
    }

    private ServerPlayer requesterPlayer() {
        if (!(laura.level() instanceof ServerLevel level) || requester == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(requester);
    }

    @Override
    public void tick() {
        if (spec == null) {
            return;
        }
        long now = laura.level().getGameTime();
        if (now - startTime > LauraConfig.fetchTimeoutSeconds.getInt() * 20L && phase != Phase.RETURN) {
            if (bag.isEmpty()) {
                fail("fetch.timeout");
                return;
            }
            phase = Phase.RETURN;
        }
        switch (phase) {
            case SEARCH -> search();
            case TO_ITEM -> toItem();
            case TO_CONTAINER -> toContainer();
            case TO_BLOCK -> toBlock();
            case BREAKING -> breaking();
            case RETURN -> returnToOwner();
        }
    }

    private void search() {
        if (collected >= amount) {
            phase = Phase.RETURN;
            return;
        }
        ItemEntity item = findItemEntity();
        if (item != null) {
            targetItem = item;
            phase = Phase.TO_ITEM;
            moveTicks = 0;
            return;
        }
        if (LauraConfig.fetchFromContainers.get()) {
            BlockPos container = findContainer();
            if (container != null) {
                targetPos = container;
                phase = Phase.TO_CONTAINER;
                moveTicks = 0;
                return;
            }
        }
        if (LauraConfig.fetchBreakBlocks.get() && laura.level() instanceof ServerLevel level
                && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            BlockPos block = findHarvestable();
            if (block != null) {
                targetPos = block;
                phase = Phase.TO_BLOCK;
                moveTicks = 0;
                return;
            }
        }
        if (bag.isEmpty()) {
            fail("fetch.not_found");
        } else {
            phase = Phase.RETURN;
        }
    }

    private ItemEntity findItemEntity() {
        int radius = LauraConfig.fetchRadius.getInt();
        List<ItemEntity> items = laura.level().getEntitiesOfClass(ItemEntity.class, laura.getBoundingBox().inflate(radius, 8, radius),
                e -> e.isAlive() && !e.getItem().isEmpty() && spec.test(e.getItem()) && !rejectedItems.contains(e.getId()) && !e.hasPickUpDelay());
        return items.stream().min(Comparator.comparingDouble(laura::distanceToSqr)).orElse(null);
    }

    private BlockPos findContainer() {
        if (!(laura.level() instanceof ServerLevel level)) {
            return null;
        }
        for (BlockPos pos : LauraInventories.storagesAround(level, laura.blockPosition(), LauraConfig.fetchRadius.getInt(), p -> !rejected.contains(p))) {
            InventoryAccess inventory = LauraInventories.at(level, pos);
            if (inventory != null && inventory.hasAnyMatching(spec)) {
                // Locked, or not hers and her partner could not open it either: she leaves it alone.
                if (LauraInventories.mayUse(laura, pos)) {
                    return pos;
                }
                rejected.add(pos);
            }
        }
        return null;
    }

    private BlockPos findHarvestable() {
        int radius = Math.min(LauraConfig.fetchRadius.getInt(), 20);
        Optional<BlockPos> found = BlockPos.findClosestMatch(laura.blockPosition(), radius, 6, pos -> !rejected.contains(pos) && canHarvest(pos, laura.level().getBlockState(pos)));
        return found.map(BlockPos::immutable).orElse(null);
    }

    private boolean canHarvest(BlockPos pos, BlockState state) {
        if (!state.is(LauraRegistries.FETCH_HARVESTABLE)) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state) && producesWanted(state, pos);
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2 && spec.test(new ItemStack(net.minecraft.world.item.Items.SWEET_BERRIES));
        }
        if (block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO || block == Blocks.CACTUS) {
            // Keep the bottom of the plant so it grows back.
            return laura.level().getBlockState(pos.below()).is(block) && spec.test(new ItemStack(block.asItem()));
        }
        return producesWanted(state, pos);
    }

    private boolean producesWanted(BlockState state, BlockPos pos) {
        if (spec.test(new ItemStack(state.getBlock().asItem()))) {
            return true;
        }
        if (laura.level() instanceof ServerLevel level && state.getBlock() instanceof CropBlock) {
            for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
                if (spec.test(drop)) {
                    return true;
                }
            }
        }
        return state.is(net.minecraft.world.level.block.Blocks.MELON) && spec.test(new ItemStack(net.minecraft.world.item.Items.MELON_SLICE));
    }

    private boolean moveTo(double x, double y, double z, double reach) {
        double distSq = laura.distanceToSqr(x, y, z);
        if (distSq <= reach * reach) {
            return true;
        }
        if (moveTicks % 20 == 0) {
            laura.getNavigation().moveTo(x, y, z, 1.15);
        }
        moveTicks++;
        return false;
    }

    private boolean unreachable() {
        return moveTicks > 240 || moveTicks > 40 && laura.getNavigation().isDone() && laura.getDeltaMovement().horizontalDistanceSqr() < 0.0001;
    }

    private void toItem() {
        if (targetItem == null || !targetItem.isAlive()) {
            phase = Phase.SEARCH;
            return;
        }
        laura.getLookControl().setLookAt(targetItem);
        if (moveTo(targetItem.getX(), targetItem.getY(), targetItem.getZ(), 1.4)) {
            ItemStack stack = targetItem.getItem();
            int take = Math.min(stack.getCount(), amount - collected);
            ItemStack taken = stack.split(take);
            laura.take(targetItem, take);
            if (stack.isEmpty()) {
                targetItem.discard();
            } else {
                targetItem.setItem(stack);
            }
            addToBag(taken);
            phase = Phase.SEARCH;
        } else if (unreachable()) {
            rejectedItems.add(targetItem.getId());
            phase = Phase.SEARCH;
        }
    }

    private void toContainer() {
        if (moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 2.3)) {
            InventoryAccess inventory = laura.level() instanceof ServerLevel level ? LauraInventories.at(level, targetPos) : null;
            if (inventory != null && !LauraInventories.isLocked(laura, targetPos)) {
                openChest(true);
                for (int slot = 0; slot < inventory.size() && collected < amount; slot++) {
                    ItemStack stack = inventory.get(slot);
                    if (!stack.isEmpty() && spec.test(stack)) {
                        addToBag(inventory.extract(slot, Math.min(stack.getCount(), amount - collected)));
                    }
                }
                laura.swing(InteractionHand.MAIN_HAND);
                usedContainers = true;
                if (laura.level() instanceof ServerLevel level) {
                    level.getServer().schedule(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 15, () -> openChest(false)));
                }
            }
            rejected.add(targetPos);
            phase = Phase.SEARCH;
        } else if (unreachable()) {
            rejected.add(targetPos);
            phase = Phase.SEARCH;
        }
    }

    private void openChest(boolean open) {
        BlockPos pos = targetPos;
        if (pos == null) {
            return;
        }
        BlockState state = laura.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            laura.level().blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            laura.level().playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
        } else if (open) {
            laura.level().playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    private void toBlock() {
        BlockState state = laura.level().getBlockState(targetPos);
        if (!canHarvest(targetPos, state)) {
            phase = Phase.SEARCH;
            return;
        }
        laura.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        if (moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 2.6)) {
            phase = Phase.BREAKING;
            progress = 0;
            laura.getNavigation().stop();
        } else if (unreachable()) {
            rejected.add(targetPos);
            phase = Phase.SEARCH;
        }
    }

    private void breaking() {
        if (!(laura.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(targetPos);
        if (!canHarvest(targetPos, state)) {
            level.destroyBlockProgress(laura.getId(), targetPos, -1);
            phase = Phase.SEARCH;
            return;
        }
        laura.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        float hardness = Math.max(0.1F, state.getDestroySpeed(level, targetPos));
        int needed = (int) Math.min(80, 8 + hardness * 12);
        progress++;
        if (progress % 5 == 0) {
            laura.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, targetPos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        level.destroyBlockProgress(laura.getId(), targetPos, Math.min(9, progress * 10 / needed));
        if (progress < needed) {
            return;
        }
        level.destroyBlockProgress(laura.getId(), targetPos, -1);
        Block block = state.getBlock();
        List<ItemStack> drops;
        if (block instanceof SweetBerryBushBlock) {
            int count = 1 + level.getRandom().nextInt(2) + (state.getValue(SweetBerryBushBlock.AGE) == 3 ? 1 : 0);
            drops = new ArrayList<>(List.of(new ItemStack(net.minecraft.world.item.Items.SWEET_BERRIES, count)));
            level.setBlock(targetPos, state.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_CLIENTS);
            level.playSound(null, targetPos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            drops = new ArrayList<>(Block.getDrops(state, level, targetPos, level.getBlockEntity(targetPos), laura, ItemStack.EMPTY));
            level.destroyBlock(targetPos, false, laura);
            if (block instanceof CropBlock crop && LauraConfig.fetchReplantCrops.get()) {
                ItemStack seed = new ItemStack(block.asItem());
                for (ItemStack drop : drops) {
                    if (ItemStack.isSameItem(drop, seed) && !drop.isEmpty()) {
                        drop.shrink(1);
                        level.setBlock(targetPos, crop.getStateForAge(0), Block.UPDATE_ALL);
                        break;
                    }
                }
            }
        }
        usedBlocks = true;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            if (spec.test(drop) && collected < amount) {
                int take = Math.min(drop.getCount(), amount - collected);
                addToBag(drop.split(take));
            }
            if (!drop.isEmpty()) {
                // Extra drops go to her own inventory, or on the ground if it is full.
                ItemStack rest = laura.bags().add(drop);
                if (!rest.isEmpty()) {
                    laura.spawnAtLocation(rest);
                }
            }
        }
        laura.brain().needs().add(Needs.Need.ENERGY, -1.5F);
        laura.brain().needs().add(Needs.Need.HYGIENE, -1F);
        phase = Phase.SEARCH;
    }

    private void addToBag(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        collected += stack.getCount();
        for (ItemStack existing : bag) {
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            bag.add(stack);
        }
        laura.setCarried(bag.get(0));
    }

    private void returnToOwner() {
        ServerPlayer player = requesterPlayer();
        if (player == null || player.level() != laura.level()) {
            // Partner gone: keep the items for later.
            for (ItemStack stack : bag) {
                ItemStack rest = laura.bags().add(stack);
                if (!rest.isEmpty()) {
                    laura.spawnAtLocation(rest);
                }
            }
            bag.clear();
            finish(false);
            return;
        }
        laura.getLookControl().setLookAt(player);
        double teleport = LauraConfig.teleportDistance.getDouble();
        if (laura.distanceToSqr(player) > teleport * teleport || unreachable()) {
            LauraMovement.teleportNear(laura, player.blockPosition());
            moveTicks = 0;
        }
        if (moveTo(player.getX(), player.getY(), player.getZ(), 2.5)) {
            int total = 0;
            ItemStack first = bag.isEmpty() ? ItemStack.EMPTY : bag.get(0).copy();
            for (ItemStack stack : bag) {
                total += stack.getCount();
                laura.brain().giveToPlayer(player, stack);
            }
            bag.clear();
            laura.swing(InteractionHand.MAIN_HAND);
            laura.playEmote(Emote.BOW);
            laura.brain().needs().add(Needs.Need.ATTENTION, 5);
            LineFormatter.Values values = LineFormatter.values().with("item", first.isEmpty() ? spec.displayName() : first.getHoverName()).with("count", total);
            LauraSpeech.say(laura, player, usedBlocks ? "fetch.delivered.harvested" : usedContainers ? "fetch.delivered.container" : "fetch.delivered", values);
            LauraAdvancements.add(player, "fetched", Math.max(1, total));
            finish(true);
        }
    }

    private void fail(String key) {
        ServerPlayer player = requesterPlayer();
        if (player != null) {
            LauraSpeech.say(laura, player, key, LineFormatter.values().with("item", spec.displayName()));
        }
        laura.playEmote(Emote.SHRUG);
        finish(false);
    }

    private void finish(boolean success) {
        if (laura.level() instanceof ServerLevel level && targetPos != null) {
            level.destroyBlockProgress(laura.getId(), targetPos, -1);
        }
        for (ItemStack stack : bag) {
            ItemStack rest = laura.bags().add(stack);
            if (!rest.isEmpty()) {
                laura.spawnAtLocation(rest);
            }
        }
        bag.clear();
        spec = null;
        requester = null;
        targetItem = null;
        targetPos = null;
        laura.setFetching(false);
        laura.setCarried(ItemStack.EMPTY);
        laura.getNavigation().stop();
    }
}
