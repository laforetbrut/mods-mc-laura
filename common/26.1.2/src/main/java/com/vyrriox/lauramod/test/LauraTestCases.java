package com.vyrriox.lauramod.test;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.dialogue.TextMatcher;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.platform.CookingPots;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.LauraInventories;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.skin.SkinService;
import com.vyrriox.lauramod.util.ItemSpec;
import com.vyrriox.lauramod.world.LauraActions;
import com.vyrriox.lauramod.world.LauraAdvancements;
import com.vyrriox.lauramod.world.LauraChat;
import com.vyrriox.lauramod.world.LauraManager;
import com.vyrriox.lauramod.world.LauraWorldData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * The self test cases: every major feature is exercised in a real world with a fake player.
 *
 * @author vyrriox
 */
public final class LauraTestCases {
    private LauraTestCases() {
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

    public static void register(List<TestRunner.TestCase> tests) {
        // ------------------------------------------------------------------ content and data
        add(tests, "registries", 100, ctx -> {
            ctx.check(LauraRegistries.LAURA.get() != null, "entity type missing");
            ctx.check(LauraRegistries.HEART.get() != null && LauraRegistries.GRAVE.get() != null, "items or blocks missing");
            for (LauraRegistries.Sound sound : LauraRegistries.Sound.values()) {
                ctx.check(sound.get() != null, "sound " + sound.id() + " missing");
            }
            ctx.check(ctx.server().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, LauraMod.id("laura_heart"))).isPresent(), "heart recipe missing");
            ctx.check(ctx.server().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, LauraMod.id("laura_grave"))).isPresent(), "grave recipe missing");
            ctx.check(ctx.server().getAdvancements().get(LauraMod.id("laura/root")) != null, "advancements missing");
            ctx.succeed();
        });
        add(tests, "dialogues_loaded", 100, ctx -> {
            ctx.check(DialogueManager.languages().contains("en_us"), "English dialogues missing");
            ctx.check(DialogueManager.languages().contains("fr_fr"), "French dialogues missing");
            for (String lang : DialogueManager.languages()) {
                ctx.check(DialogueManager.sets().get(lang).lines("summon").size() > 0, lang + " has no summon line");
            }
            for (String lang : new String[]{"en_us", "fr_fr"}) {
                try (InputStream in = LauraMod.class.getResourceAsStream("/assets/lauramod/lang/" + lang + ".json")) {
                    ctx.check(in != null, "lang file " + lang + " missing");
                }
            }
            ctx.succeed();
        });
        add(tests, "chat_matching", 100, ctx -> {
            ctx.check(intent("follow me").equals("follow"), "follow me");
            ctx.check(intent("suis-moi").equals("follow"), "suis-moi");
            ctx.check(intent("Laura je t'aime !").equals("love_you"), "je t'aime");
            ctx.check(intent("I feel lonely").equals("summon"), "summon en");
            ctx.check(intent("je me sens seul").equals("summon"), "summon fr");
            ctx.check(intent("ÉPOUSE-MOI").equals("marry_me"), "accents and case");
            ctx.check(DialogueManager.match("the weather is nice today, let us talk about stuff") == null
                    || !DialogueManager.match("the weather is nice today, let us talk about stuff").intent().equals("summon"), "false summon");
            List<String> items = DialogueManager.matchItems(TextMatcher.Prepared.of("bring me some bread please"));
            ctx.check(!items.isEmpty() && items.get(0).equals("minecraft:bread"), "bread keyword: " + items);
            List<String> itemsFr = DialogueManager.matchItems(TextMatcher.Prepared.of("apporte-moi des pommes"));
            ctx.check(!itemsFr.isEmpty() && itemsFr.get(0).equals("minecraft:apple"), "pommes keyword: " + itemsFr);
            ctx.check(!intent("I have 3 apples").equals("love_you"), "a number fired love_you");
            DialogueManager.IntentMatch frenchDans = DialogueManager.match("je suis dans la maison", "fr_fr");
            ctx.check(frenchDans == null || !frenchDans.intent().equals("emote_dance"), "French word taken for a Dutch order");
            ctx.check(DialogueManager.match("follow me", "fr_fr") != null, "English is always understood");
            ctx.check(DialogueManager.splitOrders("cut a tree then come here").size() == 2, "connector then");
            ctx.check(DialogueManager.splitOrders("coupe un arbre puis viens ici").size() == 2, "connector puis");
            ctx.succeed();
        });
        // Frequent mistakes, chat shortcuts and filler words, per language.
        add(tests, "chat_spelling", 100, ctx -> {
            String[][] cases = {
                    {"fr_fr", "tu est jolie", "beautiful"}, {"fr_fr", "tes trop belle laura", "beautiful"},
                    {"fr_fr", "t'es vraiment mignone", "beautiful"}, {"fr_fr", "jtm", "love_you"}, {"fr_fr", "jtaime fort", "love_you"},
                    {"fr_fr", "slt", "hello"}, {"fr_fr", "sa va ?", "how_are_you"}, {"fr_fr", "tg", "insult"}, {"fr_fr", "Salope", "insult"},
                    {"en_us", "ur so pretty", "beautiful"}, {"en_us", "ily", "love_you"}, {"en_us", "how r u", "how_are_you"},
                    {"en_us", "thx", "thanks"}};
            for (String[] c : cases) {
                DialogueManager.IntentMatch m = DialogueManager.match(c[1], c[0]);
                ctx.check(m != null && m.intent().equals(c[2]), "\"" + c[1] + "\" gave " + (m == null ? "nothing" : m.intent()));
            }
            // Triggers that contain a filler word still match as typed.
            DialogueManager.IntentMatch typed = DialogueManager.match("trop mignonne", "fr_fr");
            ctx.check(typed != null && typed.intent().equals("beautiful"), "trigger with a filler word");
            ctx.succeed();
        });
        add(tests, "config_reload", 200, ctx -> {
            LauraMod.reloadAll();
            ctx.check(DialogueManager.languages().size() >= 2, "dialogues lost after reload");
            ctx.succeed();
        });

        // ------------------------------------------------------------------ summoning
        add(tests, "summon_command", 200, ctx -> withLaura(ctx, laura -> {
            ctx.check(laura.isOwnedBy(ctx.player()), "not owned by the summoner");
            ctx.check(laura.getLauraName().equals(LauraConfig.defaultName.get()), "default name");
            ctx.succeed();
        }));
        add(tests, "summon_chat_french", 200, ctx -> {
            ServerPlayer player = ctx.player();
            MockPlayers.setLanguage(player, "fr_fr");
            LauraChat.onChat(player, "je me sens seule...");
            ctx.waitFor("a companion", 100, () -> !LauraManager.findAll(player).isEmpty(), ctx::succeed);
        });
        add(tests, "several_companions", 200, ctx -> {
            ServerPlayer player = ctx.player();
            LauraManager.summon(player, false);
            resetCooldown(ctx);
            LauraManager.summon(player, false);
            ctx.waitFor("two companions", 100, () -> LauraManager.findAll(player).size() == 2, () -> {
                List<LauraEntity> all = LauraManager.findAll(player);
                ctx.check(!all.get(0).getLauraName().equals(all.get(1).getLauraName()), "both have the same name");
                ctx.succeed();
            });
        });
        add(tests, "rename", 200, ctx -> withLaura(ctx, laura -> {
            LauraActions.perform(ctx.player(), laura, LauraAction.RENAME, "Emma", LauraActions.Source.MENU);
            ctx.check(laura.getLauraName().equals("Emma"), "name is " + laura.getLauraName());
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ orders and movement
        add(tests, "stay_and_follow", 600, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraActions.perform(player, laura, LauraAction.STAY, "", LauraActions.Source.COMMAND);
            ctx.check(laura.getMode() == LauraMode.STAY, "mode is " + laura.getMode());
            BlockPos start = laura.blockPosition();
            player.teleportTo(ctx.origin.getX() + 12.5, ctx.origin.getY(), ctx.origin.getZ() + 0.5);
            ctx.after(60, () -> {
                ctx.check(laura.blockPosition().distSqr(start) < 4, "she moved while staying");
                LauraActions.perform(player, laura, LauraAction.FOLLOW, "", LauraActions.Source.COMMAND);
                ctx.waitFor("her to follow", 300, () -> laura.distanceTo(player) < 6, ctx::succeed);
            });
        }));
        // Under teleportDistance (128 by default) she must walk the whole way, never blink to the player.
        add(tests, "follow_walks", 400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            player.teleportTo(ctx.origin.getX() + 18.5, ctx.origin.getY(), ctx.origin.getZ() + 18.5);
            Vec3[] last = {laura.position()};
            ctx.waitFor("her to walk to the player", 300, () -> {
                double step = laura.position().distanceTo(last[0]);
                last[0] = laura.position();
                ctx.check(step < 3, "she jumped " + Math.round(step) + " blocks in one tick");
                return laura.distanceTo(player) < 8;
            }, ctx::succeed);
        }));
        // The threshold is a config value: lowered under the distance, she teleports instead of walking.
        add(tests, "follow_teleport", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            double old = LauraConfig.teleportDistance.getDouble();
            LauraConfig.teleportDistance.set(12.0);
            ctx.onCleanup(() -> LauraConfig.teleportDistance.set(old));
            player.teleportTo(ctx.origin.getX() + 18.5, ctx.origin.getY(), ctx.origin.getZ() + 18.5);
            Vec3[] last = {laura.position()};
            ctx.waitFor("her to teleport", 100, () -> {
                double step = laura.position().distanceTo(last[0]);
                last[0] = laura.position();
                return step > 5 && laura.distanceTo(player) < 8;
            }, ctx::succeed);
        }));
        add(tests, "proximity_chat", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraActions.perform(player, laura, LauraAction.STAY, "", LauraActions.Source.COMMAND);
            double range = LauraConfig.chatRange.getInt();
            player.teleportTo(laura.getX() + range + 20, laura.getY() + 30, laura.getZ());
            LauraChat.onChat(player, "follow me");
            ctx.after(10, () -> {
                ctx.check(laura.getMode() == LauraMode.STAY, "she heard from too far away");
                player.teleportTo(laura.getX() + 3, laura.getY(), laura.getZ());
                LauraChat.onChat(player, "follow me");
                ctx.after(10, () -> {
                    ctx.check(laura.getMode() == LauraMode.FOLLOW, "she did not hear from close by");
                    ctx.succeed();
                });
            });
        }));
        add(tests, "emote", 200, ctx -> withLaura(ctx, laura -> {
            LauraActions.perform(ctx.player(), laura, LauraAction.EMOTE, "CLAP", LauraActions.Source.MENU);
            ctx.check(laura.getEmote() == Emote.CLAP, "emote is " + laura.getEmote());
            ctx.check(LauraAdvancements.get(ctx.player(), "emote.clap") == 1, "emote counter");
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ needs, gifts, desires
        add(tests, "hungry_eats_bread", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            laura.brain().needs().set(Needs.Need.HUNGER, 10);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.brain().needs().get(Needs.Need.HUNGER) > 10, "hunger did not go up");
            ctx.check(player.getMainHandItem().getCount() == 2, "bread not consumed");
            ctx.succeed();
        }));
        add(tests, "desire_fulfilled", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            long now = laura.level().getGameTime();
            laura.brain().forceDesire(new Desire(DesireType.Kind.ITEM, "minecraft:poppy", false, now, now + 20 * 600));
            ctx.check(laura.brain().desire() != null, "desire not set");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(laura.brain().desire() == null, "desire still active");
            ctx.check(LauraAdvancements.get(player, "desires") == 1, "desire counter");
            ctx.succeed();
        }));
        // Every music disc is a gift: the default table matches them by what they are (@music_disc),
        // not by a tag that some Minecraft versions or loaders do not have.
        add(tests, "gift_music_disc", 200, ctx -> withLaura(ctx, laura -> {
            ItemSpec discs = ItemSpec.parse("@music_disc").orElse(null);
            ctx.check(discs != null, "@music_disc not understood");
            for (Item disc : List.of(Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_PIGSTEP,
                    Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_5, Items.MUSIC_DISC_RELIC)) {
                ctx.check(discs.test(new ItemStack(disc)), BuiltInRegistries.ITEM.getKey(disc) + " is not a music disc");
            }
            ctx.check(!discs.test(new ItemStack(Items.JUKEBOX)), "a jukebox counted as a music disc");
            GiftTable.loadDefaults();
            ctx.onCleanup(GiftTable::load);
            ServerPlayer player = ctx.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_PIGSTEP));
            LauraActions.giveItem(player, laura, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            ctx.check(player.getInventory().countItem(Items.MUSIC_DISC_PIGSTEP) == 0, "she did not take the disc");
            ctx.check(LauraAdvancements.get(player, "gifts") == 1, "the disc was not taken as a gift");
            ctx.succeed();
        }));
        add(tests, "hay_quiet", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK));
            LauraActions.gag(player, laura, player.getMainHandItem());
            ctx.check(laura.isGagged(), "hay not applied");
            LauraActions.ungag(player, laura, false);
            ctx.check(!laura.isGagged(), "hay not removed");
            ctx.succeed();
        }));
        add(tests, "skin_builtin", 200, ctx -> withLaura(ctx, laura -> {
            SkinService.requestSkin(ctx.player(), laura, "builtin:laura_summer", true);
            ctx.check(laura.getSkinRaw().equals("builtin:laura_summer"), "skin is " + laura.getSkinRaw());
            SkinService.requestSkin(ctx.player(), laura, "url:https://evil.example.com/skin.png", true);
            ctx.check(laura.getSkinRaw().equals("builtin:laura_summer"), "a non whitelisted URL was accepted");
            ctx.succeed();
        }));

        // ------------------------------------------------------------------ persistence, death
        add(tests, "save_and_load", 200, ctx -> withLaura(ctx, laura -> {
            laura.setCustomName(net.minecraft.network.chat.Component.literal("Lina"));
            laura.setAffection(777);
            laura.inventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
            CompoundTag tag = new CompoundTag();
            laura.saveWithoutId(tag);
            LauraEntity copy = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
            ctx.check(copy != null, "could not create a copy");
            copy.load(tag);
            ctx.check(copy.getLauraName().equals("Lina"), "name lost");
            ctx.check(copy.getAffection() == 777, "affection lost");
            ctx.check(copy.inventory().getItem(0).getCount() == 5, "inventory lost");
            copy.discard();
            ctx.succeed();
        }));
        add(tests, "grave_revive", 400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            laura.setInvulnerable(false);
            laura.hurt(ctx.level.damageSources().genericKill(), Float.MAX_VALUE);
            ctx.waitFor("her death", 60, () -> !laura.isAlive(), () -> {
                ctx.check(LauraManager.hasDeadCompanion(player), "no grave record");
                BlockPos grave = ctx.origin.offset(2, 0, 0);
                ctx.level.setBlockAndUpdate(grave, LauraRegistries.GRAVE.get().defaultBlockState());
                ctx.check(LauraManager.reviveAtGrave(player, grave), "revive failed");
                ctx.waitFor("her return", 100, () -> !LauraManager.findAll(player).isEmpty(), ctx::succeed);
            });
        }));

        // ------------------------------------------------------------------ storage, fetch, work
        add(tests, "inventory_access", 100, ctx -> {
            BlockPos chest = ctx.origin.offset(3, 0, 3);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            InventoryAccess access = LauraInventories.at(ctx.level, chest);
            ctx.check(access != null, "no inventory for a chest");
            ItemStack rest = access.insert(new ItemStack(Items.BREAD, 10));
            ctx.check(rest.isEmpty(), "insert failed");
            ctx.check(access.hasAnyMatching(s -> s.is(Items.BREAD)), "bread not found");
            int taken = 0;
            for (int i = 0; i < access.size(); i++) {
                taken += access.extract(i, 64).getCount();
            }
            ctx.check(taken == 10, "extracted " + taken);
            ctx.succeed();
        });
        add(tests, "fetch_from_chest", 900, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(8, 0, 0);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.APPLE, 10));
            }
            LauraActions.fetch(player, laura, "minecraft:apple", 3, false);
            ctx.waitFor("3 apples delivered", 800, () -> player.getInventory().countItem(Items.APPLE) >= 3, ctx::succeed);
        }));
        // The command takes full ids, a count and "queue" on one line.
        add(tests, "fetch_command", 900, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(8, 0, 0);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.BREAD, 10));
            }
            ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "laura fetch minecraft:bread 2");
            ctx.waitFor("2 breads delivered", 800, () -> player.getInventory().countItem(Items.BREAD) >= 2, () -> {
                ctx.check(player.getInventory().countItem(Items.BREAD) == 2, "count ignored: " + player.getInventory().countItem(Items.BREAD));
                ctx.succeed();
            });
        }));
        // A number typed in a chat order is the quantity she brings back.
        add(tests, "fetch_chat_count", 900, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(8, 0, 0);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.BREAD, 20));
            }
            LauraChat.onChat(player, "bring me 3 bread please");
            ctx.waitFor("3 breads delivered", 800, () -> player.getInventory().countItem(Items.BREAD) >= 3, () -> {
                ctx.check(player.getInventory().countItem(Items.BREAD) == 3, "count ignored: " + player.getInventory().countItem(Items.BREAD));
                ctx.succeed();
            });
        }));
        // "Come" brings her back; it never creates a second companion.
        add(tests, "come_does_not_summon", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraManager.call(player);
            ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "laura come");
            ctx.after(5, () -> {
                ctx.check(LauraWorldData.get(ctx.server()).byOwner(player.getUUID()).size() == 1, "a second companion was created");
                ctx.succeed();
            });
        }));
        add(tests, "dismiss_and_release", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            int before = laura.getAffection();
            LauraManager.dismiss(player, laura);
            resetCooldown(ctx);
            LauraManager.summon(player, false);
            ctx.waitFor("her to come back", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
                LauraEntity back = LauraManager.findAll(player).get(0);
                ctx.check(back.getAffection() == before - LauraConfig.dismissAffectionPenalty.getInt(), "dismiss penalty: " + before + " to " + back.getAffection());
                back.inventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
                // The first command only asks for confirmation.
                ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "laura release");
                ctx.check(back.isAlive(), "released without confirmation");
                ctx.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "laura release confirm");
                ctx.check(LauraWorldData.get(ctx.server()).byOwner(player.getUUID()).isEmpty(), "she is still in the world data");
                ctx.check(!ctx.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, back.getBoundingBox().inflate(3),
                        e -> e.getItem().is(Items.DIAMOND)).isEmpty(), "her belongings were not dropped");
                ctx.succeed();
            });
        }));
        add(tests, "assign_chest", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos chest = ctx.origin.offset(0, 0, 3);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(chest));
            LauraActions.perform(player, laura, LauraAction.CHEST, "WOOD", LauraActions.Source.MENU);
            ctx.check(laura.workplace().purposeOf(chest) == ChestPurpose.WOOD, "chest not assigned");
            ctx.succeed();
        }));
        add(tests, "chop_tree", 1400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos base = ctx.origin.offset(6, 0, 6);
            for (int y = 0; y < 5; y++) {
                ctx.level.setBlockAndUpdate(base.above(y), Blocks.OAK_LOG.defaultBlockState());
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = 3; y <= 5; y++) {
                        BlockPos p = base.offset(dx, y, dz);
                        if (ctx.level.getBlockState(p).isAir()) {
                            ctx.level.setBlockAndUpdate(p, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1));
                        }
                    }
                }
            }
            LauraActions.task(player, laura, LauraTask.Type.CHOP_TREE, 12, false);
            ctx.waitFor("the tree to fall", 1300, () -> !ctx.level.getBlockState(base).is(Blocks.OAK_LOG)
                    && (player.getInventory().countItem(Items.OAK_LOG) > 0 || laura.inventory().countItem(Items.OAK_LOG) > 0), ctx::succeed);
        }));
        add(tests, "harvest_field", 1200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos field = ctx.origin.offset(-6, 0, 4);
            for (int dx = 0; dx < 3; dx++) {
                BlockPos soil = field.offset(dx, -1, 0);
                ctx.level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
                ctx.level.setBlockAndUpdate(soil.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            }
            ctx.level.setBlockAndUpdate(field.offset(1, -1, 1), Blocks.WATER.defaultBlockState());
            LauraActions.task(player, laura, LauraTask.Type.HARVEST, 12, false);
            ctx.waitFor("the wheat", 1100, () -> player.getInventory().countItem(Items.WHEAT) + laura.inventory().countItem(Items.WHEAT) >= 3, ctx::succeed);
        }));
        add(tests, "cook_on_campfire", 2400, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            BlockPos fire = ctx.origin.offset(4, 0, -4);
            ctx.level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
            BlockPos chest = ctx.origin.offset(6, 0, -4);
            ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                be.setItem(0, new ItemStack(Items.BEEF, 2));
            }
            laura.workplace().assign(chest, ctx.level.dimension(), ChestPurpose.INGREDIENTS);
            LauraActions.task(player, laura, LauraTask.Type.COOK, 12, false);
            ctx.waitFor("cooked beef", 2300, () -> player.getInventory().countItem(Items.COOKED_BEEF) + laura.inventory().countItem(Items.COOKED_BEEF) > 0, ctx::succeed);
        }));
        add(tests, "chat_queue", 300, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            LauraActions.task(player, laura, LauraTask.Type.HARVEST, 5, false);
            LauraChat.onChat(player, "come here then go home");
            ctx.after(10, () -> {
                ctx.check(!laura.workplace().queued().isEmpty(), "nothing queued");
                ctx.succeed();
            });
        }));
        add(tests, "wear_backpack", 200, ctx -> withLaura(ctx, laura -> {
            ServerPlayer player = ctx.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUNDLE));
            laura.mobInteract(player, InteractionHand.MAIN_HAND);
            ctx.check(laura.getBackItem().is(Items.BUNDLE), "the bundle is not on her back");
            ctx.check(player.getMainHandItem().isEmpty(), "the bundle was not taken");
            CompoundTag tag = new CompoundTag();
            laura.saveWithoutId(tag);
            LauraEntity copy = LauraRegistries.LAURA.get().create(ctx.level, EntitySpawnReason.LOAD);
            ctx.check(copy != null, "could not create a copy");
            copy.load(tag);
            ctx.check(copy.getBackItem().is(Items.BUNDLE), "back item not saved");
            copy.discard();
            ctx.succeed();
        }));
        add(tests, "bags_overflow", 100, ctx -> withLaura(ctx, laura -> {
            for (int i = 0; i < laura.inventory().getContainerSize(); i++) {
                laura.inventory().setItem(i, new ItemStack(Items.STONE, 64));
            }
            ItemStack rest = laura.bags().add(new ItemStack(Items.DIAMOND, 3));
            ctx.check(rest.getCount() == 3, "a full bag accepted items without a backpack");
            ctx.check(!laura.bags().canAdd(new ItemStack(Items.DIAMOND)), "canAdd on a full bag");
            ctx.check(laura.bags().count(s -> s.is(Items.STONE)) == 64 * laura.inventory().getContainerSize(), "count");
            ctx.check(laura.bags().take(s -> s.is(Items.STONE), 70).stream().mapToInt(ItemStack::getCount).sum() == 70, "take");
            ctx.succeed();
        }));
        if (LauraMod.platform().isModLoaded("farmersdelight")) {
            add(tests, "compat_farmers_delight_pot", 3000, ctx -> withLaura(ctx, laura -> {
                ServerPlayer player = ctx.player();
                CookingPots pots = LauraMod.platform().cookingPots();
                CookingPots.Recipe recipe = null;
                for (CookingPots.Recipe r : pots.recipes(ctx.level)) {
                    boolean simple = true;
                    for (net.minecraft.world.item.crafting.Ingredient i : r.ingredients()) {
                        simple &= i.items().findAny().isPresent();
                    }
                    if (simple && r.ingredients().size() <= 3) {
                        recipe = r;
                        break;
                    }
                }
                ctx.check(recipe != null, "no Farmer's Delight pot recipe found");
                BlockPos fire = ctx.origin.offset(4, 0, -4);
                ctx.level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
                ctx.level.setBlockAndUpdate(fire.above(), BuiltInRegistries.BLOCK.getValue(Identifier.parse("farmersdelight:cooking_pot")).defaultBlockState());
                ctx.check(pots.isPot(ctx.level, fire.above()), "the pot is not recognized");
                BlockPos chest = ctx.origin.offset(6, 0, -4);
                ctx.level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
                if (ctx.level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
                    int slot = 0;
                    for (net.minecraft.world.item.crafting.Ingredient i : recipe.ingredients()) {
                        be.setItem(slot++, new ItemStack(i.items().findFirst().orElseThrow()));
                    }
                    if (!recipe.container().isEmpty()) {
                        be.setItem(slot, recipe.container().copyWithCount(4));
                    }
                }
                laura.workplace().assign(chest, ctx.level.dimension(), ChestPurpose.INGREDIENTS);
                ItemStack result = recipe.result();
                LauraActions.task(player, laura, LauraTask.Type.COOK, 12, false);
                ctx.waitFor(result.getHoverName().getString() + " from the pot", 2900,
                        () -> player.getInventory().countItem(result.getItem()) + laura.bags().count(s -> s.is(result.getItem())) > 0, ctx::succeed);
            }));
        }
        if (LauraMod.platform().isModLoaded("kubejs")) {
            // Needs run-selftest/kubejs/server_scripts/laura_selftest.js.
            add(tests, "compat_kubejs", 400, ctx -> {
                ctx.check(com.vyrriox.lauramod.gift.GiftTable.find(new ItemStack(Items.EMERALD_BLOCK)) != null, "script gift missing");
                ctx.check(com.vyrriox.lauramod.gift.GiftTable.isFavoriteFood(new ItemStack(Items.BAKED_POTATO)), "script favorite food missing");
                ctx.check(intent("kubejs test phrase").equals("kubejs_test"), "script chat trigger missing");
                ctx.check(com.vyrriox.lauramod.cooking.Cookbook.meals().stream().anyMatch(m -> m.id().equals("kubejs_bread")), "script meal missing");
                withLaura(ctx, laura -> {
                    ctx.check(laura.getAffection() >= LauraConfig.startAffection.getInt() + 77, "summon event did not run: " + laura.getAffection());
                    LauraChat.onChat(ctx.player(), "kubejs test phrase");
                    ctx.waitFor("the scripted emote", 40, () -> laura.getEmote() == Emote.CELEBRATE, ctx::succeed);
                });
            });
        }
        add(tests, "advancement_counters", 100, ctx -> {
            ServerPlayer player = ctx.player();
            LauraAdvancements.add(player, "hugs", 1);
            ctx.check(LauraAdvancements.get(player, "hugs") == 1, "counter not saved");
            ctx.check(LauraWorldData.get(ctx.server()).meta(player.getUUID()).stats.get("hugs") == 1, "world data");
            ctx.succeed();
        });
    }

    private static String intent(String message) {
        DialogueManager.IntentMatch m = DialogueManager.match(message);
        return m == null ? "" : m.intent();
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
            body.accept(laura);
        });
    }
}
