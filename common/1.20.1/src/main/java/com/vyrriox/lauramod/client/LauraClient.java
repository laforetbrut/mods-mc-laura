package com.vyrriox.lauramod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.anim.DefaultAnimations;
import com.vyrriox.lauramod.client.gui.EmoteWheelScreen;
import com.vyrriox.lauramod.client.gui.LauraMenuScreen;
import com.vyrriox.lauramod.client.lang.LanguageOverlay;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.desire.DesireTable;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.platform.ClientBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * Client entry point shared by every loader.
 *
 * @author vyrriox
 */
public final class LauraClient {
    public static final String KEY_CATEGORY = "key.categories.lauramod";
    public static final KeyMapping MENU_KEY = new KeyMapping("key.lauramod.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY);
    public static final KeyMapping EMOTE_KEY = new KeyMapping("key.lauramod.emotes", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY);
    public static final KeyMapping CALL_KEY = new KeyMapping("key.lauramod.call", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    private static boolean joined;

    private LauraClient() {
    }

    public static List<KeyMapping> keyMappings() {
        return List.of(MENU_KEY, EMOTE_KEY, CALL_KEY);
    }

    /** Called once by the loader during client setup. */
    public static void init() {
        LauraClientConfig.load();
        DefaultAnimations.reload();
        LauraMod.setClientBridge(new ClientBridge() {
            @Override
            public void openInteractionScreen(LauraEntity laura) {
                Minecraft mc = Minecraft.getInstance();
                if (!LauraClientConfig.menuOnRightClick.get() || mc.player == null) {
                    return;
                }
                if (!laura.isOwnedBy(mc.player) && !ClientState.othersCanInteract) {
                    return;
                }
                mc.execute(() -> mc.setScreen(new LauraMenuScreen(laura)));
            }

            @Override
            public boolean isScreenOpen() {
                return Minecraft.getInstance().screen instanceof LauraMenuScreen;
            }

            @Override
            public void handlePacket(byte[] data) {
                LauraClientNetwork.handle(data);
            }
        });
    }

    /** Called after the registries are ready (item icons in desires need them). */
    public static void afterRegistries() {
        DesireTable.load();
    }

    /** Called every client tick (end phase). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientState.ticks++;
        ClientSelfTest.tick();
        if (mc.player == null || mc.level == null) {
            if (joined) {
                joined = false;
                onDisconnect();
            }
            return;
        }
        if (!joined) {
            joined = true;
            LauraClientNetwork.hello();
        }
        LanguageOverlay.ensureInstalled();
        while (MENU_KEY.consumeClick()) {
            LauraEntity laura = nearestOwned(64);
            if (laura != null) {
                mc.setScreen(new LauraMenuScreen(laura));
            } else {
                mc.player.displayClientMessage(Component.translatable("lauramod.menu.none_near"), true);
            }
        }
        while (EMOTE_KEY.consumeClick()) {
            LauraEntity laura = nearestOwned(32);
            if (laura != null && mc.screen == null) {
                mc.setScreen(new EmoteWheelScreen(laura));
            }
        }
        while (CALL_KEY.consumeClick()) {
            mc.player.connection.sendCommand("laura come");
        }
    }

    /** Owned companions near the local player, nearest first. */
    public static List<LauraEntity> ownedNearby(double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return List.of();
        }
        return mc.level.getEntitiesOfClass(LauraEntity.class, mc.player.getBoundingBox().inflate(range),
                        l -> l.isAlive() && (l.isOwnedBy(mc.player) || ClientState.othersCanInteract))
                .stream().sorted(Comparator.comparingDouble(l -> l.distanceToSqr(mc.player))).toList();
    }

    public static LauraEntity nearestOwned(double range) {
        List<LauraEntity> list = ownedNearby(range);
        return list.isEmpty() ? null : list.get(0);
    }

    private static void onDisconnect() {
        ClientState.reset();
        LauraClientNetwork.reset();
        SkinTextures.clear();
        ClientModels.clear();
    }

    /** Called after a client resource reload. */
    public static void onResourceReload() {
        DefaultAnimations.reload();
        ClientModels.clear();
        LanguageOverlay.reload();
    }
}
