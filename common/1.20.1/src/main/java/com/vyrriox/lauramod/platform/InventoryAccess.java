package com.vyrriox.lauramod.platform;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * A loader-independent view of an inventory. Loaders expose modded storage through it (item handler
 * capabilities on Forge and NeoForge, the Transfer API on Fabric), so Laura can use Sophisticated
 * Storage chests, AE2 and Refined Storage interfaces, Create vaults, drawers...
 *
 * @author vyrriox
 */
public interface InventoryAccess {
    int size();

    /** The stack in a slot. Do not modify it. */
    ItemStack get(int slot);

    /** Removes up to {@code amount} items from a slot and returns them. */
    ItemStack extract(int slot, int amount);

    /** Inserts as much as possible and returns what did not fit. */
    ItemStack insert(ItemStack stack);

    default boolean hasAnyMatching(Predicate<ItemStack> filter) {
        for (int i = 0; i < size(); i++) {
            ItemStack s = get(i);
            if (!s.isEmpty() && filter.test(s)) {
                return true;
            }
        }
        return false;
    }

    /** True if at least one item of this kind would fit. */
    default boolean canInsert(ItemStack stack) {
        for (int i = 0; i < size(); i++) {
            ItemStack s = get(i);
            if (s.isEmpty() || ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Wraps a vanilla container. */
    static InventoryAccess of(Container container) {
        return new InventoryAccess() {
            @Override
            public int size() {
                return container.getContainerSize();
            }

            @Override
            public ItemStack get(int slot) {
                return container.getItem(slot);
            }

            @Override
            public ItemStack extract(int slot, int amount) {
                ItemStack taken = container.removeItem(slot, amount);
                container.setChanged();
                return taken;
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                ItemStack rest = stack.copy();
                for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
                    ItemStack in = container.getItem(i);
                    if (!in.isEmpty() && ItemStack.isSameItemSameTags(in, rest) && container.canPlaceItem(i, rest)) {
                        int move = Math.min(rest.getCount(), Math.min(in.getMaxStackSize(), container.getMaxStackSize()) - in.getCount());
                        if (move > 0) {
                            in.grow(move);
                            rest.shrink(move);
                        }
                    }
                }
                for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
                    if (container.getItem(i).isEmpty() && container.canPlaceItem(i, rest)) {
                        container.setItem(i, rest.split(Math.min(rest.getCount(), Math.min(rest.getMaxStackSize(), container.getMaxStackSize()))));
                    }
                }
                container.setChanged();
                return rest;
            }
        };
    }
}
