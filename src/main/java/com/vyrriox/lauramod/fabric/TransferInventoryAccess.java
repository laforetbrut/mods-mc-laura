package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.platform.InventoryAccess;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Any block exposing an item storage through the Fabric Transfer API: vanilla containers, modded
 * chests, drawers, storage network interfaces... Seen as slots, so the common code can browse it
 * like a vanilla container.
 *
 * @author vyrriox
 */
abstract class TransferInventoryAccess implements InventoryAccess {
    protected final Storage<ItemVariant> storage;

    private TransferInventoryAccess(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    /** The item storage of the block at pos (whole inventory, no side), or null when there is none. */
    static InventoryAccess at(ServerLevel level, BlockPos pos) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, null);
        if (storage == null) {
            return null;
        }
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            return new Slotted(slotted);
        }
        return new Views(storage);
    }

    /** The view behind a slot index, or null when the index is out of range. */
    protected abstract StorageView<ItemVariant> view(int slot);

    @Override
    public ItemStack get(int slot) {
        StorageView<ItemVariant> view = view(slot);
        if (view == null || view.isResourceBlank() || view.getAmount() <= 0) {
            return ItemStack.EMPTY;
        }
        ItemVariant resource = view.getResource();
        return resource.toStack(clampToStack(resource, view.getAmount()));
    }

    @Override
    public ItemStack extract(int slot, int amount) {
        StorageView<ItemVariant> view = view(slot);
        if (amount <= 0 || view == null || view.isResourceBlank() || Transaction.isOpen()) {
            return ItemStack.EMPTY;
        }
        ItemVariant resource = view.getResource();
        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = view.extract(resource, clampToStack(resource, amount), transaction);
            transaction.commit();
        }
        return moved <= 0 ? ItemStack.EMPTY : resource.toStack((int) moved);
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || Transaction.isOpen()) {
            return stack;
        }
        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
        return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) moved);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty() || Transaction.isOpen()) {
            return false;
        }
        // Simulated: the transaction is closed without commit, so nothing moves.
        try (Transaction transaction = Transaction.openOuter()) {
            return storage.insert(ItemVariant.of(stack), 1, transaction) == 1;
        }
    }

    /** A slot shows at most one full stack, even in storages that hold more of an item. */
    private static int clampToStack(ItemVariant resource, long amount) {
        int max = Math.max(1, resource.toStack().getMaxStackSize());
        return (int) Math.min(amount, max);
    }

    /** Storages with real slots (vanilla containers and most modded chests). */
    private static final class Slotted extends TransferInventoryAccess {
        private final SlottedStorage<ItemVariant> slotted;

        Slotted(SlottedStorage<ItemVariant> slotted) {
            super(slotted);
            this.slotted = slotted;
        }

        @Override
        public int size() {
            return slotted.getSlotCount();
        }

        @Override
        protected StorageView<ItemVariant> view(int slot) {
            return slot < 0 || slot >= slotted.getSlotCount() ? null : slotted.getSlot(slot);
        }
    }

    /** Storages without slots (networks, drawers): the views present when the inventory was opened. */
    private static final class Views extends TransferInventoryAccess {
        private final List<StorageView<ItemVariant>> views = new ArrayList<>();

        Views(Storage<ItemVariant> storage) {
            super(storage);
            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                views.add(view);
            }
        }

        @Override
        public int size() {
            return views.size();
        }

        @Override
        protected StorageView<ItemVariant> view(int slot) {
            return slot < 0 || slot >= views.size() ? null : views.get(slot);
        }
    }
}
