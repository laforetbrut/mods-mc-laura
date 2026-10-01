package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.platform.CookingPots;
import com.vyrriox.lauramod.cooking.Cookbook;
import com.vyrriox.lauramod.cooking.CookingSupport;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cook: gathers ingredients and fuel, cooks raw food in smokers, furnaces and campfires, prepares
 * meals from the cookbook at a crafting table, and stores the dishes.
 *
 * @author vyrriox
 */
public class CookWork implements Work {
    private enum Phase {
        PLAN, WALK, LOAD, COLLECT, CRAFT, GATHER, STORE, WAIT, LOAD_POT, COLLECT_POT
    }

    private static final int KEEP_FOR_HERSELF = 4;

    private final WorkContext ctx;
    private final WorkArea area;
    private final boolean oneShot;
    private final boolean forHerself;
    private final List<ItemStack> products = new ArrayList<>();
    private final Set<BlockPos> loadedStations = new HashSet<>();
    private final Set<BlockPos> rejected = new HashSet<>();
    private Phase phase = Phase.PLAN;
    private Phase next;
    private BlockPos target;
    private Cookbook.Meal meal;
    private int progress;
    private int idleCooldown;
    private int gatherCooldown;
    private int waitTicks;
    private String failKey = "work.cook.nothing";
    private final CookingPots pots = com.vyrriox.lauramod.LauraMod.platform().cookingPots();
    private List<CookingPots.Recipe> potRecipes = List.of();
    private int potRecipesAge = Integer.MAX_VALUE;
    private CookingPots.Recipe potRecipe;

    public CookWork(WorkContext ctx, WorkArea area, boolean oneShot, boolean forHerself) {
        this.ctx = ctx;
        this.area = area;
        this.oneShot = oneShot;
        this.forHerself = forHerself;
    }

    @Override
    public String failKey() {
        return failKey;
    }

    @Override
    public List<ItemStack> products() {
        return forHerself ? List.of() : products;
    }

    @Override
    public void stop() {
        ctx.laura().getNavigation().stop();
        ctx.laura().setCarried(ItemStack.EMPTY);
    }

    @Override
    public Status tick() {
        if (gatherCooldown > 0) {
            gatherCooldown--;
        }
        return switch (phase) {
            case PLAN -> plan();
            case WALK -> walk();
            case LOAD -> load();
            case COLLECT -> collect();
            case CRAFT -> craft();
            case GATHER -> gather();
            case STORE -> store();
            case WAIT -> waitForStations();
            case LOAD_POT -> loadPot();
            case COLLECT_POT -> collectPot();
        };
    }

    private ServerLevel level() {
        return ctx.level();
    }

    private boolean isRaw(ItemStack s) {
        return CookingSupport.isRawCookable(level(), s);
    }

    private boolean isFuel(ItemStack s) {
        return CookingSupport.isFuel(level(), s) && !s.is(Items.LAVA_BUCKET) && !s.isEdible();
    }

    /** What she takes out of a furnace: food and nothing else. */
    static boolean isCookedFood(ItemStack s) {
        return !s.isEmpty() && s.isEdible();
    }

    /**
     * The containers (bowls, bottles) taken from her bags for a pot recipe, as one stack. Her bags
     * give one stack per slot they took from: stacks that cannot be merged into the first one go
     * back through {@code giveBack} instead of being forgotten.
     */
    public static ItemStack mergeContainers(List<ItemStack> taken, java.util.function.Consumer<ItemStack> giveBack) {
        ItemStack merged = ItemStack.EMPTY;
        for (ItemStack stack : taken) {
            if (stack.isEmpty()) {
                continue;
            }
            if (merged.isEmpty()) {
                merged = stack;
                continue;
            }
            if (ItemStack.isSameItemSameTags(merged, stack)) {
                int move = Math.min(stack.getCount(), merged.getMaxStackSize() - merged.getCount());
                merged.grow(move);
                stack.shrink(move);
            }
            if (!stack.isEmpty()) {
                giveBack.accept(stack);
            }
        }
        return merged;
    }

    private boolean isDish(ItemStack s) {
        if (!s.isEdible() && !s.is(Items.CAKE)) {
            return false;
        }
        return !isRaw(s) && !(isPotIngredient(s) && !isPotResult(s));
    }

    // ------------------------------------------------------------------ cooking pots (Farmer's Delight)

    private List<CookingPots.Recipe> potRecipes() {
        if (pots == CookingPots.NONE) {
            return List.of();
        }
        if (potRecipesAge++ > 400) {
            potRecipes = pots.recipes(level());
            potRecipesAge = 0;
        }
        return potRecipes;
    }

    private boolean isPotIngredient(ItemStack s) {
        for (CookingPots.Recipe r : potRecipes()) {
            if (!r.container().isEmpty() && ItemStack.isSameItem(r.container(), s)) {
                return true;
            }
            for (net.minecraft.world.item.crafting.Ingredient i : r.ingredients()) {
                if (i.test(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPotResult(ItemStack s) {
        for (CookingPots.Recipe r : potRecipes()) {
            if (ItemStack.isSameItem(r.result(), s)) {
                return true;
            }
        }
        return false;
    }

    /** A pot recipe she has everything for (ingredients and containers), in her bags. */
    private CookingPots.Recipe craftablePotRecipe() {
        for (CookingPots.Recipe r : potRecipes()) {
            if (canMake(r)) {
                return r;
            }
        }
        return null;
    }

    private boolean canMake(CookingPots.Recipe r) {
        Map<Item, Integer> used = new java.util.HashMap<>();
        for (net.minecraft.world.item.crafting.Ingredient ingredient : r.ingredients()) {
            ItemStack found = ctx.peek(s -> ingredient.test(s) && ctx.count(x -> ItemStack.isSameItem(x, s)) > used.getOrDefault(s.getItem(), 0));
            if (found.isEmpty()) {
                return false;
            }
            used.merge(found.getItem(), 1, Integer::sum);
        }
        if (!r.container().isEmpty()) {
            int need = Math.max(1, r.result().getCount()) + used.getOrDefault(r.container().getItem(), 0);
            return ctx.count(s -> ItemStack.isSameItem(s, r.container())) >= need;
        }
        return true;
    }

    private BlockPos findPot(boolean withMeals) {
        if (pots == CookingPots.NONE) {
            return null;
        }
        ServerLevel level = level();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = area.radius();
        ChunkPos min = new ChunkPos(area.center().offset(-r, 0, -r));
        ChunkPos max = new ChunkPos(area.center().offset(r, 0, r));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockPos pos : level.getChunk(cx, cz).getBlockEntities().keySet()) {
                    if (!area.contains(pos) || rejected.contains(pos) || !pots.isPot(level, pos)) {
                        continue;
                    }
                    boolean fits = withMeals ? pots.hasMeals(level, pos) : pots.isReadyForIngredients(level, pos);
                    double d = pos.distSqr(ctx.laura().blockPosition());
                    if (fits && d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private Status loadPot() {
        CookingPots.Recipe recipe = potRecipe;
        potRecipe = null;
        if (recipe == null || !pots.isReadyForIngredients(level(), target) || !canMake(recipe)) {
            phase = Phase.PLAN;
            return Status.WORKING;
        }
        List<ItemStack> ingredients = new ArrayList<>();
        for (net.minecraft.world.item.crafting.Ingredient ingredient : recipe.ingredients()) {
            List<ItemStack> one = ctx.take(ingredient, 1);
            if (one.isEmpty()) {
                ingredients.forEach(ctx::store);
                phase = Phase.PLAN;
                return Status.WORKING;
            }
            ingredients.add(one.get(0));
        }
        ItemStack containers = ItemStack.EMPTY;
        if (!recipe.container().isEmpty()) {
            List<ItemStack> taken = ctx.take(s -> ItemStack.isSameItem(s, recipe.container()), Math.max(1, recipe.result().getCount()));
            containers = mergeContainers(taken, ctx::store);
        }
        if (!pots.load(level(), target, ingredients, containers)) {
            ingredients.forEach(ctx::store);
            ctx.store(containers);
            rejected.add(target);
        } else {
            // The pot takes what fits in its container slot and leaves the rest in the stack.
            ctx.store(containers);
            loadedStations.add(target);
            waitTicks = 0;
            ctx.laura().swing(InteractionHand.MAIN_HAND);
            level().playSound(null, target, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status collectPot() {
        ItemStack meals = pots.collect(level(), target);
        if (!meals.isEmpty()) {
            record(meals.copy());
            ctx.store(meals);
            ctx.laura().setCarried(meals);
            ctx.laura().swing(InteractionHand.MAIN_HAND);
            level().playSound(null, target, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
        if (!pots.isCooking(level(), target) && !pots.hasMeals(level(), target)) {
            loadedStations.remove(target);
        }
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status plan() {
        if (idleCooldown > 0) {
            idleCooldown--;
            return Status.IDLE;
        }
        // 1. Finished food waiting in a station or a cooking pot.
        BlockPos done = findStation(true);
        if (done != null) {
            go(done, Phase.COLLECT);
            return Status.WORKING;
        }
        BlockPos potDone = findPot(true);
        if (potDone != null) {
            go(potDone, Phase.COLLECT_POT);
            return Status.WORKING;
        }
        // 1b. A cooking pot recipe she has everything for.
        CookingPots.Recipe recipe = craftablePotRecipe();
        if (recipe != null) {
            BlockPos pot = findPot(false);
            if (pot != null) {
                potRecipe = recipe;
                go(pot, Phase.LOAD_POT);
                return Status.WORKING;
            }
        }
        // 2. Raw food to put in a station.
        if (ctx.has(this::isRaw)) {
            BlockPos station = findStation(false);
            if (station != null) {
                go(station, Phase.LOAD);
                return Status.WORKING;
            }
            failKey = "work.cook.no_station";
        }
        // 3. A meal she can prepare right now.
        meal = craftableMeal();
        if (meal != null) {
            BlockPos table = Cookbook.requireCraftingTable() ? findCraftingTable() : null;
            if (!Cookbook.requireCraftingTable() || table != null) {
                go(table != null ? table : ctx.laura().blockPosition(), Phase.CRAFT);
                progress = 0;
                return Status.WORKING;
            }
            failKey = "work.cook.no_table";
        }
        // 4. Ingredients waiting in chests.
        if (gatherCooldown <= 0) {
            gatherCooldown = oneShot ? 400 : 200;
            BlockPos source = ctx.laura().workplace().findSource(ChestPurpose.INGREDIENTS, s -> isRaw(s) || Cookbook.isIngredient(s) || isPotIngredient(s), area);
            if (source != null && !ctx.inventoryAlmostFull()) {
                go(source, Phase.GATHER);
                return Status.WORKING;
            }
            if (!ctx.has(this::isFuel)) {
                BlockPos fuel = ctx.laura().workplace().findSource(ChestPurpose.FUEL, this::isFuel, area);
                if (fuel != null) {
                    go(fuel, Phase.GATHER);
                    return Status.WORKING;
                }
            }
        }
        // 5. Stations still cooking what she loaded.
        if (!loadedStations.isEmpty() && waitTicks < 20 * 90) {
            phase = Phase.WAIT;
            return Status.WORKING;
        }
        // 6. Dishes to store.
        if (!oneShot && ctx.count(this::isDish) > KEEP_FOR_HERSELF) {
            BlockPos chest = ctx.laura().workplace().findDepositTarget(ChestPurpose.MEALS, ctx.peek(this::isDish), area);
            if (chest != null) {
                go(chest, Phase.STORE);
                return Status.WORKING;
            }
        }
        if (oneShot) {
            if (products.isEmpty()) {
                return Status.FAILED;
            }
            ctx.laura().playEmote(Emote.CELEBRATE);
            return Status.DONE;
        }
        idleCooldown = 120;
        return Status.IDLE;
    }

    private void go(BlockPos pos, Phase then) {
        target = pos;
        next = then;
        ctx.resetWalk();
        phase = Phase.WALK;
    }

    private Status walk() {
        if (ctx.walkTo(target, 2.3)) {
            ctx.laura().getNavigation().stop();
            phase = next;
        } else if (ctx.stuck()) {
            rejected.add(target);
            phase = Phase.PLAN;
        }
        return Status.WORKING;
    }

    /** Stations in the area. With {@code finished}, only those holding cooked output. */
    private BlockPos findStation(boolean finished) {
        ServerLevel level = level();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;
        int r = area.radius();
        ChunkPos min = new ChunkPos(area.center().offset(-r, 0, -r));
        ChunkPos max = new ChunkPos(area.center().offset(r, 0, r));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = e.getKey();
                    if (!area.contains(pos) || rejected.contains(pos)) {
                        continue;
                    }
                    BlockEntity be = e.getValue();
                    int score;
                    if (be instanceof AbstractFurnaceBlockEntity furnace && !(be instanceof BlastFurnaceBlockEntity)) {
                        if (finished) {
                            // Only food: ingots, glass or charcoal smelted there by someone else stay where they are.
                            if (!isCookedFood(furnace.getItem(2))) {
                                continue;
                            }
                            score = 10;
                        } else {
                            ItemStack input = furnace.getItem(0);
                            if (!input.isEmpty() && (!ctx.has(s -> ItemStack.isSameItemSameTags(s, input)) || input.getCount() >= input.getMaxStackSize())) {
                                continue;
                            }
                            if (furnace.getItem(1).isEmpty() && !ctx.has(this::isFuel) && !furnace.getBlockState().getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)) {
                                continue;
                            }
                            score = be instanceof net.minecraft.world.level.block.entity.SmokerBlockEntity ? 20 : 10;
                        }
                    } else if (be instanceof CampfireBlockEntity campfire && Cookbook.useCampfires()) {
                        if (finished || !campfire.getBlockState().getValue(CampfireBlock.LIT)) {
                            continue;
                        }
                        boolean free = false;
                        for (ItemStack s : campfire.getItems()) {
                            if (s.isEmpty()) {
                                free = true;
                                break;
                            }
                        }
                        if (!free) {
                            continue;
                        }
                        score = 5;
                    } else {
                        continue;
                    }
                    double d = pos.distSqr(ctx.laura().blockPosition());
                    if (score > bestScore || score == bestScore && d < bestDist) {
                        bestScore = score;
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private Status load() {
        ServerLevel level = level();
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack input = furnace.getItem(0);
            List<ItemStack> raw = ctx.take(s -> isRaw(s) && (input.isEmpty() || ItemStack.isSameItemSameTags(s, input)),
                    input.isEmpty() ? 64 : input.getMaxStackSize() - input.getCount());
            for (ItemStack stack : raw) {
                ItemStack current = furnace.getItem(0);
                if (current.isEmpty()) {
                    furnace.setItem(0, stack);
                } else if (ItemStack.isSameItemSameTags(current, stack)) {
                    current.grow(stack.getCount());
                } else {
                    ctx.store(stack);
                }
            }
            if (furnace.getItem(1).isEmpty()) {
                List<ItemStack> fuel = ctx.take(this::isFuel, 16);
                for (ItemStack f : fuel) {
                    if (furnace.getItem(1).isEmpty()) {
                        furnace.setItem(1, f);
                    } else if (ItemStack.isSameItemSameTags(furnace.getItem(1), f)) {
                        furnace.getItem(1).grow(f.getCount());
                    } else {
                        ctx.store(f);
                    }
                }
            }
            furnace.setChanged();
            loadedStations.add(target);
            level.playSound(null, target, SoundEvents.VILLAGER_WORK_BUTCHER, SoundSource.BLOCKS, 0.8F, 1.1F);
        } else if (be instanceof CampfireBlockEntity campfire) {
            for (int i = 0; i < 4; i++) {
                List<ItemStack> one = ctx.take(this::isRaw, 1);
                if (one.isEmpty()) {
                    break;
                }
                if (!CookingSupport.placeOnCampfire(level, campfire, ctx.laura(), one.get(0))) {
                    ctx.store(one.get(0));
                    break;
                }
            }
            loadedStations.add(target);
        }
        ctx.laura().swing(InteractionHand.MAIN_HAND);
        waitTicks = 0;
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status collect() {
        BlockEntity be = level().getBlockEntity(target);
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack out = isCookedFood(furnace.getItem(2)) ? furnace.removeItem(2, 64) : ItemStack.EMPTY;
            if (!out.isEmpty()) {
                record(out.copy());
                ctx.store(out);
                ctx.laura().setCarried(out);
                ctx.laura().swing(InteractionHand.MAIN_HAND);
                level().playSound(null, target, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            if (furnace.getItem(0).isEmpty()) {
                loadedStations.remove(target);
            }
        }
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status waitForStations() {
        waitTicks++;
        if (waitTicks % 40 == 0) {
            // Campfires drop their food around them.
            for (BlockPos pos : new ArrayList<>(loadedStations)) {
                if (level().getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
                    ctx.collectItemsAround(pos, 2.5, this::isDish);
                    boolean empty = true;
                    for (ItemStack s : campfire.getItems()) {
                        if (!s.isEmpty()) {
                            empty = false;
                            break;
                        }
                    }
                    if (empty) {
                        loadedStations.remove(pos);
                    }
                } else if (pots.isPot(level(), pos)) {
                    if (!pots.isCooking(level(), pos) && !pots.hasMeals(level(), pos)) {
                        loadedStations.remove(pos);
                    }
                } else if (!(level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) || furnace.getItem(0).isEmpty() && furnace.getItem(2).isEmpty()) {
                    loadedStations.remove(pos);
                }
            }
            phase = Phase.PLAN;
        }
        if (waitTicks > 20 * 90) {
            loadedStations.clear();
            phase = Phase.PLAN;
        }
        return Status.WORKING;
    }

    private Cookbook.Meal craftableMeal() {
        for (Cookbook.Meal m : Cookbook.meals()) {
            boolean ok = true;
            for (Map.Entry<ItemSpec, Integer> e : m.ingredients().entrySet()) {
                if (ctx.count(e.getKey()) < e.getValue()) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return m;
            }
        }
        return null;
    }

    private BlockPos findCraftingTable() {
        ServerLevel level = level();
        int r = area.radius();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(area.center().offset(-r, -4, -r), area.center().offset(r, 4, r))) {
            if (area.contains(pos) && level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
                double d = pos.distSqr(ctx.laura().blockPosition());
                if (d < bestDist) {
                    bestDist = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private Status craft() {
        if (meal == null) {
            phase = Phase.PLAN;
            return Status.WORKING;
        }
        progress++;
        ctx.laura().getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.8, target.getZ() + 0.5);
        if (progress % 10 == 0) {
            ctx.laura().swing(progress % 20 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            level().playSound(null, target, progress % 20 == 0 ? SoundEvents.VILLAGER_WORK_FARMER : SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5F, 1.3F);
        }
        if (progress < ctx.scaled(50)) {
            return Status.WORKING;
        }
        for (Map.Entry<ItemSpec, Integer> e : meal.ingredients().entrySet()) {
            if (ctx.count(e.getKey()) < e.getValue()) {
                phase = Phase.PLAN;
                return Status.WORKING;
            }
        }
        for (Map.Entry<ItemSpec, Integer> e : meal.ingredients().entrySet()) {
            ctx.take(e.getKey(), e.getValue());
        }
        for (Map.Entry<Item, Integer> r : meal.returns().entrySet()) {
            ctx.store(new ItemStack(r.getKey(), r.getValue()));
        }
        ItemStack result = meal.resultStack();
        record(result.copy());
        ctx.store(result);
        ctx.laura().setCarried(result);
        ctx.laura().spawnItemParticles(result, 6);
        ctx.laura().brain().needs().add(Needs.Need.FUN, 2);
        level().playSound(null, ctx.laura().blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.3F, 1.8F);
        meal = null;
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status gather() {
        int taken = ctx.withdrawFrom(target, s -> isRaw(s) || Cookbook.isIngredient(s) || isPotIngredient(s), 64);
        if (!ctx.has(this::isFuel)) {
            taken += ctx.withdrawFrom(target, this::isFuel, 16);
        }
        if (taken == 0) {
            rejected.add(target);
        }
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private Status store() {
        int keep = KEEP_FOR_HERSELF;
        int dishes = ctx.count(this::isDish);
        int toMove = Math.max(0, dishes - keep);
        if (toMove > 0) {
            List<ItemStack> moving = ctx.take(this::isDish, toMove);
            for (ItemStack s : moving) {
                ctx.store(ctx.insertAt(target, s));
            }
            ctx.laura().swing(InteractionHand.MAIN_HAND);
        }
        ctx.laura().setCarried(ItemStack.EMPTY);
        phase = Phase.PLAN;
        return Status.WORKING;
    }

    private void record(ItemStack stack) {
        ctx.credit("meals", stack);
        if (!oneShot || stack.isEmpty()) {
            return;
        }
        for (ItemStack p : products) {
            if (ItemStack.isSameItemSameTags(p, stack)) {
                p.grow(stack.getCount());
                return;
            }
        }
        products.add(stack);
    }
}
