package com.vyrriox.lauramod.platform;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Finds inventories in the world, modded ones included.
 *
 * @author vyrriox
 */
public final class LauraInventories {
    private LauraInventories() {
    }

    /** The inventory of the block at pos, or null. */
    @Nullable
    public static InventoryAccess at(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        InventoryAccess modded = null;
        try {
            modded = LauraMod.platform().inventoryAt(level, pos);
        } catch (RuntimeException e) {
            LauraMod.LOGGER.debug("Inventory lookup failed at {}: {}", pos, e.toString());
        }
        if (modded != null) {
            return modded;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container container ? InventoryAccess.of(container) : null;
    }

    /**
     * True for block entities that store things. Machines of vanilla (furnaces, hoppers, brewing
     * stands...) are left alone unless a player assigns them explicitly.
     */
    public static boolean isStorage(BlockEntity be) {
        return !(be instanceof AbstractFurnaceBlockEntity || be instanceof BrewingStandBlockEntity || be instanceof HopperBlockEntity
                || be instanceof DispenserBlockEntity || be instanceof JukeboxBlockEntity || be instanceof LecternBlockEntity
                || be instanceof ChiseledBookShelfBlockEntity || be instanceof CampfireBlockEntity)
                && !be.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("crafter");
    }

    /** Storage positions around a center, nearest first. */
    public static List<BlockPos> storagesAround(ServerLevel level, BlockPos center, int radius, Predicate<BlockPos> extra) {
        List<BlockPos> out = new ArrayList<>();
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = e.getKey();
                    if (pos.distSqr(center) <= (double) radius * radius && isStorage(e.getValue()) && extra.test(pos)) {
                        out.add(pos.immutable());
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return out;
    }
}
