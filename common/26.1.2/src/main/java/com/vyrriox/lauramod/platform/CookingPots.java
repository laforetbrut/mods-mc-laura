package com.vyrriox.lauramod.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Cooking pots of food mods (Farmer's Delight): the cook fills them with the ingredients of a
 * recipe and the containers, waits, and collects the meals. The loader glue provides the real
 * implementation when such a mod is installed.
 *
 * @author vyrriox
 */
public interface CookingPots {
    CookingPots NONE = new CookingPots() {
        @Override
        public boolean isPot(ServerLevel level, BlockPos pos) {
            return false;
        }

        @Override
        public boolean isReadyForIngredients(ServerLevel level, BlockPos pos) {
            return false;
        }

        @Override
        public boolean hasMeals(ServerLevel level, BlockPos pos) {
            return false;
        }

        @Override
        public boolean isCooking(ServerLevel level, BlockPos pos) {
            return false;
        }

        @Override
        public List<Recipe> recipes(ServerLevel level) {
            return List.of();
        }

        @Override
        public boolean load(ServerLevel level, BlockPos pos, List<ItemStack> ingredients, ItemStack containers) {
            return false;
        }

        @Override
        public ItemStack collect(ServerLevel level, BlockPos pos) {
            return ItemStack.EMPTY;
        }
    };

    /** A pot recipe: one item per ingredient, the result and the container it is served in. */
    record Recipe(List<Ingredient> ingredients, ItemStack result, ItemStack container) {
    }

    boolean isPot(ServerLevel level, BlockPos pos);

    /** Heated, with no ingredient, no meal inside: ready for a new recipe. */
    boolean isReadyForIngredients(ServerLevel level, BlockPos pos);

    /** Served meals are waiting to be taken. */
    boolean hasMeals(ServerLevel level, BlockPos pos);

    /** Ingredients are cooking or a meal waits for containers. */
    boolean isCooking(ServerLevel level, BlockPos pos);

    List<Recipe> recipes(ServerLevel level);

    /**
     * Puts the ingredients and the containers into the pot. False if it could not, and then the pot
     * is left untouched (another kind of container already sits in its container slot). On success
     * the containers the pot took are removed from the {@code containers} stack: what is left in it
     * did not fit and still belongs to the caller.
     */
    boolean load(ServerLevel level, BlockPos pos, List<ItemStack> ingredients, ItemStack containers);

    /** Takes the served meals out. */
    ItemStack collect(ServerLevel level, BlockPos pos);
}
