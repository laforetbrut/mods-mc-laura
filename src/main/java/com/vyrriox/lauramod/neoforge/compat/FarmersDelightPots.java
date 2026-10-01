package com.vyrriox.lauramod.neoforge.compat;

import com.vyrriox.lauramod.platform.CookingPots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Farmer's Delight cooking pots for Laura's cook job. Only loaded when Farmer's Delight is installed.
 *
 * @author vyrriox
 */
public final class FarmersDelightPots implements CookingPots {
    private static final int INPUTS = 6;

    private static CookingPotBlockEntity pot(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot ? pot : null;
    }

    @Override
    public boolean isPot(ServerLevel level, BlockPos pos) {
        return pot(level, pos) != null;
    }

    @Override
    public boolean isReadyForIngredients(ServerLevel level, BlockPos pos) {
        CookingPotBlockEntity pot = pot(level, pos);
        if (pot == null || !pot.isHeated()) {
            return false;
        }
        ItemStackHandler inv = pot.getInventory();
        for (int i = 0; i < INPUTS; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return inv.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT).isEmpty() && inv.getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    @Override
    public boolean hasMeals(ServerLevel level, BlockPos pos) {
        CookingPotBlockEntity pot = pot(level, pos);
        return pot != null && !pot.getInventory().getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    @Override
    public boolean isCooking(ServerLevel level, BlockPos pos) {
        CookingPotBlockEntity pot = pot(level, pos);
        if (pot == null) {
            return false;
        }
        ItemStackHandler inv = pot.getInventory();
        for (int i = 0; i < INPUTS; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return !inv.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT).isEmpty();
    }

    @Override
    public List<Recipe> recipes(ServerLevel level) {
        List<Recipe> out = new ArrayList<>();
        for (RecipeHolder<CookingPotRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get())) {
            CookingPotRecipe recipe = holder.value();
            List<Ingredient> ingredients = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (!ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }
            if (ingredients.isEmpty() || ingredients.size() > INPUTS) {
                continue;
            }
            out.add(new Recipe(List.copyOf(ingredients), recipe.getResultItem(level.registryAccess()).copy(), recipe.getOutputContainer().copy()));
        }
        return out;
    }

    @Override
    public boolean load(ServerLevel level, BlockPos pos, List<ItemStack> ingredients, ItemStack containers) {
        if (!isReadyForIngredients(level, pos) || ingredients.size() > INPUTS) {
            return false;
        }
        CookingPotBlockEntity pot = pot(level, pos);
        ItemStackHandler inv = pot.getInventory();
        ItemStack current = inv.getStackInSlot(CookingPotBlockEntity.CONTAINER_SLOT);
        if (!containers.isEmpty() && !current.isEmpty() && !ItemStack.isSameItemSameComponents(current, containers)) {
            // Another container is in the slot: the meal could not be served. Nothing goes into the pot.
            return false;
        }
        for (int i = 0; i < ingredients.size(); i++) {
            inv.setStackInSlot(i, ingredients.get(i));
        }
        if (!containers.isEmpty()) {
            // The slot takes what fits; the rest stays in the stack, which the caller keeps.
            int room = Math.min(inv.getSlotLimit(CookingPotBlockEntity.CONTAINER_SLOT), containers.getMaxStackSize()) - current.getCount();
            int move = Math.max(0, Math.min(containers.getCount(), room));
            if (move > 0) {
                inv.setStackInSlot(CookingPotBlockEntity.CONTAINER_SLOT, containers.copyWithCount(current.getCount() + move));
                containers.shrink(move);
            }
        }
        pot.setChanged();
        return true;
    }

    @Override
    public ItemStack collect(ServerLevel level, BlockPos pos) {
        CookingPotBlockEntity pot = pot(level, pos);
        if (pot == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = pot.getInventory().extractItem(CookingPotBlockEntity.OUTPUT_SLOT, 64, false);
        pot.setChanged();
        return out;
    }
}
