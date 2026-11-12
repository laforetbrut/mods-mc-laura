package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.ui.LauraTheme;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.brain.Needs;
import dev.dryaides.minuit.FillColor;
import dev.dryaides.minuit.HudAnchor;
import dev.dryaides.minuit.HudBar;
import dev.dryaides.minuit.HudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

/**
 * The optional overlay in the top left corner: health and needs of the nearest owned companion,
 * as minuit HUD bars in the mod's theme. minuit draws them; this class only feeds them.
 *
 * @author vyrriox
 */
public final class NeedsHud {
    private static final int W = 88;
    private static final int RANGE = 32;
    /** Base colour of each need bar, in the order of Needs.Need. */
    private static final int[] COLORS = {0xFFFFB38A, 0xFFB39DFF, 0xFF7EE0C3, 0xFFF77FB2, 0xFF8FD3FF};

    private static HudBar health;
    private static HudBar[] needs;
    private static LauraEntity shown;

    private NeedsHud() {
    }

    /** Called by the loader's HUD hook every frame: updates the bars minuit draws. */
    public static void render(GuiGraphicsExtractor graphics) {
        if (health == null) {
            register();
        }
        Minecraft mc = Minecraft.getInstance();
        boolean visible = LauraClientConfig.showNeedsHud.get() && mc.player != null && !mc.options.hideGui
                && !mc.getDebugOverlay().showDebugScreen() && !(mc.screen instanceof LauraMenuScreen);
        shown = visible ? nearestOwnedOnly() : null;
        if (shown == null) {
            return;
        }
        float hp = shown.getHealth() / Math.max(1, shown.getMaxHealth());
        health.label(shown.getLauraName()).value(hp).text(Math.round(shown.getHealth()) + "/" + Math.round(shown.getMaxHealth()));
        long packed = shown.getPackedNeeds();
        for (Needs.Need need : Needs.Need.values()) {
            int v = Needs.unpack(packed, need);
            needs[need.ordinal()].value(v / 100F).text(v + "%");
        }
    }

    private static void register() {
        LauraTheme.register();
        health = HudLayer.main().add(new HudBar(LauraTheme.ID, "health", HudAnchor.TOP_LEFT, W)
                .icon(KawaiiIcons.of(Icon.HEALTH)).fillColor((f, p) -> p.bad()));
        health.visibleWhen(() -> shown != null);
        Needs.Need[] all = Needs.Need.values();
        needs = new HudBar[all.length];
        for (Needs.Need need : all) {
            int base = COLORS[need.ordinal()];
            // Under 20 the bar takes the theme's bad colour, under 45 its warning colour.
            FillColor color = (f, p) -> f < 0.20F ? p.bad() : f < 0.45F ? p.warn() : base;
            HudBar bar = HudLayer.main().add(new HudBar(LauraTheme.ID, "need_" + need.key(), HudAnchor.TOP_LEFT, W)
                    .label(I18n.get("lauramod.need." + need.key())).icon(KawaiiIcons.of(Icon.need(need))).fillColor(color));
            bar.visibleWhen(() -> shown != null && ClientState.needsEnabled);
            needs[need.ordinal()] = bar;
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
