package com.vyrriox.lauramod.client;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.gui.EmoteWheelScreen;
import com.vyrriox.lauramod.client.gui.LauraMenuScreen;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual check of the client: with {@code -Dlauramod.clienttest=true} and a quick play world, it
 * summons a companion, dresses her up, opens every screen of the mod and saves a screenshot of each
 * in {@code screenshots/}, then quits. Development only.
 *
 * @author vyrriox
 */
public final class ClientSelfTest {
    private static final boolean ENABLED = Boolean.getBoolean("lauramod.clienttest");
    private static final List<Runnable> STEPS = new ArrayList<>();
    private static int wait = -1;
    private static int step;

    private ClientSelfTest() {
    }

    public static void tick() {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (STEPS.isEmpty()) {
            // Other windows can take the focus on a busy machine: the game must not pause under the
            // test (in memory only, like the HUD setting below).
            mc.options.pauseOnLostFocus = false;
            if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
                mc.setScreen(null);
            }
            plan(mc);
            wait = 100;
        }
        if (--wait > 0) {
            return;
        }
        if (step >= STEPS.size()) {
            return;
        }
        try {
            STEPS.get(step++).run();
        } catch (Throwable t) {
            LauraMod.LOGGER.error("[CLIENTTEST] step {} failed", step, t);
        }
        wait = 25;
    }

    private static void onServer(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                action.accept(player);
            }
        });
    }

    private static LauraEntity laura() {
        return LauraClient.nearestOwned(32);
    }

    private static void wave(Minecraft mc) {
        onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                com.vyrriox.lauramod.world.LauraActions.perform(player, all.get(0), com.vyrriox.lauramod.network.LauraAction.EMOTE, "WAVE",
                        com.vyrriox.lauramod.world.LauraActions.Source.MENU);
            }
        });
    }

    /** Day time is a world clock now: sets the overworld one. */
    private static void setDayTime(ServerPlayer player, long time) {
        player.level().registryAccess().get(net.minecraft.world.clock.WorldClocks.OVERWORLD)
                .ifPresent(clock -> player.level().clockManager().setTotalTicks(clock, time));
    }

    /**
     * Removes every companion of the test world, whoever owns her: the development player of some
     * loaders gets another name and UUID on each run.
     */
    private static void removeAllCompanions(net.minecraft.server.MinecraftServer server) {
        for (java.util.UUID owner : com.vyrriox.lauramod.world.LauraWorldData.get(server).all().stream().map(r -> r.owner).distinct().toList()) {
            com.vyrriox.lauramod.test.MockPlayers.forgetCompanions(server, owner);
        }
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            List<LauraEntity> left = new ArrayList<>();
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof LauraEntity laura) {
                    left.add(laura);
                }
            }
            left.forEach(LauraEntity::discard);
        }
    }

    /** Puts her back 3 blocks in front of the player, facing them: she may have walked off since the last shot. */
    private static void inFront(ServerPlayer player, LauraEntity laura) {
        Vec3 look = player.getLookAngle();
        laura.teleportTo(player.getX() + look.x * 3, player.getY(), player.getZ() + look.z * 3);
        laura.getNavigation().stop();
        laura.setYRot(player.getYRot() + 180);
        laura.setYBodyRot(player.getYRot() + 180);
        laura.setYHeadRot(player.getYRot() + 180);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "laura_" + name + ".png", mc.getMainRenderTarget(), 1,
                message -> LauraMod.LOGGER.info("[CLIENTTEST] {}", message.getString()));
    }

    private static void plan(Minecraft mc) {
        STEPS.add(() -> onServer(mc, player -> {
            // The quick play world is kept between runs: start from one new companion, whatever
            // earlier runs left in it (several companions on top of each other, one still gagged),
            // and without monsters, which hurt or killed the player during the night shots.
            removeAllCompanions(player.level().getServer());
            player.level().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            setDayTime(player, 6000);
            player.level().getServer().setWeatherParameters(6000, 0, false, false);
            LauraManager.summon(player, false);
        }));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                LauraEntity l = all.get(0);
                inFront(player, l);
                l.setBackItem(new ItemStack(Items.BUNDLE));
                l.brain().needs().set(com.vyrriox.lauramod.entity.brain.Needs.Need.HUNGER, 35);
            }
        }));
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, l.getEyePosition());
            }
        });
        // In memory only: the user's config file is not rewritten by the test.
        STEPS.add(() -> com.vyrriox.lauramod.config.LauraClientConfig.showNeedsHud.set(true));
        STEPS.add(() -> shot(mc, "world_front"));
        STEPS.add(() -> onServer(mc, player -> LauraManager.findAll(player).stream().findFirst().ifPresent(l -> inFront(player, l))));
        // A long line to check the speech bubble: wrapping, width and readability.
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                ClientState.addBubble(l.getId(), net.minecraft.network.chat.Component.literal(
                        "Je peux reparler ! Et j'ai beaucoup de choses à dire sur cette longue journée passée ensemble."), 20);
            }
        });
        STEPS.add(() -> shot(mc, "world_speech"));
        STEPS.add(() -> onServer(mc, player -> {
            setDayTime(player, 18000);
            LauraManager.findAll(player).stream().findFirst().ifPresent(l -> inFront(player, l));
        }));
        STEPS.add(() -> shot(mc, "world_speech_night"));
        STEPS.add(() -> onServer(mc, player -> setDayTime(player, 6000)));
        for (LauraMenuScreen.Tab tab : LauraMenuScreen.Tab.values()) {
            STEPS.add(() -> {
                LauraEntity l = laura();
                if (l != null) {
                    mc.setScreen(new LauraMenuScreen(l, tab));
                }
            });
            STEPS.add(() -> shot(mc, "menu_" + tab.name().toLowerCase(java.util.Locale.ROOT)));
        }
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                mc.setScreen(new EmoteWheelScreen(l));
            }
        });
        STEPS.add(() -> shot(mc, "emote_wheel"));
        STEPS.add(() -> mc.setScreen(null));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK));
                com.vyrriox.lauramod.world.LauraActions.gag(player, all.get(0), player.getMainHandItem());
                inFront(player, all.get(0));
            }
        }));
        STEPS.add(() -> shot(mc, "world_hay"));
        // The wave on the default model, as the reference for the custom model below.
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                com.vyrriox.lauramod.world.LauraActions.ungag(player, all.get(0), false);
            }
        }));
        STEPS.add(() -> wave(mc));
        STEPS.add(() -> shot(mc, "default_wave"));
        // Custom model check: config/lauramod/models/test_fr.bbmodel, when present, has French bone
        // names, a nose on its front, a blue right arm and no animation. It shows the borrowed
        // default animations (the same wave, on "bras_d") and the bone lookup (hay on "tete", bag
        // on "corps").
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                com.vyrriox.lauramod.client.network.LauraClientNetwork.setModel(l.getId(), "server:test_fr");
            }
        });
        STEPS.add(() -> {
        });
        STEPS.add(() -> wave(mc));
        STEPS.add(() -> shot(mc, "model_wave"));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK));
                com.vyrriox.lauramod.world.LauraActions.gag(player, all.get(0), player.getMainHandItem());
            }
        }));
        STEPS.add(() -> shot(mc, "model_hay"));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                all.get(0).setYRot(player.getYRot());
                all.get(0).setYBodyRot(player.getYRot());
                all.get(0).setYHeadRot(player.getYRot());
            }
        }));
        STEPS.add(() -> shot(mc, "world_back"));
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                com.vyrriox.lauramod.client.network.LauraClientNetwork.setModel(l.getId(), "reset");
            }
        });
        STEPS.add(() -> shot(mc, "world_back_default"));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                all.get(0).openInventory(player);
            }
        }));
        STEPS.add(() -> shot(mc, "inventory"));
        STEPS.add(() -> {
            LauraMod.LOGGER.info("[CLIENTTEST] done");
            mc.stop();
        });
    }
}
