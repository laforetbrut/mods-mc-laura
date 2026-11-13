package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.client.ui.LauraTheme;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.network.LauraAction;
import dev.dryaides.minuit.BaseComponent;
import dev.dryaides.minuit.Box;
import dev.dryaides.minuit.Draw;
import dev.dryaides.minuit.PanelComponent;
import dev.dryaides.minuit.RadialComponent;
import dev.dryaides.minuit.client.MinuitScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A radial emote picker in the spirit of the Emotecraft wheel: hold the emote key, point at an
 * emote and let go (or click it). The mouse wheel turns the pages.
 *
 * @author vyrriox
 */
public class EmoteWheelScreen extends MinuitScreen {
    private static final int PER_PAGE = 10;
    private static final int RADIUS = 72;
    private static int page;
    private final LauraEntity laura;
    private final List<Emote> emotes = new ArrayList<>();
    private final long openedAt = Util.getMillis();
    private RadialComponent<Emote> wheel;

    public EmoteWheelScreen(LauraEntity laura) {
        super("lauramod.wheel.title");
        this.laura = laura;
        for (Emote e : Emote.values()) {
            if (e.playerTriggered) {
                emotes.add(e);
            }
        }
    }

    @Override
    protected boolean showsThemeTab() {
        return false;
    }

    @Override
    protected PanelComponent build(int screenWidth, int screenHeight) {
        RadialComponent.Builder<Emote> builder = RadialComponent.<Emote>builder(new Box("wheel", 0, 0, screenWidth, screenHeight), RADIUS)
                .clickToSelect()
                .icon(e -> KawaiiIcons.of("EMOTE_" + e.name()))
                .label(e -> words().get("lauramod.emote." + e.animationName()))
                .centerLabel(laura::getLauraName)
                .startPage(page)
                .onPage(p -> page = p)
                .onPick(this::play)
                .onCancel(this::onClose)
                // A light veil, so she stays visible while picking.
                .dim(false);
        int pages = Math.max(1, (emotes.size() + PER_PAGE - 1) / PER_PAGE);
        for (int p = 0; p < pages; p++) {
            int from = p * PER_PAGE;
            builder.page((p + 1) + "/" + pages, emotes.subList(from, Math.min(emotes.size(), from + PER_PAGE)));
        }
        wheel = builder.build();
        String hint = words().get("lauramod.wheel.hint");
        return new PanelComponent(LauraTheme.get(), words(), 0, 0) {
            {
                add(wheel);
            }

            @Override
            public int width() {
                return screenWidth;
            }

            @Override
            public int height() {
                return screenHeight;
            }

            @Override
            protected void drawBackground(Draw d, int mx, int my) {
                d.gradient(0, 0, screenWidth, screenHeight, 0x40301020, 0x40301020, 0x60301020, 0x60301020);
            }

            @Override
            protected void drawForeground(Draw d, int mx, int my) {
                d.push(screenWidth / 2F, screenHeight / 2F + RADIUS + 30, 0.75F);
                d.centered(hint, 0, 0, 0xFFFFFFFF);
                d.pop();
            }
        };
    }

    private void play(Emote emote) {
        LauraClientNetwork.action(laura.getId(), LauraAction.EMOTE, emote.name());
        BaseComponent.click();
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (wheel != null && (key == 262 || key == 68)) {
            // Right arrow or D.
            wheel.turnPage(1);
            return true;
        }
        if (wheel != null && (key == 263 || key == 65)) {
            // Left arrow or A.
            wheel.turnPage(-1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        // Hold and release: a quick tap keeps the wheel open for clicking.
        if (LauraClient.EMOTE_KEY.matches(event) && Util.getMillis() - openedAt > 250) {
            if (wheel != null) {
                wheel.release();
            } else {
                onClose();
            }
            return true;
        }
        return super.keyReleased(event);
    }
}
