package com.vyrriox.lauramod.fabric.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import com.vyrriox.lauramod.platform.InventoryAccess;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Applied Energistics 2: any block of an ME network (interface, ME chest, drive, cable with a
 * terminal...) gives Laura the whole network storage. Extraction and insertion use the network
 * energy like a player terminal. Only loaded when AE2 is installed.
 *
 * @author vyrriox
 */
public final class Ae2Storage implements InventoryAccess {
    private final MEStorage storage;
    private final IEnergyService energy;
    private final IActionSource source = IActionSource.empty();
    private final List<AEItemKey> keys = new ArrayList<>();
    private final List<Long> amounts = new ArrayList<>();

    private Ae2Storage(IGrid grid) {
        this.storage = grid.getStorageService().getInventory();
        this.energy = grid.getEnergyService();
        for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEItemKey item && entry.getLongValue() > 0) {
                keys.add(item);
                amounts.add(entry.getLongValue());
            }
        }
    }

    /** The network storage behind the block at pos, or null if it is not part of a powered ME network. */
    @Nullable
    public static InventoryAccess at(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = host.getGridNode(null);
        if (node == null) {
            for (Direction side : Direction.values()) {
                node = host.getGridNode(side);
                if (node != null) {
                    break;
                }
            }
        }
        if (node == null || !node.isActive()) {
            return null;
        }
        return new Ae2Storage(node.getGrid());
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public ItemStack get(int slot) {
        AEItemKey key = keys.get(slot);
        long amount = amounts.get(slot);
        return key.toStack((int) Math.min(amount, key.getMaxStackSize()));
    }

    @Override
    public ItemStack extract(int slot, int amount) {
        if (slot < 0 || slot >= keys.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = keys.get(slot);
        long taken = StorageHelper.poweredExtraction(energy, storage, key, amount, source, Actionable.MODULATE);
        amounts.set(slot, Math.max(0, amounts.get(slot) - taken));
        return taken <= 0 ? ItemStack.EMPTY : key.toStack((int) taken);
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return stack;
        }
        long inserted = StorageHelper.poweredInsert(energy, storage, key, stack.getCount(), source, Actionable.MODULATE);
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) inserted);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        return key != null && StorageHelper.poweredInsert(energy, storage, key, 1, source, Actionable.SIMULATE) > 0;
    }
}
