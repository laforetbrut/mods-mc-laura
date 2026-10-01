package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.platform.InventoryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Everything Laura carries: her own bag, plus the backpack she wears on her back when it has an
 * inventory (Sophisticated Backpacks, Traveler's Backpack and any item exposing the loader's item
 * handler). Items go to her bag first, then to the backpack.
 *
 * @author vyrriox
 */
public final class LauraBags {
    /** Items she can wear on her back, in addition to anything with "backpack" in its id. */
    public static final TagKey<Item> WEARABLE_ON_BACK = TagKey.create(Registries.ITEM, LauraMod.id("wearable_on_back"));

    private final LauraEntity laura;
    private final SimpleContainer bag;
    @Nullable
    private final InventoryAccess backpack;

    LauraBags(LauraEntity laura) {
        this.laura = laura;
        this.bag = laura.inventory();
        ItemStack back = laura.getBackItem();
        InventoryAccess access = null;
        if (!back.isEmpty() && LauraMod.isInitialized() && !laura.level().isClientSide) {
            access = LauraMod.platform().itemInventory(back);
        }
        this.backpack = access;
    }

    /** True for backpacks and other items she can wear on her back. */
    public static boolean isWearableOnBack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(WEARABLE_ON_BACK)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().contains("backpack") || id.getNamespace().contains("backpack");
    }

    /** True when she wears a backpack that adds storage. */
    public boolean hasBackpack() {
        return backpack != null;
    }

    public int backpackSlots() {
        return backpack == null ? 0 : backpack.size();
    }

    /** Stores a stack; returns what did not fit anywhere. */
    public ItemStack add(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack rest = bag.addItem(stack);
        if (!rest.isEmpty() && backpack != null) {
            rest = backpack.insert(rest);
        }
        return rest;
    }

    public boolean canAdd(ItemStack stack) {
        return bag.canAddItem(stack) || backpack != null && backpack.canInsert(stack);
    }

    public int count(Predicate<ItemStack> filter) {
        int n = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack s = bag.getItem(i);
            if (!s.isEmpty() && filter.test(s)) {
                n += s.getCount();
            }
        }
        if (backpack != null) {
            for (int i = 0; i < backpack.size(); i++) {
                ItemStack s = backpack.get(i);
                if (!s.isEmpty() && filter.test(s)) {
                    n += s.getCount();
                }
            }
        }
        return n;
    }

    /** A copy of the first matching stack, or EMPTY. */
    public ItemStack peek(Predicate<ItemStack> filter) {
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack s = bag.getItem(i);
            if (!s.isEmpty() && filter.test(s)) {
                return s.copy();
            }
        }
        if (backpack != null) {
            for (int i = 0; i < backpack.size(); i++) {
                ItemStack s = backpack.get(i);
                if (!s.isEmpty() && filter.test(s)) {
                    return s.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** Removes up to {@code max} matching items, from her bag first. */
    public List<ItemStack> take(Predicate<ItemStack> filter, int max) {
        List<ItemStack> out = new ArrayList<>();
        int left = max;
        for (int i = 0; i < bag.getContainerSize() && left > 0; i++) {
            ItemStack s = bag.getItem(i);
            if (!s.isEmpty() && filter.test(s)) {
                ItemStack taken = s.split(Math.min(left, s.getCount()));
                left -= taken.getCount();
                out.add(taken);
            }
        }
        bag.setChanged();
        if (backpack != null) {
            for (int i = 0; i < backpack.size() && left > 0; i++) {
                ItemStack s = backpack.get(i);
                if (!s.isEmpty() && filter.test(s)) {
                    ItemStack taken = backpack.extract(i, Math.min(left, s.getCount()));
                    left -= taken.getCount();
                    if (!taken.isEmpty()) {
                        out.add(taken);
                    }
                }
            }
        }
        return out;
    }

    /** Moves matching items into another inventory. Returns how many moved. */
    public int moveInto(InventoryAccess target, Predicate<ItemStack> filter) {
        int moved = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack s = bag.getItem(i);
            if (!s.isEmpty() && filter.test(s)) {
                int before = s.getCount();
                ItemStack rest = target.insert(s.copy());
                moved += before - rest.getCount();
                bag.setItem(i, rest);
            }
        }
        bag.setChanged();
        if (backpack != null) {
            for (int i = 0; i < backpack.size(); i++) {
                ItemStack s = backpack.get(i);
                if (!s.isEmpty() && filter.test(s)) {
                    // Take first, then put back what did not fit: never duplicates items.
                    ItemStack taken = backpack.extract(i, s.getCount());
                    ItemStack rest = target.insert(taken);
                    moved += taken.getCount() - rest.getCount();
                    if (!rest.isEmpty()) {
                        ItemStack lost = backpack.insert(rest);
                        if (!lost.isEmpty()) {
                            lost = bag.addItem(lost);
                        }
                        if (!lost.isEmpty()) {
                            laura.spawnAtLocation(lost);
                        }
                    }
                }
            }
        }
        return moved;
    }

    /** Free slots of her own bag (the backpack is extra room, used when the bag is full). */
    public int freeBagSlots() {
        int free = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            if (bag.getItem(i).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /** True when neither her bag nor her backpack has room left for new kinds of items. */
    public boolean almostFull() {
        if (freeBagSlots() > 2) {
            return false;
        }
        if (backpack == null) {
            return true;
        }
        int free = 0;
        for (int i = 0; i < backpack.size(); i++) {
            if (backpack.get(i).isEmpty()) {
                free++;
            }
        }
        return free <= 2;
    }
}
