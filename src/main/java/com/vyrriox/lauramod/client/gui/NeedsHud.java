package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.gui.kawaii.KDraw;
import com.vyrriox.lauramod.client.gui.kawaii.Kawaii;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The optional overlay in the top left corner: face, name, health and needs of the nearest owned
 * companion. A need under 20% blinks so it is noticed during a fight or a long build.
 *
 * @author vyrriox
 */
public final class NeedsHud {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int W = 88;
    private static final int RANGE = 32;
    private static final int[] COLORS = {Kawaii.PEACH_DEEP, Kawaii.LAVENDER_DEEP, Kawaii.MINT_DEEP, Kawaii.PINK_DEEP, Kawaii.SKY_DEEP};

    private NeedsHud() {
    }

    /** Called by the loader's HUD hook every frame. */
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!LauraClientConfig.showNeedsHud.get() || mc.player == null || mc.options.hideGui
                || mc.getDebugOverlay().showDebugScreen() || mc.screen instanceof LauraMenuScreen) {
            return;
        }
        LauraEntity laura = nearestOwnedOnly();
        if (laura == null) {
            return;
        }
        KDraw d = new KDraw(graphics);
        Needs.Need[] needs = Needs.Need.values();
        boolean showNeeds = ClientState.needsEnabled;
        int h = 28 + (showNeeds ? needs.length * 9 : 0);
        Kawaii.rounded(d, X + 1, Y + 2, W, h, 4, 0x30A0406E);
        Kawaii.rounded(d, X, Y, W, h, 4, 0xD8FFF7FB);

        d.face(SkinTextures.get(laura.getSkin()), X + 4, Y + 4, 12);
        Component name = d.ellipsize(Component.literal(laura.getLauraName()), W - 24);
        d.text(name, X + 20, Y + 6, Kawaii.PLUM);

        boolean blink = (ClientState.ticks / 8) % 2 == 0;
        float health = laura.getHealth() / Math.max(1, laura.getMaxHealth());
        Kawaii.heart(d, X + 4, Y + 19, 1, health < 0.25F && blink ? Kawaii.ROSE : Kawaii.PINK_DEEP);
        Kawaii.bar(d, X + 14, Y + 19, W - 18, 6, health, 0xFFFF6B8A, 0x30A0406E);
        if (!showNeeds) {
            return;
        }
        long packed = laura.getPackedNeeds();
        for (Needs.Need need : needs) {
            int v = Needs.unpack(packed, need);
            int ny = Y + 28 + need.ordinal() * 9;
            d.icon(Icon.need(need), X + 4, ny - 1, 8);
            int color = Kawaii.needColor(v, COLORS[need.ordinal()]);
            if (v < 20 && blink) {
                color = Kawaii.WHITE;
            }
            Kawaii.bar(d, X + 14, ny, W - 18, 6, v / 100F, color, 0x30A0406E);
        }
    }

    /** Only the player's own companions, even when the server lets others interact with them. */
    private static LauraEntity nearestOwnedOnly() {
        Minecraft mc = Minecraft.getInstance();
        for (LauraEntity laura : LauraClient.ownedNearby(RANGE)) {
            if (laura.isOwnedBy(mc.player)) {
                return laura;
            }
        }
        return null;
    }
}
