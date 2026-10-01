package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.LauraInventories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Everything about Laura's work: assigned containers, active jobs with their areas, and the queue of
 * errands she has been asked to do.
 *
 * @author vyrriox
 */
public final class LauraWorkplace {
    public static final int MAX_CHESTS = 48;
    public static final int MAX_QUEUE = 16;

    public record ChestAssignment(BlockPos pos, ResourceKey<Level> dimension, ChestPurpose purpose) {
    }

    private final LauraEntity laura;
    private final List<ChestAssignment> chests = new ArrayList<>();
    private final EnumMap<LauraJob, WorkArea> jobs = new EnumMap<>(LauraJob.class);
    private final Deque<LauraTask> queue = new ArrayDeque<>();
    private LauraTask current;

    public LauraWorkplace(LauraEntity laura) {
        this.laura = laura;
    }

    // ------------------------------------------------------------------ chests

    public boolean assign(BlockPos pos, ResourceKey<Level> dimension, ChestPurpose purpose) {
        chests.removeIf(c -> c.pos().equals(pos) && c.dimension().equals(dimension));
        if (chests.size() >= MAX_CHESTS) {
            return false;
        }
        chests.add(new ChestAssignment(pos.immutable(), dimension, purpose));
        return true;
    }

    public boolean unassign(BlockPos pos, ResourceKey<Level> dimension) {
        return chests.removeIf(c -> c.pos().equals(pos) && c.dimension().equals(dimension));
    }

    public void clearChests() {
        chests.clear();
    }

    public List<ChestAssignment> chests() {
        return Collections.unmodifiableList(chests);
    }

    public ChestPurpose purposeOf(BlockPos pos) {
        for (ChestAssignment c : chests) {
            if (c.pos().equals(pos) && c.dimension().equals(laura.level().dimension())) {
                return c.purpose();
            }
        }
        return null;
    }

    private InventoryAccess inventory(BlockPos pos) {
        return laura.level() instanceof ServerLevel level ? LauraInventories.at(level, pos) : null;
    }

    /** Loaded containers assigned to a purpose in her current dimension, nearest first. */
    public List<BlockPos> assigned(ChestPurpose purpose) {
        List<BlockPos> out = new ArrayList<>();
        Level level = laura.level();
        for (ChestAssignment c : chests) {
            if (c.purpose() == purpose && c.dimension().equals(level.dimension()) && level.isLoaded(c.pos()) && inventory(c.pos()) != null) {
                out.add(c.pos());
            }
        }
        out.sort(Comparator.comparingDouble(p -> p.distSqr(laura.blockPosition())));
        return out;
    }

    /**
     * Where to store a stack for a purpose: an assigned chest of that purpose with room, then an
     * assigned STORAGE chest, then (if allowed) any unassigned container in the area.
     */
    public BlockPos findDepositTarget(ChestPurpose purpose, ItemStack sample, WorkArea area) {
        for (BlockPos pos : assigned(purpose)) {
            if (hasRoom(pos, sample)) {
                return pos;
            }
        }
        for (BlockPos pos : assigned(ChestPurpose.STORAGE)) {
            if (hasRoom(pos, sample)) {
                return pos;
            }
        }
        if (area != null && LauraConfig.depositInChests.get() && chests.stream().noneMatch(c -> c.purpose() == purpose || c.purpose() == ChestPurpose.STORAGE)) {
            for (BlockPos pos : containersIn(area, false)) {
                if (purposeOf(pos) == null && hasRoom(pos, sample)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** A container holding a matching item: assigned ones first, then any unassigned one in the area. */
    public BlockPos findSource(ChestPurpose purpose, Predicate<ItemStack> wanted, WorkArea area) {
        for (BlockPos pos : assigned(purpose)) {
            InventoryAccess inv = inventory(pos);
            if (inv != null && inv.hasAnyMatching(wanted)) {
                return pos;
            }
        }
        if (area != null) {
            for (BlockPos pos : containersIn(area, false)) {
                ChestPurpose p = purposeOf(pos);
                boolean usable = p == null || p == purpose || p == ChestPurpose.STORAGE;
                InventoryAccess inv = usable ? inventory(pos) : null;
                if (inv != null && inv.hasAnyMatching(wanted)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean hasRoom(BlockPos pos, ItemStack sample) {
        if (laura.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity) {
            return false;
        }
        InventoryAccess inv = inventory(pos);
        return inv != null && inv.canInsert(sample);
    }

    /** Storage blocks (modded ones included, machines excluded) whose position is inside the area, nearest first. */
    public List<BlockPos> containersIn(WorkArea area, boolean includeFurnaces) {
        if (!(laura.level() instanceof ServerLevel level) || !area.isIn(level)) {
            return new ArrayList<>();
        }
        List<BlockPos> out = LauraInventories.storagesAround(level, area.center(), area.radius() + 2, area::contains);
        out.sort(Comparator.comparingDouble(p -> p.distSqr(laura.blockPosition())));
        return out;
    }

    // ------------------------------------------------------------------ jobs

    public void enableJob(LauraJob job, WorkArea area) {
        if (job != LauraJob.NONE) {
            jobs.put(job, area);
        }
    }

    public void disableJob(LauraJob job) {
        jobs.remove(job);
    }

    public void clearJobs() {
        jobs.clear();
    }

    public Map<LauraJob, WorkArea> jobs() {
        return Collections.unmodifiableMap(jobs);
    }

    public boolean hasJobs() {
        return !jobs.isEmpty();
    }

    // ------------------------------------------------------------------ task queue

    public boolean enqueue(LauraTask task) {
        if (queue.size() >= MAX_QUEUE) {
            return false;
        }
        queue.addLast(task);
        return true;
    }

    /** Puts a task in front of the queue and makes it current (the current errand is dropped). */
    public void replaceCurrent(LauraTask task) {
        current = task;
    }

    public LauraTask current() {
        return current;
    }

    /** Moves to the next task. Returns it (or null when the queue is empty). */
    public LauraTask advance() {
        current = queue.pollFirst();
        return current;
    }

    public void finishCurrent() {
        current = null;
    }

    public List<LauraTask> queued() {
        return List.copyOf(queue);
    }

    public void clearQueue() {
        queue.clear();
        current = null;
    }

    public boolean removeQueued(int index) {
        if (index < 0 || index >= queue.size()) {
            return false;
        }
        List<LauraTask> list = new ArrayList<>(queue);
        list.remove(index);
        queue.clear();
        queue.addAll(list);
        return true;
    }

    // ------------------------------------------------------------------ persistence

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag chestList = new ListTag();
        for (ChestAssignment c : chests) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("Pos", c.pos().asLong());
            ct.putString("Dim", c.dimension().location().toString());
            ct.putString("Purpose", c.purpose().name());
            chestList.add(ct);
        }
        tag.put("Chests", chestList);
        CompoundTag jobTag = new CompoundTag();
        for (Map.Entry<LauraJob, WorkArea> e : jobs.entrySet()) {
            jobTag.put(e.getKey().name(), e.getValue().save());
        }
        tag.put("Jobs", jobTag);
        ListTag queueList = new ListTag();
        if (current != null) {
            queueList.add(current.save());
        }
        for (LauraTask task : queue) {
            queueList.add(task.save());
        }
        tag.put("Queue", queueList);
        return tag;
    }

    public void load(CompoundTag tag) {
        chests.clear();
        ListTag chestList = tag.getList("Chests", Tag.TAG_COMPOUND);
        for (int i = 0; i < chestList.size(); i++) {
            CompoundTag ct = chestList.getCompound(i);
            ChestPurpose purpose = ChestPurpose.byName(ct.getString("Purpose"));
            ResourceLocation dim = ResourceLocation.tryParse(ct.getString("Dim"));
            if (purpose != null && dim != null) {
                chests.add(new ChestAssignment(BlockPos.of(ct.getLong("Pos")), ResourceKey.create(Registries.DIMENSION, dim), purpose));
            }
        }
        jobs.clear();
        CompoundTag jobTag = tag.getCompound("Jobs");
        for (String key : jobTag.getAllKeys()) {
            LauraJob job = LauraJob.byName(key);
            if (job != null && job != LauraJob.NONE) {
                jobs.put(job, WorkArea.load(jobTag.getCompound(key)));
            }
        }
        queue.clear();
        current = null;
        ListTag queueList = tag.getList("Queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < queueList.size(); i++) {
            LauraTask task = LauraTask.load(queueList.getCompound(i));
            if (task != null) {
                queue.addLast(task);
            }
        }
    }
}
