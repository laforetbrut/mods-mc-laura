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

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "laura_" + name + ".png", mc.getMainRenderTarget(),
                message -> LauraMod.LOGGER.info("[CLIENTTEST] {}", message.getString()));
    }

    private static void plan(Minecraft mc) {
        STEPS.add(() -> onServer(mc, player -> {
            player.serverLevel().setDayTime(6000);
            player.serverLevel().setWeatherParameters(6000, 0, false, false);
            LauraManager.summon(player, false);
        }));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                LauraEntity l = all.get(0);
                Vec3 look = player.getLookAngle();
                l.teleportTo(player.getX() + look.x * 3, player.getY(), player.getZ() + look.z * 3);
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
        // A long line to check the speech bubble: wrapping, width and readability.
        STEPS.add(() -> {
            LauraEntity l = laura();
            if (l != null) {
                ClientState.addBubble(l.getId(), net.minecraft.network.chat.Component.literal(
                        "Je peux reparler ! Et j'ai beaucoup de choses à dire sur cette longue journée passée ensemble."), 20);
            }
        });
        STEPS.add(() -> shot(mc, "world_speech"));
        STEPS.add(() -> onServer(mc, player -> player.serverLevel().setDayTime(18000)));
        STEPS.add(() -> shot(mc, "world_speech_night"));
        STEPS.add(() -> onServer(mc, player -> player.serverLevel().setDayTime(6000)));
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
                all.get(0).setYRot(player.getYRot() + 180);
                all.get(0).setYBodyRot(player.getYRot() + 180);
                all.get(0).setYHeadRot(player.getYRot() + 180);
            }
        }));
        STEPS.add(() -> shot(mc, "world_hay"));
        STEPS.add(() -> onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (!all.isEmpty()) {
                all.get(0).setYRot(player.getYRot());
                all.get(0).setYBodyRot(player.getYRot());
                all.get(0).setYHeadRot(player.getYRot());
            }
        }));
        STEPS.add(() -> shot(mc, "world_back"));
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
