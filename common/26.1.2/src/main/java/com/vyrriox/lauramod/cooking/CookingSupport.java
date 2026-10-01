package com.vyrriox.lauramod.cooking;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooking recipe lookups. Isolated here because recipe and fuel APIs change between versions.
 *
 * @author vyrriox
 */
public final class CookingSupport {
    private static final Map<Item, ItemStack> SMOKING_CACHE = new HashMap<>();

    private CookingSupport() {
    }

    /** Result of smoking the item (food only), or EMPTY. */
    public static ItemStack cookedResult(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack cached = SMOKING_CACHE.get(input.getItem());
        if (cached != null) {
            return cached.copy();
        }
        SingleRecipeInput one = new SingleRecipeInput(input.copyWithCount(1));
        ItemStack result = level.recipeAccess().getRecipeFor(RecipeType.SMOKING, one, level)
                .map(holder -> holder.value().assemble(one))
                .orElse(ItemStack.EMPTY);
        SMOKING_CACHE.put(input.getItem(), result.copy());
        return result;
    }

    /** Raw food that becomes better food when cooked. */
    public static boolean isRawCookable(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack result = cookedResult(level, stack);
        return !result.isEmpty() && result.get(DataComponents.FOOD) != null && !ItemStack.isSameItem(result, stack);
    }

    public static boolean isFuel(ServerLevel level, ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    /** Puts one item on a lit campfire. */
    public static boolean placeOnCampfire(ServerLevel level, CampfireBlockEntity campfire, LauraEntity laura, ItemStack stack) {
        return campfire.placeFood(level, laura, stack.copyWithCount(1));
    }

    public static void clearCache() {
        SMOKING_CACHE.clear();
    }
}
