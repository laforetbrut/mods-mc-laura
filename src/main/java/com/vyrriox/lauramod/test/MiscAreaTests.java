package com.vyrriox.lauramod.test;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.cooking.Cookbook;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.desire.DesireTable;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.model.ModelParser;
import com.vyrriox.lauramod.model.Molang;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.MojangSkinResolver;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import com.vyrriox.lauramod.skin.SkinRef;
import com.vyrriox.lauramod.skin.SkinService;
import com.vyrriox.lauramod.util.CacheFiles;
import com.vyrriox.lauramod.util.LruCache;
import com.vyrriox.lauramod.world.LauraActions;
import com.vyrriox.lauramod.world.LauraAdvancements;
import com.vyrriox.lauramod.world.LauraManager;
import com.vyrriox.lauramod.world.LauraWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Self tests of gifts and food, her bag, sleep, the gag, skins and uploads, the config files and the
 * admin command.
 *
 * @author vyrriox
 */
public final class MiscAreaTests {
    private MiscAreaTests() {
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
        // ------------------------------------------------------------------ gifts and food
        // A gift given while her bag is full lands at her feet, and she says so in one line.
        add(tests, "gift_full_bag_dropped", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            for (int i = 0; i < laura.inventory().getContainerSize(); i++) {
                laura.inventory().setItem(i, new ItemStack(Items.STONE, 64));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 2));
            long spoken = LauraSpeech.spokenCount();
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(player.getMainHandItem().getCount() == 1, "the gift was not taken");
            ctx.check(count(ctx, laura.getBoundingBox().inflate(3), Items.STICK) == 1, "the gift that did not fit was destroyed");
            ctx.check(LauraSpeech.spokenCount() - spoken == 1, "she said " + (LauraSpeech.spokenCount() - spoken) + " lines for one gift");
            ctx.succeed();
        }));
        // Hugs in a row are all given, but only the first of each cooldown makes her fonder.
        add(tests, "cuddle_cooldown", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            int seconds = LauraConfig.cuddleCooldownSeconds.getInt();
            ctx.onCleanup(() -> LauraConfig.cuddleCooldownSeconds.set(seconds));
            LauraConfig.cuddleCooldownSeconds.set(60);
            laura.setAffection(300);
            int hugs = LauraAdvancements.get(player, "hugs");
            for (int i = 0; i < 5; i++) {
                LauraActions.perform(player, laura, LauraAction.HUG, "", LauraActions.Source.MENU);
            }
            ctx.check(LauraAdvancements.get(player, "hugs") == hugs + 5, "a hug in a row was refused");
            ctx.check(laura.getAffection() == 304, "five hugs in a row: affection is " + laura.getAffection());
            for (int i = 0; i < 3; i++) {
                LauraActions.perform(player, laura, LauraAction.KISS, "", LauraActions.Source.MENU);
            }
            ctx.check(laura.getAffection() == 309, "three kisses in a row: affection is " + laura.getAffection());
            LauraConfig.cuddleCooldownSeconds.set(0);
            LauraActions.perform(player, laura, LauraAction.HUG, "", LauraActions.Source.MENU);
            LauraActions.perform(player, laura, LauraAction.HUG, "", LauraActions.Source.MENU);
            ctx.check(laura.getAffection() == 317, "without a cooldown two hugs gave " + (laura.getAffection() - 309));
            ctx.succeed();
        }));
        // The same kind of gift only makes her fonder once per cooldown: taking it back from her bag
        // and giving it again is free affection otherwise. Another kind of gift still counts.
        add(tests, "gift_cooldown_per_kind", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            laura.setAffection(300);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 3));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.getAffection() == 340, "first diamond: affection is " + laura.getAffection());
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.getAffection() == 340, "the same gift counted again: affection is " + laura.getAffection());
            ctx.check(player.getMainHandItem().isEmpty(), "she refused the gift during the cooldown");
            ctx.check(laura.bags().count(s -> s.is(Items.DIAMOND)) == 3, "the diamonds are not in her bag");
            ctx.check(LauraAdvancements.get(player, "gifts") == 1, "gift counter is " + LauraAdvancements.get(player, "gifts"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.getAffection() == 365, "another kind of gift did not count: affection is " + laura.getAffection());
            // A wish is still fulfilled during the cooldown.
            long now = laura.level().getGameTime();
            laura.brain().forceDesire(new Desire(DesireType.Kind.ITEM, "minecraft:diamond", false, now, now + 20 * 600));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.brain().desire() == null, "the wish was not fulfilled during the cooldown");
            // The cooldown is saved with her.
            CompoundTag tag = laura.saveWithoutId(new CompoundTag());
            ctx.check(tag.getCompoundOrEmpty("LauraBrain").getCompoundOrEmpty("GiftCooldowns").getLongOr("minecraft:diamond", 0L) > 0, "the cooldown is not saved");
            // And a setting: 0 turns the limit off.
            int old = LauraConfig.giftCooldownSeconds.getInt();
            ctx.onCleanup(() -> LauraConfig.giftCooldownSeconds.set(old));
            LauraConfig.giftCooldownSeconds.set(0);
            laura.setAffection(300);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.getAffection() == 340, "giftCooldownSeconds = 0 did not turn the limit off: " + laura.getAffection());
            ctx.succeed();
        }));
        // Eight cookies in a row: she eats them all, says one line per cookie, and only the first one
        // makes her fonder.
        add(tests, "feeding_one_line_and_limit", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            laura.setAffection(300);
            laura.brain().needs().set(Needs.Need.HUNGER, 30);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 8));
            long spoken = LauraSpeech.spokenCount();
            for (int i = 0; i < 8; i++) {
                LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            }
            ctx.check(player.getMainHandItem().isEmpty(), "she did not eat every cookie: " + player.getMainHandItem().getCount() + " left");
            ctx.check(LauraSpeech.spokenCount() - spoken == 8, "she said " + (LauraSpeech.spokenCount() - spoken) + " lines for 8 cookies");
            ctx.check(laura.getAffection() == 305, "8 cookies gave " + (laura.getAffection() - 300) + " affection");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.getAffection() == 310, "another favorite food did not count: " + laura.getAffection());
            ctx.succeed();
        }));
        // One interaction per player and per tick: a click that reaches the server twice hands over one item.
        add(tests, "interact_once_per_tick", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 4));
            laura.mobInteract(player, InteractionHand.MAIN_HAND);
            laura.mobInteract(player, InteractionHand.MAIN_HAND);
            ctx.check(player.getMainHandItem().getCount() == 3, "two cookies went in one tick: " + player.getMainHandItem().getCount() + " left");
            ctx.after(3, () -> {
                laura.mobInteract(player, InteractionHand.MAIN_HAND);
                ctx.check(player.getMainHandItem().getCount() == 2, "the next click was ignored");
                ctx.succeed();
            });
        }));
        // What she gives back is named for what it is (not "0 Air") and really reaches the player:
        // in their inventory, or at their feet when it is full.
        add(tests, "return_gift_named_and_delivered", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            ItemStack first = laura.brain().giveReturnGift(player);
            ctx.check(!first.isEmpty() && first.getCount() > 0, "the announced return gift is " + first);
            ctx.check(!first.getHoverName().getString().equals(ItemStack.EMPTY.getHoverName().getString()), "the announced return gift is named like an empty stack");
            ctx.check(player.getInventory().countItem(first.getItem()) == first.getCount(),
                    "the player has " + player.getInventory().countItem(first.getItem()) + " of " + first);
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            }
            ItemStack second = laura.brain().giveReturnGift(player);
            ctx.check(!second.isEmpty(), "no second return gift");
            ctx.check(count(ctx, player.getBoundingBox().inflate(4), second.getItem()) == second.getCount(),
                    "with a full inventory the return gift did not drop at the player's feet");
            // Other callers read the stack after handing it over: it must stay as it was.
            ItemStack bread = new ItemStack(Items.BREAD, 5);
            laura.brain().giveToPlayer(player, bread);
            ctx.check(bread.getCount() == 5 && bread.is(Items.BREAD), "giveToPlayer emptied the stack it was given");
            ctx.succeed();
        }));
        // The name tag of the default gift and wish tables can be given (an unnamed one: the game
        // uses a named tag to rename her before the mod sees it).
        add(tests, "name_tag_gift_and_wish", 200, ctx -> withLaura(ctx, laura -> {
            defaultGifts(ctx);
            ServerPlayer player = ctx.player();
            long now = laura.level().getGameTime();
            laura.brain().forceDesire(new Desire(DesireType.Kind.ITEM, "minecraft:name_tag", false, now, now + 20 * 600));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NAME_TAG));
            laura.mobInteract(player, InteractionHand.MAIN_HAND);
            ctx.check(player.getMainHandItem().isEmpty(), "she did not take the name tag");
            ctx.check(laura.brain().desire() == null, "the name tag wish is still open");
            ctx.check(LauraAdvancements.get(player, "gifts") == 1, "the name tag was not taken as a gift");
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ her bag, sleep, gag
        // Lowering general.inventoryRows drops what no longer fits at her feet, as the guide says.
        add(tests, "inventory_rows_lowered", 300, ctx -> {
            int old = LauraConfig.inventoryRows.getInt();
            ctx.onCleanup(() -> LauraConfig.inventoryRows.set(old));
            LauraConfig.inventoryRows.set(6);
            LauraEntity big = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
            ctx.check(big != null && big.inventory().getContainerSize() == 54, "could not create a companion with 6 rows");
            for (int i = 0; i < 54; i++) {
                big.inventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            }
            CompoundTag tag = big.saveWithoutId(new CompoundTag());
            big.discard();
            LauraConfig.inventoryRows.set(1);
            LauraEntity small = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
            ctx.check(small != null, "could not create a companion with 1 row");
            small.load(tag);
            ctx.check(small.inventory().getContainerSize() == 9, "her bag has " + small.inventory().getContainerSize() + " slots");
            ctx.check(small.inventory().countItem(Items.COBBLESTONE) == 9 * 64, "her bag holds " + small.inventory().countItem(Items.COBBLESTONE));
            // Saved again before her first tick: nothing is lost either.
            CompoundTag again = small.saveWithoutId(new CompoundTag());
            ctx.check(again.getListOrEmpty("Inventory").size() == 54, "the items waiting to be dropped are not saved");
            small.snapTo(ctx.origin.getX() + 3.5, ctx.origin.getY(), ctx.origin.getZ() + 3.5, 0, 0);
            ctx.check(ctx.level.addFreshEntity(small), "could not add her to the world");
            ctx.waitFor("the items that no longer fit", 100,
                    () -> count(ctx, small.getBoundingBox().inflate(6), Items.COBBLESTONE) == 45 * 64, ctx::succeed);
        });
        // The sleep order: she is not woken by the first daylight check, and wakes up by herself once rested.
        add(tests, "sleep_order_wakes_up", 400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            laura.brain().needs().set(Needs.Need.ENERGY, 50);
            LauraActions.perform(player, laura, LauraAction.SLEEP, "", LauraActions.Source.COMMAND);
            ctx.check(laura.isAsleep(), "she did not go to sleep");
            ctx.after(60, () -> {
                ctx.check(laura.isAsleep(), "the ordered nap was cut short");
                laura.brain().needs().set(Needs.Need.ENERGY, 100);
                ctx.waitFor("her to wake up by herself", 100, () -> !laura.isAsleep(), ctx::succeed);
            });
        }));
        // gag.durationSeconds = 0 means "never": the gag must survive a save and a load.
        add(tests, "gag_without_timer_survives_reload", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            int old = LauraConfig.gagSeconds.getInt();
            ctx.onCleanup(() -> LauraConfig.gagSeconds.set(old));
            LauraConfig.gagSeconds.set(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK));
            LauraActions.gag(player, laura, player.getMainHandItem());
            ctx.check(laura.isGagged() && laura.gagTicksLeft() == 0, "gag without a timer: " + laura.gagTicksLeft());
            CompoundTag tag = laura.saveWithoutId(new CompoundTag());
            ctx.check(tag.getIntOr("GagTicks", 0) == 0, "saved timer is " + tag.getIntOr("GagTicks", 0));
            LauraEntity copy = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
            ctx.check(copy != null, "could not create a copy");
            copy.load(tag);
            ctx.check(copy.isGagged() && copy.gagTicksLeft() == 0, "after a reload the gag has a timer of " + copy.gagTicksLeft() + " ticks");
            copy.discard();
            ctx.after(40, () -> {
                ctx.check(laura.isGagged(), "she removed a gag that has no timer");
                ctx.succeed();
            });
        }));
        // Dismissed while she sleeps in a bed: the bed is free again and she comes back awake.
        add(tests, "dismiss_frees_bed", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos foot = ctx.origin.offset(2, 0, 2);
            BlockPos head = foot.relative(Direction.NORTH);
            BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
            ctx.level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 18);
            ctx.level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 18);
            laura.goToSleep(head);
            ctx.check(laura.isSleeping() && ctx.level.getBlockState(head).getValue(BedBlock.OCCUPIED), "she is not sleeping in the bed");
            LauraManager.dismiss(player, laura);
            ctx.check(!ctx.level.getBlockState(head).getValue(BedBlock.OCCUPIED), "the bed is still occupied after the dismissal");
            resetCooldown(ctx);
            LauraManager.summon(player, false);
            ctx.waitFor("her to come back", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
                LauraEntity back = LauraManager.findAll(player).get(0);
                ctx.check(!back.isAsleep(), "she came back asleep");
                ctx.succeed();
            });
        }));

        // ------------------------------------------------------------------ admin
        // /laura admin remove: a loaded companion leaves her belongings, one that is not loaded leaves
        // when she is loaded again instead of registering herself.
        add(tests, "admin_remove", 400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            String command = "laura admin remove " + player.getGameProfile().name();
            laura.inventory().setItem(0, new ItemStack(Items.EMERALD, 7));
            AABB around = laura.getBoundingBox().inflate(4);
            ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
            ctx.check(laura.isRemoved(), "the loaded companion is still there");
            ctx.check(count(ctx, around, Items.EMERALD) == 7, "the belongings of the loaded companion were deleted");
            ctx.check(LauraWorldData.get(ctx.server()).byOwner(player.getUUID()).isEmpty(), "her record is still there");
            resetCooldown(ctx);
            LauraManager.summon(player, false);
            ctx.waitFor("a second companion", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
                LauraEntity second = LauraManager.findAll(player).get(0);
                second.inventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
                CompoundTag snapshot = second.saveWithoutId(new CompoundTag());
                // Like a companion in an unloaded chunk: registered, but her entity cannot be found.
                second.discard();
                ctx.after(2, () -> {
                    ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
                    ctx.check(LauraWorldData.get(ctx.server()).byOwner(player.getUUID()).isEmpty(), "the record of the unloaded companion is still there");
                    // Her chunk loads: she is back in the world.
                    LauraEntity loaded = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
                    ctx.check(loaded != null, "could not create her");
                    loaded.load(snapshot);
                    loaded.snapTo(ctx.origin.getX() - 4.5, ctx.origin.getY(), ctx.origin.getZ() - 4.5, 0, 0);
                    ctx.check(ctx.level.addFreshEntity(loaded), "could not add her to the world");
                    AABB there = loaded.getBoundingBox().inflate(4);
                    LauraManager.track(loaded);
                    ctx.check(loaded.isRemoved(), "the removed companion stayed in the world");
                    ctx.check(LauraWorldData.get(ctx.server()).byOwner(player.getUUID()).isEmpty(), "the removed companion registered herself again");
                    ctx.check(count(ctx, there, Items.DIAMOND) == 5, "her belongings were not dropped");
                    ctx.succeed();
                });
            });
        }));
        // A wrong value in recipes.json or desires.json costs its own entry, never the reload.
        add(tests, "bad_config_values", 300, ctx -> {
            Path recipes = LauraMod.configDir().resolve("recipes.json");
            Path desires = LauraMod.configDir().resolve("desires.json");
            byte[] oldRecipes = Files.isRegularFile(recipes) ? Files.readAllBytes(recipes) : null;
            byte[] oldDesires = Files.isRegularFile(desires) ? Files.readAllBytes(desires) : null;
            ctx.onCleanup(() -> {
                restore(recipes, oldRecipes);
                restore(desires, oldDesires);
                LauraMod.reloadAll();
            });
            Files.writeString(recipes, """
                    { "requireCraftingTable": {}, "useCampfires": [true, false], "meals": [
                      { "id": "selftest_ok", "result": "minecraft:bread", "count": 1, "ingredients": { "minecraft:wheat": 3 } },
                      { "id": "selftest_count", "result": "minecraft:bread", "count": [1, 2], "ingredients": { "minecraft:wheat": 3 } },
                      { "id": "selftest_amount", "result": "minecraft:bread", "ingredients": { "minecraft:wheat": "many" } },
                      { "id": "selftest_result", "result": { "item": "minecraft:bread" }, "ingredients": { "minecraft:wheat": 1 } },
                      { "id": "selftest_returns", "result": "minecraft:cake", "ingredients": { "minecraft:wheat": 1 }, "returns": { "minecraft:bucket": null } }
                    ] }
                    """, StandardCharsets.UTF_8);
            Files.writeString(desires, """
                    { "items": [ { "item": 5 }, { "item": "minecraft:poppy", "weight": "heavy" } ],
                      "places": [ { "id": "selftest_place", "biomes": [ {}, null, [1], "minecraft:plains" ], "weight": {} } ],
                      "activities": [ { "id": ["dance"] }, { "id": "hug", "weight": 5 } ] }
                    """, StandardCharsets.UTF_8);
            LauraMod.reloadAll();
            List<String> ids = Cookbook.meals().stream().map(Cookbook.Meal::id).toList();
            ctx.check(ids.contains("selftest_ok"), "the valid meal was lost: " + ids);
            ctx.check(ids.stream().noneMatch(id -> id.startsWith("selftest_") && !id.equals("selftest_ok")), "an invalid meal was kept: " + ids);
            ctx.check(Cookbook.requireCraftingTable() && Cookbook.useCampfires(), "a wrong flag did not fall back to its default");
            DesireTable.PlaceDesire place = DesireTable.place("selftest_place");
            ctx.check(place != null && place.targets().equals(List.of("minecraft:plains")), "the place with wrong biome values: " + place);
            ctx.check(DesireTable.items().size() == 1 && DesireTable.activities().size() == 1, "desires kept: " + DesireTable.items().size() + " items, " + DesireTable.activities().size() + " activities");
            ctx.check(DialogueManager.languages().size() >= 2, "the dialogues were not reloaded after the bad files");
            ctx.succeed();
        });

        // ------------------------------------------------------------------ orders from other players
        // A player who may not command her is answered in private, once every few seconds: nobody
        // can make somebody else's companion talk to everyone in earshot.
        add(tests, "refusal_private_and_limited", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer other = MockPlayers.create(ctx.level, "LauraOther" + (ctx.origin.getZ() / 64));
            ctx.onCleanup(() -> MockPlayers.remove(other));
            other.teleportTo(laura.getX() + 2, laura.getY(), laura.getZ());
            long spoken = LauraSpeech.spokenCount();
            long refused = LauraSpeech.refusalCount();
            for (int i = 0; i < 5; i++) {
                LauraActions.perform(other, laura, LauraAction.FOLLOW, "", LauraActions.Source.MENU);
            }
            laura.mobInteract(other, InteractionHand.MAIN_HAND);
            ctx.check(LauraSpeech.spokenCount() == spoken, "a refusal was said to everyone in earshot");
            ctx.check(LauraSpeech.refusalCount() - refused == 1, "the refused player got " + (LauraSpeech.refusalCount() - refused) + " answers");
            ctx.succeed();
        }));
        // Menu actions are limited per player: the packets beyond the budget are dropped.
        add(tests, "action_budget", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            for (int i = 1; i <= 60; i++) {
                FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_ACTION);
                buf.writeVarInt(laura.getId());
                buf.writeVarInt(LauraAction.RENAME.ordinal());
                buf.writeUtf("N" + i, 256);
                LauraNetwork.handleServer(player, LauraNetwork.toBytes(buf));
            }
            ctx.check(laura.getLauraName().equals("N" + LauraNetwork.ACTIONS_PER_SECOND), "after 60 packets in one tick her name is " + laura.getLauraName());
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ skins, uploads, models
        // A finished upload builds its own entry (no scan of the folders), files are named after the
        // player's UUID, and a second upload right after the first is refused.
        add(tests, "upload_store_and_cooldown", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            String prefix = SkinService.uploadPrefix(player);
            ctx.check(prefix.equals(player.getUUID().toString().replace("-", "") + "_") && prefix.length() == 33, "upload prefix is " + prefix);
            Path uploads = ServerAssetStore.dir(AssetKind.SKIN).resolve("uploads");
            ctx.onCleanup(() -> {
                try {
                    Files.deleteIfExists(uploads.resolve(prefix + "selftest_a.png"));
                    Files.deleteIfExists(uploads.resolve(prefix + "selftest_b.png"));
                } catch (java.io.IOException ignored) {
                    // Best effort.
                }
                ServerAssetStore.rescan();
            });
            byte[] skin = pngHeader(64, 64);
            int scans = ServerAssetStore.scans();
            int writes = ServerAssetStore.storeWrites();
            LauraNetwork.handleServer(player, uploadPacket("selftest_a.png", skin, laura.getId()));
            ServerAssetStore.Entry entry = ServerAssetStore.get(AssetKind.SKIN, "uploads/" + prefix + "selftest_a");
            ctx.check(entry != null, "the uploaded skin is not listed");
            ctx.check(entry.sha1().equals(ServerAssetStore.sha1(skin)), "wrong hash for the uploaded skin");
            ctx.check(laura.getSkinRaw().equals("server:" + entry.name() + "#" + entry.sha1()), "skin is " + laura.getSkinRaw());
            ctx.check(ServerAssetStore.scans() == scans, "the upload scanned the folders");
            ctx.check(ServerAssetStore.storeWrites() == writes + 1, "the upload was not written once");
            // The same file again: nothing to write.
            ServerAssetStore.Entry same = storeAgain(prefix + "selftest_a", skin);
            ctx.check(same != null && ServerAssetStore.storeWrites() == writes + 1, "an identical upload was written again");
            // Another upload right away is refused.
            LauraNetwork.handleServer(player, uploadPacket("selftest_b.png", skin, laura.getId()));
            ctx.check(ServerAssetStore.get(AssetKind.SKIN, "uploads/" + prefix + "selftest_b") == null, "a second upload was accepted right after the first");
            ctx.check(!Files.exists(uploads.resolve(prefix + "selftest_b.png")), "the refused upload was written");
            ctx.succeed();
        }));
        // Her look changes at most once every few seconds: every client near her keeps a texture per look.
        add(tests, "look_change_cooldown", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            SkinService.requestSkin(player, laura, "builtin:laura_summer", true);
            ctx.check(laura.getSkinRaw().equals("builtin:laura_summer"), "skin is " + laura.getSkinRaw());
            SkinService.requestSkin(player, laura, "builtin:laura_winter", true);
            ctx.check(laura.getSkinRaw().equals("builtin:laura_summer"), "a second skin change was accepted right away");
            SkinService.requestModel(player, laura, "pack:selftest_one");
            ctx.check(laura.getModelName().equals("pack:selftest_one"), "model is " + laura.getModelName());
            SkinService.requestModel(player, laura, "pack:selftest_two");
            ctx.check(laura.getModelName().equals("pack:selftest_one"), "a second model change was accepted right away");
            ctx.after(60, () -> {
                SkinService.requestSkin(player, laura, "builtin:laura_winter", true);
                ctx.check(laura.getSkinRaw().equals("builtin:laura_winter"), "the skin could not be changed after the cooldown");
                ctx.succeed();
            });
        }));
        // Account skins: the cache is bounded, and one player gets one lookup every few seconds.
        // The answers are stored by hand: the tests never go online.
        add(tests, "player_skin_lookup_limits", 300, ctx -> {
            ServerPlayer player = ctx.player();
            for (int i = 0; i < MojangSkinResolver.CACHE_SIZE + 60; i++) {
                String name = "lt_name" + i;
                MojangSkinResolver.remember(name, Optional.of(new MojangSkinResolver.Result(name, "https://textures.minecraft.net/texture/selftest" + i, false)), 60_000);
            }
            ctx.check(MojangSkinResolver.cachedCount() == MojangSkinResolver.CACHE_SIZE, "the cache holds " + MojangSkinResolver.cachedCount() + " names");
            String last = "lt_name" + (MojangSkinResolver.CACHE_SIZE + 59);
            ctx.check(MojangSkinResolver.resolve(last).isDone(), "a remembered name started a lookup");
            LauraManager.summon(player, false);
            resetCooldown(ctx);
            LauraManager.summon(player, false);
            ctx.waitFor("two companions", 100, () -> LauraManager.findAll(player).size() == 2, () -> {
                LauraEntity first = LauraManager.findAll(player).get(0);
                LauraEntity second = LauraManager.findAll(player).get(1);
                String before = second.getSkinRaw();
                SkinService.requestSkin(player, first, "player:" + last, true);
                SkinService.requestSkin(player, second, "player:lt_name" + (MojangSkinResolver.CACHE_SIZE + 58), true);
                ctx.after(5, () -> {
                    ctx.check(first.getSkinRaw().startsWith("url:https://textures.minecraft.net/texture/selftest"), "the first lookup was not applied: " + first.getSkinRaw());
                    ctx.check(second.getSkinRaw().equals(before), "a second lookup by the same player was accepted right away");
                    ctx.succeed();
                });
            });
        });
        // URL skins: addresses of the local network are refused, whatever their family.
        add(tests, "private_addresses_refused", 100, ctx -> {
            for (String local : new String[]{"fd12:3456:789a::1", "fc00::1", "100.64.0.1", "100.127.255.254", "0.0.0.1", "192.168.1.1",
                    "10.0.0.1", "172.16.0.1", "127.0.0.1", "169.254.1.1", "fe80::1", "::1", "::ffff:10.0.0.1"}) {
                ctx.check(SkinRef.isLocalAddress(InetAddress.getByName(local)), local + " is allowed");
            }
            for (String remote : new String[]{"8.8.8.8", "1.1.1.1", "100.63.255.255", "100.128.0.1", "2606:4700:4700::1111", "fb00::1"}) {
                ctx.check(!SkinRef.isLocalAddress(InetAddress.getByName(remote)), remote + " is refused");
            }
            ctx.succeed();
        });
        // A model that passes the size check cannot freeze or exhaust the clients that render it.
        add(tests, "model_limits", 100, ctx -> {
            Molang.Context molang = new Molang.Context();
            molang.animTime = 1;
            long start = System.nanoTime();
            double sum = Molang.parse("math.die_roll(2147483647, 0, 1)").eval(molang);
            ctx.check(sum <= Molang.MAX_DIE_ROLLS && System.nanoTime() - start < 200_000_000L, "die_roll is not bounded: " + sum);
            ctx.check(Molang.parse("math.die_roll(-5, 1, 1)").eval(molang) == 0, "negative die_roll count");
            ctx.check(Math.abs(Molang.parse("math.sin(query.anim_time * 90) * 2").eval(molang) - 2) < 1e-6, "a normal expression is broken");
            ctx.check(Molang.parse("((((1 + 2)))) * 3").eval(molang) == 9, "nested parentheses are broken");
            ctx.check(Molang.parse("(".repeat(100) + "1" + ")".repeat(100)).eval(molang) == 0, "an expression nested 100 deep was accepted");
            ctx.check(Molang.parse("-".repeat(200) + "1").eval(molang) == 0, "200 unary signs were accepted");
            ctx.check(Molang.parse("(".repeat(200_000) + "1" + ")".repeat(200_000)).eval(molang) == 0, "a huge expression was accepted");

            String cube = "\"elements\":[{\"type\":\"cube\",\"uuid\":\"c1\",\"from\":[0,0,0],\"to\":[1,1,1]}]";
            String head = "{\"meta\":{\"box_uv\":true},\"resolution\":{\"width\":64,\"height\":64}," + cube + ",";
            ctx.check(ModelParser.parse("ok.bbmodel", head + "\"outliner\":[\"c1\"]," + texture(64, 64, 1) + "}", null).cubeCount() == 1, "a small model was refused");
            ctx.check(refused(head + "\"outliner\":[\"c1\"]," + texture(8192, 8192, 1) + "}"), "a 8192x8192 texture was accepted");
            ctx.check(refused(head + "\"outliner\":[\"c1\"]," + texture(2048, 2048, 3) + "}"), "three 2048x2048 textures were accepted");
            ctx.check(refused(head + "\"outliner\":[\"c1\"]," + texture(16, 16, ModelParser.MAX_TEXTURES + 1) + "}"), "too many textures were accepted");
            ctx.check(refused(head + "\"outliner\":[\"c1\"],\"textures\":[{\"source\":\"data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[40]) + "\"}]}"),
                    "a texture that is not a PNG was accepted");
            StringBuilder nested = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                nested.append("{\"name\":\"g").append(i).append("\",\"uuid\":\"g").append(i).append("\",\"children\":[");
            }
            nested.append("\"c1\"");
            nested.append("]}".repeat(200));
            ctx.check(refused(head + "\"outliner\":[" + nested + "]}"), "groups nested 200 deep were accepted");
            StringBuilder many = new StringBuilder();
            for (int i = 0; i <= ModelParser.MAX_BONES; i++) {
                many.append("{\"name\":\"b").append(i).append("\",\"uuid\":\"b").append(i).append("\",\"children\":[]},");
            }
            ctx.check(refused(head + "\"outliner\":[" + many + "\"c1\"]}"), "more bones than the limit were accepted");
            String loop = "{\"minecraft:geometry\":[{\"description\":{\"texture_width\":16,\"texture_height\":16},\"bones\":["
                    + "{\"name\":\"a\",\"parent\":\"b\",\"cubes\":[{\"origin\":[0,0,0],\"size\":[1,1,1],\"uv\":[0,0]}]},{\"name\":\"b\",\"parent\":\"a\"}]}]}";
            boolean loopRefused = false;
            try {
                ModelParser.parse("loop.geo.json", loop, null);
            } catch (IllegalArgumentException e) {
                loopRefused = true;
            }
            ctx.check(loopRefused, "bones that are each other's parent were accepted");
            ctx.succeed();
        });
        // What the client uses to keep its downloads in check (memory and disk), without a client.
        add(tests, "download_cache_limits", 100, ctx -> {
            List<String> evicted = new ArrayList<>();
            LruCache<String, String> cache = new LruCache<>(3, evicted::add);
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            ctx.check("A".equals(cache.get("a")), "get");
            cache.put("d", "D");
            ctx.check(evicted.equals(List.of("B")) && cache.size() == 3, "the least recently used entry was not the one removed: " + evicted);
            ctx.check(cache.get("b") == null && cache.get("a") != null, "wrong entry removed");
            cache.clear();
            ctx.check(evicted.size() == 4 && cache.size() == 0, "clear did not release every entry: " + evicted);

            Path dir = LauraMod.platform().gameDir().resolve("lauramod-selftest-cache");
            Files.createDirectories(dir);
            Path old = dir.resolve("old.png");
            Path mid = dir.resolve("mid.png");
            Path fresh = dir.resolve("fresh.png");
            ctx.onCleanup(() -> {
                for (Path p : new Path[]{old, mid, fresh, dir}) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (java.io.IOException ignored) {
                        // Best effort.
                    }
                }
            });
            long day = 24L * 60 * 60 * 1000;
            long now = System.currentTimeMillis();
            for (Path p : new Path[]{old, mid, fresh}) {
                Files.write(p, new byte[100]);
            }
            Files.setLastModifiedTime(old, FileTime.fromMillis(now - 40 * day));
            Files.setLastModifiedTime(mid, FileTime.fromMillis(now - 10 * day));
            ctx.check(CacheFiles.prune(dir, 30 * day, 1000) == 1 && !Files.exists(old) && Files.exists(mid), "the file older than the limit was not the one deleted");
            ctx.check(CacheFiles.prune(dir, 30 * day, 150) == 1 && !Files.exists(mid) && Files.exists(fresh), "the size limit did not delete the oldest file");
            Files.setLastModifiedTime(fresh, FileTime.fromMillis(now - 40 * day));
            CacheFiles.touch(fresh);
            ctx.check(CacheFiles.prune(dir, 30 * day, 1000) == 0 && Files.exists(fresh), "a file used again was deleted");
            ctx.succeed();
        });

        // ------------------------------------------------------------------ optional mods
        if (LauraMod.platform().isModLoaded("curios") && LauraMod.platform().isModLoaded("kubejs")) {
            // Needs run-selftest/kubejs/server_scripts/laura_selftest.js (it lets a gold nugget go in a ring slot).
            // With the GRAVE revive mode she comes back with her trinkets: Curios must not drop them as well.
            add(tests, "compat_curios_death", 600, ctx -> withLaura(ctx, laura -> {
                ServerPlayer player = ctx.player();
                LauraConfig.ReviveMode old = LauraConfig.reviveMode.get();
                ctx.onCleanup(() -> LauraConfig.reviveMode.set(old));
                LauraConfig.reviveMode.set(LauraConfig.ReviveMode.GRAVE);
                ctx.check(LauraMod.platform().equipTrinket(laura, new ItemStack(Items.GOLD_NUGGET), false), "the trinket could not be put on her");
                ctx.check(nuggets(LauraMod.platform().trinkets(laura)) == 1, "she does not wear the trinket");
                AABB around = laura.getBoundingBox().inflate(6);
                laura.setInvulnerable(false);
                laura.hurt(ctx.level.damageSources().genericKill(), Float.MAX_VALUE);
                ctx.waitFor("her death", 60, () -> !laura.isAlive(), () -> ctx.after(5, () -> {
                    ctx.check(count(ctx, around, Items.GOLD_NUGGET) == 0, "her trinket was dropped although she keeps her belongings");
                    BlockPos grave = ctx.origin.offset(2, 0, 0);
                    ctx.level.setBlockAndUpdate(grave, LauraRegistries.GRAVE.get().defaultBlockState());
                    ctx.check(LauraManager.reviveAtGrave(player, grave), "revive failed");
                    ctx.waitFor("her return", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
                        LauraEntity back = LauraManager.findAll(player).get(0);
                        ctx.check(nuggets(LauraMod.platform().trinkets(back)) == 1, "she came back with " + nuggets(LauraMod.platform().trinkets(back)) + " trinkets");
                        AABB there = back.getBoundingBox().inflate(4);
                        LauraManager.release(player, back);
                        ctx.check(count(ctx, there, Items.GOLD_NUGGET) == 1, "released: her trinket was not dropped");
                        ctx.succeed();
                    });
                }));
            }));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static ServerAssetStore.Entry storeAgain(String stem, byte[] data) {
        try {
            return ServerAssetStore.storeUpload(AssetKind.SKIN, stem, data);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** The self tests check the built-in gift table, not a file an earlier run left behind. */
    private static void defaultGifts(TestRunner.Context ctx) {
        GiftTable.loadDefaults();
        ctx.onCleanup(GiftTable::load);
    }

    private static int nuggets(List<ItemStack> stacks) {
        int n = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(Items.GOLD_NUGGET)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** Items of one kind lying on the ground in the box. */
    private static int count(TestRunner.Context ctx, AABB box, Item item) {
        int n = 0;
        for (ItemEntity entity : ctx.level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && e.getItem().is(item))) {
            n += entity.getItem().getCount();
        }
        return n;
    }

    private static void restore(Path file, byte[] content) {
        try {
            if (content == null) {
                Files.deleteIfExists(file);
            } else {
                Files.write(file, content);
            }
        } catch (java.io.IOException ignored) {
            // Best effort.
        }
    }

    private static boolean refused(String bbmodel) {
        try {
            ModelParser.parse("selftest.bbmodel", bbmodel, null);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /** The "textures" member of a .bbmodel: {@code count} embedded PNG files of the given size. */
    private static String texture(int width, int height, int count) {
        String source = "{\"source\":\"data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeader(width, height)) + "\"}";
        StringBuilder sb = new StringBuilder("\"textures\":[");
        for (int i = 0; i < count; i++) {
            sb.append(i == 0 ? "" : ",").append(source);
        }
        return sb.append("]").toString();
    }

    /** The first bytes of a PNG file: enough for the checks that read its size without decoding it. */
    private static byte[] pngHeader(int width, int height) {
        byte[] b = new byte[33];
        byte[] magic = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        System.arraycopy(magic, 0, b, 0, magic.length);
        b[11] = 13;
        b[12] = 'I';
        b[13] = 'H';
        b[14] = 'D';
        b[15] = 'R';
        for (int i = 0; i < 4; i++) {
            b[16 + i] = (byte) (width >>> (24 - 8 * i));
            b[20 + i] = (byte) (height >>> (24 - 8 * i));
        }
        b[24] = 8;
        b[25] = 6;
        return b;
    }

    /** A whole skin upload in one packet, as the client sends it. */
    private static byte[] uploadPacket(String name, byte[] data, int entityId) {
        FriendlyByteBuf buf = LauraNetwork.buffer(LauraNetwork.C2S_UPLOAD);
        buf.writeVarInt(AssetKind.SKIN.ordinal());
        buf.writeUtf(name, 128);
        buf.writeVarInt(data.length);
        buf.writeVarInt(0);
        buf.writeByteArray(data);
        buf.writeVarInt(entityId);
        buf.writeBoolean(true);
        return LauraNetwork.toBytes(buf);
    }

    private static void resetCooldown(TestRunner.Context ctx) {
        LauraWorldData.get(ctx.server()).meta(ctx.player().getUUID()).lastSummon = 0;
    }

    /** Summons a companion for the test player, then runs the body once she is in the world. */
    private static void withLaura(TestRunner.Context ctx, Consumer<LauraEntity> body) {
        ServerPlayer player = ctx.player();
        // The test world keeps the data of its fake players from one run to the next.
        player.getInventory().clearContent();
        resetCooldown(ctx);
        LauraManager.summon(player, false);
        ctx.waitFor("the summon", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
            LauraEntity laura = LauraManager.findAll(player).get(0);
            laura.brain().needs().fillAll();
            body.accept(laura);
        });
    }
}
