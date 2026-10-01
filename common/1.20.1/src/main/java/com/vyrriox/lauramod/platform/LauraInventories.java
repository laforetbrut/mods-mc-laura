package com.vyrriox.lauramod.platform;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
                && !isCrafter(be);
    }

    /**
     * Crafters. Minecraft 1.20.1 has none of its own, so these are the ones of other mods. The
     * registry id is tested first: a class name can be anything, and the names of the game are
     * remapped in a release jar.
     */
    private static boolean isCrafter(BlockEntity be) {
        return isCrafter(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()), be.getClass().getName());
    }

    /** The rule of {@link #isCrafter(BlockEntity)} on plain names, which the self tests check without a crafter block. */
    public static boolean isCrafter(@Nullable ResourceLocation typeId, String className) {
        return typeId != null && typeId.getPath().contains("crafter")
                || className.toLowerCase(Locale.ROOT).contains("crafter");
    }

    // ------------------------------------------------------------------ permissions

    /** The lock field of vanilla containers, found by its type so that it works with every mapping. */
    private static final Field LOCK_FIELD = findLockField();

    private static Field findLockField() {
        for (Field field : BaseContainerBlockEntity.class.getDeclaredFields()) {
            if (field.getType() == LockCode.class && !Modifier.isStatic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException e) {
                    LauraMod.LOGGER.debug("Container locks will be read from their saved data: {}", e.toString());
                }
            }
        }
        return null;
    }

    /** The vanilla lock of a container ("Lock" in its data), or null when the block cannot have one. */
    @Nullable
    private static LockCode lockOf(ServerLevel level, @Nullable BlockEntity be) {
        if (!(be instanceof BaseContainerBlockEntity)) {
            return null;
        }
        try {
            if (LOCK_FIELD != null) {
                return (LockCode) LOCK_FIELD.get(be);
            }
            return LockCode.fromTag(be.saveWithoutMetadata());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** True when the block at pos has a vanilla lock and neither she nor her partner holds its key. */
    private static boolean isLockedFor(ServerLevel level, BlockPos pos, LauraEntity laura, @Nullable ServerPlayer owner) {
        LockCode lock = lockOf(level, level.getBlockEntity(pos));
        if (lock == null || lock.unlocksWith(laura.getMainHandItem())) {
            return false;
        }
        return owner == null || !lock.unlocksWith(owner.getMainHandItem());
    }

    /**
     * True when the container at pos is locked for her. A vanilla lock is respected the way a player
     * has to respect it: the key must be in her hand or in her partner's. Both halves of a double
     * chest count.
     */
    public static boolean isLocked(LauraEntity laura, BlockPos pos) {
        if (!(laura.level() instanceof ServerLevel level) || !level.isLoaded(pos)) {
            return false;
        }
        ServerPlayer owner = LauraSpeech.owner(laura);
        if (isLockedFor(level, pos, laura, owner)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            return level.isLoaded(other) && isLockedFor(level, other, laura, owner);
        }
        return false;
    }

    /**
     * True when she may take from or put into the container at pos. A locked container is never
     * used. A container her partner assigned to her is hers to use. Any other container is only used
     * on behalf of her partner: that player must be there, in the same dimension, and be allowed to
     * open it (spawn protection, world border, and the claims of other mods, asked through the loader).
     */
    public static boolean mayUse(LauraEntity laura, BlockPos pos) {
        if (!(laura.level() instanceof ServerLevel level) || isLocked(laura, pos)) {
            return false;
        }
        if (laura.workplace().purposeOf(pos) != null) {
            return true;
        }
        ServerPlayer owner = LauraSpeech.owner(laura);
        return owner != null && playerMayUse(owner, level, pos);
    }

    /** True when the player may open the container at pos: the world rules first, then the other mods. */
    public static boolean playerMayUse(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.level() != level || !level.mayInteract(player, pos)) {
            return false;
        }
        try {
            return LauraMod.platform().mayUseContainer(player, level, pos);
        } catch (RuntimeException e) {
            LauraMod.LOGGER.debug("Container permission check failed at {}: {}", pos, e.toString());
            return false;
        }
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
