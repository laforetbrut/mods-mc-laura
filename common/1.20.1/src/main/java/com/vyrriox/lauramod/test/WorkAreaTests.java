package com.vyrriox.lauramod.test;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.ai.LauraWorkGoal;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.entity.work.CookWork;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.entity.work.LauraWorkplace;
import com.vyrriox.lauramod.entity.work.LumberjackWork;
import com.vyrriox.lauramod.entity.work.WorkArea;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.platform.CookingPots;
import com.vyrriox.lauramod.platform.LauraInventories;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.world.LauraActions;
import com.vyrriox.lauramod.world.LauraManager;
import com.vyrriox.lauramod.world.LauraWorldData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Self tests of her work: jobs, errands, the task queue, fetching and the containers she may use.
 *
 * @author vyrriox
 */
public final class WorkAreaTests {
    private WorkAreaTests() {
    }

    private record Case(String name, int timeoutTicks, TestBody body) implements TestRunner.TestCase {
        @Override
        public void run(TestRunner.Context ctx) throws Exception {
            body.run(ctx);
        }
    }

    @FunctionalInterface
    private interface TestBody {
        void run(TestRunner.Context ctx) throws Exception;
    }

    private static void add(List<TestRunner.TestCase> tests, String name, int timeoutTicks, TestBody body) {
        tests.add(new Case(name, timeoutTicks, body));
    }

    public static void addTo(List<TestRunner.TestCase> tests) {
        // ------------------------------------------------------------------ containers she may use
        // A crafter is a machine, not a storage. Minecraft 1.20.1 has no crafter of its own: the
        // rule is checked on the names a crafter of another mod has (the id of its block entity
        // type first, its class name can be anything), and a dropper stands for the machine in
        // the world.
        add(tests, "work_crafter_not_storage", 100, ctx -> {
            BlockPos machine = ctx.origin.offset(3, 0, 3);
            BlockPos chest = ctx.origin.offset(5, 0, 3);
            place(ctx, machine, Blocks.DROPPER.defaultBlockState());
            place(ctx, chest, Blocks.CHEST.defaultBlockState());
            BlockEntity be = ctx.level.getBlockEntity(machine);
            ctx.check(be != null, "no block entity for the dropper");
            ctx.check(LauraInventories.isCrafter(new ResourceLocation("somemod", "mechanical_crafter"), "a.b"), "a crafter type id is not recognized");
            ctx.check(LauraInventories.isCrafter(null, "somemod.block.AutoCrafterBlockEntity"), "a crafter class name is not recognized");
            ctx.check(!LauraInventories.isCrafter(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()), be.getClass().getName()), "a dropper counts as a crafter");
            ctx.check(!LauraInventories.isStorage(be), "a machine counts as a storage");
            List<BlockPos> storages = LauraInventories.storagesAround(ctx.level, ctx.origin, 10, p -> true);
            ctx.check(!storages.contains(machine), "the machine is in the storages she searches");
            ctx.check(storages.contains(chest), "the chest is not in the storages she searches");
            ctx.succeed();
        });
        // A chest with a vanilla lock is left alone, assigned or not, until she or her partner holds its key.
        add(tests, "work_locked_chest", 1200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(8, 0, 0);
            place(ctx, chest, Blocks.CHEST.defaultBlockState());
            // An item no other test leaves around: the test world is kept between runs.
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.NETHER_STAR, 10));
            }
            lock(ctx, chest, "secret");
            ctx.check(LauraInventories.isLocked(laura, chest), "the lock is not seen");
            ctx.check(!LauraInventories.mayUse(laura, chest), "she may use a locked chest");
            laura.workplace().assign(chest, ctx.level.dimension(), ChestPurpose.PANTRY);
            ctx.check(!LauraInventories.mayUse(laura, chest), "she may use a locked chest once it is assigned");
            ctx.check(laura.workplace().assigned(ChestPurpose.PANTRY).isEmpty(), "a locked chest is listed as her pantry");
            laura.workplace().unassign(chest, ctx.level.dimension());
            // Assigning a chest the player cannot open is refused.
            BlockPos near = ctx.origin.offset(0, 0, 3);
            place(ctx, near, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
            lock(ctx, near, "secret");
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(near));
            LauraActions.perform(player, laura, LauraAction.CHEST, "WOOD", LauraActions.Source.MENU);
            ctx.check(laura.workplace().purposeOf(near) == null, "a locked chest was assigned");
            LauraActions.fetch(player, laura, "minecraft:nether_star", 3, false);
            ctx.waitFor("the fetch to give up", 200, () -> !laura.fetchGoal().isActive(), () -> {
                ctx.check(player.getInventory().countItem(Items.NETHER_STAR) == 0, "she took items from a locked chest");
                ctx.check(ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be && be.getItem(0).getCount() == 10, "the locked chest lost items");
                // With the key in her partner's hand the chest opens, as it would for that player.
                ItemStack key = new ItemStack(Items.TRIPWIRE_HOOK);
                key.setHoverName(Component.literal("secret"));
                player.setItemInHand(InteractionHand.MAIN_HAND, key);
                ctx.check(!LauraInventories.isLocked(laura, chest), "the key does not open the lock");
                LauraActions.perform(player, laura, LauraAction.CHEST, "WOOD", LauraActions.Source.MENU);
                ctx.check(laura.workplace().purposeOf(near) == ChestPurpose.WOOD, "the chest was not assigned with the key in hand");
                LauraActions.fetch(player, laura, "minecraft:nether_star", 3, false);
                ctx.waitFor("3 items from the unlocked chest", 800, () -> player.getInventory().countItem(Items.NETHER_STAR) >= 3, ctx::succeed);
            });
        }));
        // A container nobody assigned to her is only used when her partner could open it: here the
        // world border stands for any rule that keeps that player out.
        add(tests, "work_container_permission", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(4, 0, 0);
            place(ctx, chest, Blocks.CHEST.defaultBlockState());
            ctx.check(LauraInventories.playerMayUse(player, ctx.level, chest), "the player may not use a free chest");
            ctx.check(LauraInventories.mayUse(laura, chest), "she may not use a free chest next to her partner");
            WorldBorder border = ctx.level.getWorldBorder();
            double size = border.getSize();
            border.setSize(1);
            boolean playerAllowed;
            boolean lauraAllowed;
            boolean assignedAllowed;
            try {
                playerAllowed = LauraInventories.playerMayUse(player, ctx.level, chest);
                lauraAllowed = LauraInventories.mayUse(laura, chest);
                laura.workplace().assign(chest, ctx.level.dimension(), ChestPurpose.STORAGE);
                assignedAllowed = LauraInventories.mayUse(laura, chest);
            } finally {
                border.setSize(size);
            }
            ctx.check(!playerAllowed, "the player may use a chest outside the world border");
            ctx.check(!lauraAllowed, "she may use a chest her partner cannot open");
            ctx.check(assignedAllowed, "she may not use a chest her partner assigned to her");
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ fetch
        // What she carries during a fetch is saved with her, and dropped with her belongings.
        add(tests, "work_fetch_bag_saved", 900, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(10, 0, 0);
            place(ctx, chest, Blocks.CHEST.defaultBlockState());
            // An item no other test leaves around: the test world is kept between runs.
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.PRISMARINE_SHARD, 10));
            }
            LauraActions.fetch(player, laura, "minecraft:prismarine_shard", 3, false);
            ctx.waitFor("her to take the items", 800, () -> !laura.fetchGoal().carried().isEmpty(), () -> {
                CompoundTag tag = laura.saveWithoutId(new CompoundTag());
                ListTag saved = tag.getList("FetchBag", Tag.TAG_COMPOUND);
                ctx.check(saved.size() == 1, "the fetch bag is not saved: " + saved.size() + " stack(s)");
                LauraEntity copy = LauraRegistries.LAURA.get().create(ctx.level);
                ctx.check(copy != null, "could not create a copy");
                copy.load(tag);
                copy.setUUID(UUID.randomUUID());
                ctx.check(copy.bags().count(s -> s.is(Items.PRISMARINE_SHARD)) == 0, "the items were put in her bag before she is in the world");
                ctx.check(ctx.level.addFreshEntity(copy), "could not add the copy to the world");
                // Release, death without revival: the carried items fall with the rest.
                laura.dropBelongings();
                ctx.check(!laura.fetchGoal().isActive(), "the fetch goes on after she dropped everything");
                int dropped = 0;
                for (ItemEntity item : ctx.level.getEntitiesOfClass(ItemEntity.class, laura.getBoundingBox().inflate(3), e -> e.getItem().is(Items.PRISMARINE_SHARD))) {
                    dropped += item.getItem().getCount();
                }
                ctx.check(dropped == 3, "dropped " + dropped + " item(s) instead of 3");
                ctx.after(3, () -> {
                    int kept = copy.bags().count(s -> s.is(Items.PRISMARINE_SHARD));
                    copy.discard();
                    ctx.check(kept == 3, "after a save and load she has " + kept + " item(s) instead of 3");
                    ctx.succeed();
                });
            });
        }));

        // ------------------------------------------------------------------ lumberjack
        // Logs of a build that touches a natural tree are not part of the tree.
        add(tests, "work_lumberjack_spares_builds", 100, ctx -> {
            BlockPos base = ctx.origin.offset(6, 0, 6);
            plantOak(ctx, base);
            // A wall of the same logs against the trunk, and a wall of another wood on the other side.
            List<BlockPos> wall = new ArrayList<>();
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 2; y++) {
                    wall.add(base.offset(1, y, z));
                    ctx.level.setBlockAndUpdate(base.offset(1, y, z), Blocks.OAK_LOG.defaultBlockState());
                    wall.add(base.offset(-1, y, z));
                    ctx.level.setBlockAndUpdate(base.offset(-1, y, z), Blocks.SPRUCE_LOG.defaultBlockState());
                }
            }
            Set<BlockPos> tree = LumberjackWork.treeLogs(ctx.level, base);
            ctx.check(tree.size() == 5, "the tree has " + tree.size() + " logs instead of 5");
            for (int y = 0; y < 5; y++) {
                ctx.check(tree.contains(base.above(y)), "trunk log " + y + " is missing");
            }
            for (BlockPos log : wall) {
                ctx.check(!tree.contains(log), "a log of the wall at " + log.subtract(base) + " counts as the tree");
            }
            // The foot of the wall is not a tree either, although it touches one.
            ctx.check(LumberjackWork.treeLogs(ctx.level, base.offset(1, 0, 1)).isEmpty(), "the wall alone counts as a tree");
            // A beam on stone posts in the leaves: not on soil, never a tree.
            BlockPos beam = ctx.origin.offset(-6, 3, 6);
            ctx.level.setBlockAndUpdate(beam.below(), Blocks.STONE.defaultBlockState());
            for (int x = 0; x < 3; x++) {
                ctx.level.setBlockAndUpdate(beam.offset(x, 0, 0), Blocks.OAK_LOG.defaultBlockState());
                for (Direction dir : Direction.values()) {
                    BlockPos p = beam.offset(x, 0, 0).relative(dir);
                    if (ctx.level.getBlockState(p).isAir()) {
                        ctx.level.setBlockAndUpdate(p, naturalLeaves());
                    }
                }
            }
            ctx.check(LumberjackWork.treeLogs(ctx.level, beam).isEmpty(), "a beam in the leaves counts as a tree");
            // A trunk on stone is a pillar.
            BlockPos pillar = ctx.origin.offset(-6, 0, -6);
            plantOak(ctx, pillar);
            ctx.level.setBlockAndUpdate(pillar.below(), Blocks.SMOOTH_STONE.defaultBlockState());
            ctx.check(LumberjackWork.treeLogs(ctx.level, pillar).isEmpty(), "a trunk on stone counts as a tree");
            // A 2 x 2 trunk is felled whole.
            BlockPos big = ctx.origin.offset(6, 0, -8);
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    ctx.level.setBlockAndUpdate(big.offset(x, -1, z), Blocks.DIRT.defaultBlockState());
                    for (int y = 0; y < 4; y++) {
                        ctx.level.setBlockAndUpdate(big.offset(x, y, z), Blocks.DARK_OAK_LOG.defaultBlockState());
                    }
                    ctx.level.setBlockAndUpdate(big.offset(x, 4, z), naturalLeaves());
                }
            }
            ctx.check(LumberjackWork.treeLogs(ctx.level, big).size() == 16, "a 2 x 2 trunk gives " + LumberjackWork.treeLogs(ctx.level, big).size() + " logs instead of 16");
            // So is a trunk that bends, like an acacia.
            BlockPos bent = ctx.origin.offset(12, 0, 0);
            ctx.level.setBlockAndUpdate(bent.below(), Blocks.DIRT.defaultBlockState());
            List<BlockPos> bend = List.of(bent, bent.above(), bent.above(2), bent.offset(1, 3, 0), bent.offset(2, 4, 0));
            for (BlockPos log : bend) {
                ctx.level.setBlockAndUpdate(log, Blocks.ACACIA_LOG.defaultBlockState());
            }
            for (Direction dir : Direction.values()) {
                BlockPos p = bent.offset(2, 4, 0).relative(dir);
                if (ctx.level.getBlockState(p).isAir()) {
                    ctx.level.setBlockAndUpdate(p, naturalLeaves());
                }
            }
            ctx.check(LumberjackWork.treeLogs(ctx.level, bent).containsAll(bend), "a bent trunk is not felled whole: " + LumberjackWork.treeLogs(ctx.level, bent).size() + " of 5 logs");
            // The option off: everything connected is felled, as before.
            boolean natural = LauraConfig.onlyNaturalTrees.get();
            LauraConfig.onlyNaturalTrees.set(false);
            ctx.onCleanup(() -> LauraConfig.onlyNaturalTrees.set(natural));
            ctx.check(LumberjackWork.treeLogs(ctx.level, base).size() == 5 + wall.size(), "onlyNaturalTrees off no longer takes every connected log");
            ctx.succeed();
        });

        // ------------------------------------------------------------------ cook
        // Only food is taken out of a furnace: what somebody else smelts there stays.
        add(tests, "work_cook_leaves_ingots", 1200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos ingots = ctx.origin.offset(4, 0, -4);
            BlockPos steaks = ctx.origin.offset(6, 0, -4);
            place(ctx, ingots, Blocks.FURNACE.defaultBlockState());
            place(ctx, steaks, Blocks.FURNACE.defaultBlockState());
            if (ctx.level.getBlockEntity(ingots) instanceof AbstractFurnaceBlockEntity furnace) {
                furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 3));
            }
            if (ctx.level.getBlockEntity(steaks) instanceof AbstractFurnaceBlockEntity furnace) {
                furnace.setItem(2, new ItemStack(Items.COOKED_BEEF, 2));
            }
            LauraActions.task(player, laura, LauraTask.Type.COOK, 12, false);
            ctx.waitFor("the cooked beef", 1100, () -> player.getInventory().countItem(Items.COOKED_BEEF) >= 2, () -> {
                ctx.check(ctx.level.getBlockEntity(ingots) instanceof AbstractFurnaceBlockEntity furnace && furnace.getItem(2).getCount() == 3, "she emptied a furnace that held ingots");
                ctx.check(laura.bags().count(s -> s.is(Items.IRON_INGOT)) == 0 && player.getInventory().countItem(Items.IRON_INGOT) == 0, "she took the ingots");
                ctx.succeed();
            });
        }));
        // Containers taken from two slots of her bag are one stack for the pot, none is forgotten.
        add(tests, "work_pot_containers_merged", 200, ctx -> withLaura(ctx, laura -> {
            laura.inventory().setItem(0, new ItemStack(Items.BOWL, 1));
            laura.inventory().setItem(5, new ItemStack(Items.BOWL, 3));
            ItemStack named = new ItemStack(Items.BOWL, 2);
            named.setHoverName(Component.literal("Soup bowl"));
            List<ItemStack> taken = laura.bags().take(s -> s.is(Items.BOWL), 4);
            ctx.check(taken.size() == 2, "her bag gave " + taken.size() + " stack(s) instead of 2");
            taken.add(named);
            List<ItemStack> back = new ArrayList<>();
            ItemStack merged = CookWork.mergeContainers(taken, back::add);
            ctx.check(merged.getCount() == 4, "merged " + merged.getCount() + " bowl(s) instead of 4");
            ctx.check(back.size() == 1 && back.get(0).getCount() == 2, "the bowls that cannot be merged were not given back");
            ctx.succeed();
        }));
        if (LauraMod.platform().isModLoaded("farmersdelight")) {
            // The container slot of a Farmer's Delight pot: another container in it refuses the load
            // and nothing goes in; the same container takes what fits and leaves her the rest.
            add(tests, "compat_farmers_delight_pot_containers", 300, ctx -> {
                CookingPots pots = LauraMod.platform().cookingPots();
                BlockPos fire = ctx.origin.offset(4, 0, -4);
                BlockPos pot = fire.above();
                ctx.level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
                ctx.level.setBlockAndUpdate(pot, BuiltInRegistries.BLOCK.get(new ResourceLocation("farmersdelight:cooking_pot")).defaultBlockState());
                ctx.check(pots.isPot(ctx.level, pot), "the pot is not recognized");
                ctx.after(5, () -> {
                    ctx.check(pots.isReadyForIngredients(ctx.level, pot), "the empty pot on a fire is not ready");
                    setPotContainers(ctx, pot, new ItemStack(Items.GLASS_BOTTLE, 1));
                    ItemStack bowls = new ItemStack(Items.BOWL, 2);
                    ctx.check(!pots.load(ctx.level, pot, List.of(new ItemStack(Items.CARROT)), bowls), "the pot took bowls although bottles sit in its container slot");
                    ctx.check(bowls.getCount() == 2, "bowls were lost: " + bowls.getCount() + " left");
                    ctx.check(pots.isReadyForIngredients(ctx.level, pot) && !pots.isCooking(ctx.level, pot), "ingredients were put in a pot that cannot serve them");
                    setPotContainers(ctx, pot, new ItemStack(Items.BOWL, 63));
                    bowls = new ItemStack(Items.BOWL, 4);
                    ctx.check(pots.load(ctx.level, pot, List.of(new ItemStack(Items.CARROT)), bowls), "the pot refused the same container");
                    ctx.check(bowls.getCount() == 3, "the bowls that do not fit were not left to her: " + bowls.getCount());
                    ctx.check(pots.isCooking(ctx.level, pot), "the ingredients are not in the pot");
                    ctx.succeed();
                });
            });
        }

        // ------------------------------------------------------------------ the mobGriefing rule
        // An errand or a job given before the rule was turned off no longer breaks blocks.
        add(tests, "work_no_griefing", 600, ctx -> withLaura(ctx, laura -> {
            BlockPos base = ctx.origin.offset(5, 0, 5);
            plantOak(ctx, base);
            ctx.check(!LumberjackWork.treeLogs(ctx.level, base).isEmpty(), "the test tree is not a tree");
            GameRules.BooleanValue rule = ctx.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
            boolean before = rule.get();
            ctx.onCleanup(() -> rule.set(before, ctx.server()));
            workAtAnyHour(ctx);
            WorkArea area = new WorkArea(ctx.origin, 12, ctx.level.dimension());
            int said = LauraSpeech.timesSaid("work.no_griefing");
            laura.workplace().replaceCurrent(new LauraTask(LauraTask.Type.CHOP_TREE, "", 0, area));
            rule.set(false, ctx.server());
            ctx.waitFor("the errand to be dropped", 100, () -> laura.workplace().current() == null, () -> {
                ctx.check(LauraSpeech.timesSaid("work.no_griefing") == said + 1, "she did not say why");
                laura.workplace().enableJob(LauraJob.LUMBERJACK, area);
                laura.workplace().enableJob(LauraJob.FARMER, area);
                laura.workGoal().reset();
                laura.setMode(LauraMode.WORK);
                ctx.after(200, () -> {
                    for (int y = 0; y < 5; y++) {
                        ctx.check(ctx.level.getBlockState(base.above(y)).is(Blocks.OAK_LOG), "she cut the tree although mobGriefing is off");
                    }
                    ctx.check(LauraSpeech.timesSaid("work.no_griefing") == said + 2, "her jobs said it " + (LauraSpeech.timesSaid("work.no_griefing") - said - 1) + " time(s)");
                    ctx.succeed();
                });
            });
        }));

        // ------------------------------------------------------------------ work that finds nothing
        nothingToDo(tests, "work_nothing_lumberjack", LauraTask.Type.CHOP_TREE, "work.lumberjack.no_tree");
        nothingToDo(tests, "work_nothing_farmer", LauraTask.Type.HARVEST, "work.farmer.nothing");
        nothingToDo(tests, "work_nothing_cook", LauraTask.Type.COOK, "work.cook.nothing");
        // A job with nothing to do says so once and pauses; the pause grows.
        add(tests, "work_job_idle_once", 600, ctx -> withLaura(ctx, laura -> {
            ctx.check(LauraWorkGoal.retryDelay(1, 4800) == LauraWorkGoal.RETRY_TICKS, "first wait");
            ctx.check(LauraWorkGoal.retryDelay(2, 4800) == 2 * LauraWorkGoal.RETRY_TICKS, "second wait");
            ctx.check(LauraWorkGoal.retryDelay(9, 4800) == 4800, "the wait has no ceiling");
            ctx.check(LauraWorkGoal.RETRY_TICKS >= 20 * 60, "the first wait is shorter than a minute");
            ServerPlayer player = ctx.player();
            workAtAnyHour(ctx);
            int idle = LauraSpeech.timesSaid("work.idle");
            int noTree = LauraSpeech.timesSaid("work.lumberjack.no_tree");
            LauraActions.enableJob(player, laura, LauraJob.LUMBERJACK, 8);
            LauraActions.enableJob(player, laura, LauraJob.FARMER, 8);
            ctx.after(400, () -> {
                ctx.check(LauraSpeech.timesSaid("work.idle") == idle + 1, "idle jobs said it " + (LauraSpeech.timesSaid("work.idle") - idle) + " time(s)");
                ctx.check(LauraSpeech.timesSaid("work.lumberjack.no_tree") == noTree, "the job announced every search");
                ctx.succeed();
            });
        }));

        // ------------------------------------------------------------------ task queue messages
        // One click, one line: a doubled menu request is one task and one answer.
        add(tests, "work_queue_one_line", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraWorkplace work = laura.workplace();
            // Something in progress, so that the next orders are queued.
            work.replaceCurrent(new LauraTask(LauraTask.Type.COOK, "", 0, new WorkArea(ctx.origin, 6, ctx.level.dimension())));
            int queued = LauraSpeech.timesSaid("task.queued");
            LauraActions.perform(player, laura, LauraAction.TASK, "CHOP_TREE:queue:8", LauraActions.Source.MENU);
            LauraActions.perform(player, laura, LauraAction.TASK, "CHOP_TREE:queue:8", LauraActions.Source.MENU);
            ctx.check(work.queued().size() == 1, "one click queued " + work.queued().size() + " tasks");
            ctx.check(LauraSpeech.timesSaid("task.queued") == queued + 1, "one click gave " + (LauraSpeech.timesSaid("task.queued") - queued) + " confirmations");
            // Another button is another order.
            LauraActions.perform(player, laura, LauraAction.TASK, "HARVEST:queue:8", LauraActions.Source.MENU);
            ctx.check(work.queued().size() == 2 && LauraSpeech.timesSaid("task.queued") == queued + 2, "a different order was taken for a repeated click");
            while (work.enqueue(new LauraTask(LauraTask.Type.STAY, "", 0, null))) {
                // Fill her list.
            }
            int full = LauraSpeech.timesSaid("task.queue_full");
            work.replaceCurrent(new LauraTask(LauraTask.Type.COOK, "", 0, new WorkArea(ctx.origin, 6, ctx.level.dimension())));
            LauraActions.perform(player, laura, LauraAction.TASK, "COOK:queue:8", LauraActions.Source.MENU);
            LauraActions.perform(player, laura, LauraAction.TASK, "COOK:queue:8", LauraActions.Source.MENU);
            ctx.check(LauraSpeech.timesSaid("task.queue_full") == full + 1, "one click on a full list gave " + (LauraSpeech.timesSaid("task.queue_full") - full) + " refusals");
            work.clearQueue();
            // The same button again a second later is a new click.
            ctx.after(20, () -> {
                work.replaceCurrent(new LauraTask(LauraTask.Type.COOK, "", 0, new WorkArea(ctx.origin, 6, ctx.level.dimension())));
                int again = LauraSpeech.timesSaid("task.queued");
                LauraActions.perform(player, laura, LauraAction.TASK, "CHOP_TREE:queue:8", LauraActions.Source.MENU);
                ctx.after(20, () -> {
                    work.replaceCurrent(new LauraTask(LauraTask.Type.COOK, "", 0, new WorkArea(ctx.origin, 6, ctx.level.dimension())));
                    LauraActions.perform(player, laura, LauraAction.TASK, "CHOP_TREE:queue:8", LauraActions.Source.MENU);
                    ctx.check(LauraSpeech.timesSaid("task.queued") == again + 2, "two separate clicks gave " + (LauraSpeech.timesSaid("task.queued") - again) + " confirmation(s)");
                    work.clearQueue();
                    ctx.succeed();
                });
            });
        }));
    }

    /**
     * An errand with nothing to do, and the same errand queued three times behind it: she says once
     * that she found nothing, the queued ones wait instead of starting and failing one after the
     * other, and an order of another kind still goes through.
     */
    private static void nothingToDo(List<TestRunner.TestCase> tests, String name, LauraTask.Type type, String failKey) {
        add(tests, name, 600, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraWorkplace work = laura.workplace();
            int failed = LauraSpeech.timesSaid(failKey);
            int started = LauraSpeech.timesSaid("task.start." + type.key());
            LauraActions.task(player, laura, type, 8, false);
            for (int i = 0; i < 3; i++) {
                LauraActions.task(player, laura, type, 8, true);
            }
            ctx.check(work.queued().size() == 3, "queued " + work.queued().size() + " instead of 3");
            ctx.waitFor("the errand to give up", 200, () -> work.current() == null && LauraSpeech.timesSaid(failKey) > failed, () -> {
                ctx.after(100, () -> {
                    ctx.check(LauraSpeech.timesSaid(failKey) == failed + 1, "she said " + (LauraSpeech.timesSaid(failKey) - failed) + " times that she found nothing");
                    ctx.check(LauraSpeech.timesSaid("task.start." + type.key()) == started + 1, "she announced " + (LauraSpeech.timesSaid("task.start." + type.key()) - started) + " starts");
                    ctx.check(work.queued().size() == 3 && work.current() == null, "the queued errands did not wait: " + work.queued().size() + " left");
                    LauraTask waiting = work.queued().get(0);
                    ctx.check(laura.workGoal().isWaiting(waiting) && laura.workGoal().isQuiet(waiting), "the queued errand is not on hold");
                    // An order of another kind is not stuck behind them.
                    work.enqueue(new LauraTask(LauraTask.Type.STAY, "", 0, null));
                    ctx.waitFor("the queued order behind the waiting errands", 40, () -> laura.getMode() == LauraMode.STAY, () -> {
                        ctx.check(work.queued().size() == 3, "the waiting errands were dropped");
                        // A direct order is tried at once.
                        LauraActions.task(player, laura, type, 8, false);
                        ctx.check(!laura.workGoal().isWaiting(waiting), "a new order did not clear the wait");
                        work.clearQueue();
                        ctx.succeed();
                    });
                });
            });
        }));
    }

    /** Her jobs stop at night by default: the tests of her jobs must not depend on the hour they run at. */
    private static void workAtAnyHour(TestRunner.Context ctx) {
        boolean atNight = LauraConfig.workAtNight.get();
        LauraConfig.workAtNight.set(true);
        ctx.onCleanup(() -> LauraConfig.workAtNight.set(atNight));
    }

    private static net.minecraft.world.level.block.state.BlockState naturalLeaves() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
    }

    /** A small natural oak: dirt, five logs, leaves around and above the top. */
    private static void plantOak(TestRunner.Context ctx, BlockPos base) {
        ctx.level.setBlockAndUpdate(base.below(), Blocks.DIRT.defaultBlockState());
        for (int y = 0; y < 5; y++) {
            ctx.level.setBlockAndUpdate(base.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 3; y <= 5; y++) {
                    BlockPos p = base.offset(dx, y, dz);
                    if (ctx.level.getBlockState(p).isAir()) {
                        ctx.level.setBlockAndUpdate(p, naturalLeaves());
                    }
                }
            }
        }
    }

    /** Puts a vanilla lock on a container, the way {@code /data merge block ... {Lock:"key"}} does. */
    private static void lock(TestRunner.Context ctx, BlockPos pos, String key) {
        BlockEntity be = ctx.level.getBlockEntity(pos);
        ctx.check(be != null, "no container at " + pos);
        CompoundTag tag = be.saveWithoutMetadata();
        tag.putString("Lock", key);
        be.load(tag);
    }

    /** The container slot of a Farmer's Delight cooking pot (CookingPotBlockEntity.CONTAINER_SLOT). */
    private static final int POT_CONTAINER_SLOT = 7;

    /** Sets the container slot of a cooking pot through its saved data, which every loader shares. */
    private static void setPotContainers(TestRunner.Context ctx, BlockPos pos, ItemStack stack) {
        BlockEntity be = ctx.level.getBlockEntity(pos);
        ctx.check(be != null, "no pot at " + pos);
        CompoundTag tag = be.saveWithoutMetadata();
        CompoundTag inventory = tag.getCompound("Inventory");
        ListTag items = inventory.getList("Items", Tag.TAG_COMPOUND);
        items.removeIf(t -> t instanceof CompoundTag item && item.getInt("Slot") == POT_CONTAINER_SLOT);
        CompoundTag item = stack.save(new CompoundTag());
        item.putInt("Slot", POT_CONTAINER_SLOT);
        items.add(item);
        inventory.put("Items", items);
        tag.put("Inventory", inventory);
        be.load(tag);
    }

    private static void resetCooldown(TestRunner.Context ctx) {
        LauraWorldData.get(ctx.server()).meta(ctx.player().getUUID()).lastSummon = 0;
    }

    /** Summons a companion for the test player, then runs the body once she is in the world. */
    private static void withLaura(TestRunner.Context ctx, Consumer<LauraEntity> body) {
        ServerPlayer player = ctx.player();
        resetCooldown(ctx);
        LauraManager.summon(player, false);
        ctx.waitFor("the summon", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
            LauraEntity laura = LauraManager.findAll(player).get(0);
            laura.brain().needs().fillAll();
            // The test world and its players are kept between runs: what an earlier run left in the
            // player's hands or on the ground must not count. Done here, once the chunks are loaded
            // and their entities can be seen.
            player.getInventory().clearContent();
            for (ItemEntity item : ctx.level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(ctx.origin).inflate(28, 16, 28))) {
                item.discard();
            }
            body.accept(laura);
        });
    }

    /**
     * Places a chest or a furnace that leaves nothing behind: when the test ends it is emptied and
     * removed, so that the next run does not find its content lying on the platform.
     */
    private static void place(TestRunner.Context ctx, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        ctx.level.setBlockAndUpdate(pos, state);
        ctx.onCleanup(() -> {
            net.minecraft.world.Clearable.tryClear(ctx.level.getBlockEntity(pos));
            ctx.level.removeBlock(pos, false);
        });
    }
}
