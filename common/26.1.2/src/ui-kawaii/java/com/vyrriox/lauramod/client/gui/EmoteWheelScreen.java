package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.gui.kawaii.KDraw;
import com.vyrriox.lauramod.client.gui.kawaii.KWidget;
import com.vyrriox.lauramod.client.gui.kawaii.Kawaii;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.network.LauraAction;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A radial emote picker in the spirit of the Emotecraft wheel: hold the emote key, point at an
 * emote and let go (or click it). The mouse wheel turns the pages.
 *
 * @author vyrriox
 */
public class EmoteWheelScreen extends Screen {
    private static final int PER_PAGE = 10;
    private static final int RADIUS = 72;
    private static int page;

    private final LauraEntity laura;
    private final List<Emote> emotes = new ArrayList<>();
    private final long openedAt = Util.getMillis();
    private int hovered = -1;
    private final float[] grow = new float[PER_PAGE];
    private long lastFrame = Util.getMillis();

    public EmoteWheelScreen(LauraEntity laura) {
        super(Component.translatable("lauramod.wheel.title"));
        this.laura = laura;
        for (Emote e : Emote.values()) {
            if (e.playerTriggered) {
                emotes.add(e);
            }
        }
        page = Math.min(page, pages() - 1);
    }

    private int pages() {
        return Math.max(1, (emotes.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private List<Emote> pageEmotes() {
        int from = page * PER_PAGE;
        return emotes.subList(from, Math.min(emotes.size(), from + PER_PAGE));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // A light veil instead of the blur, so she stays visible while picking.
        graphics.fillGradient(0, 0, width, height, 0x40301020, 0x60301020);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        float delta = Math.min(5F, (now - lastFrame) / 50F);
        lastFrame = now;
        KDraw d = new KDraw(graphics);
        int cx = width / 2;
        int cy = height / 2;
        List<Emote> list = pageEmotes();
        hovered = sectorAt(mouseX, mouseY, list.size());

        // Ring of little dots.
        for (int i = 0; i < 60; i++) {
            double a = i * Math.PI * 2 / 60;
            int px = cx + (int) Math.round(Math.cos(a) * RADIUS);
            int py = cy + (int) Math.round(Math.sin(a) * RADIUS);
            d.fill(px, py, px + 1, py + 1, 0x80FFFFFF);
        }
        // Center bubble.
        Kawaii.rounded(d, cx - 34, cy - 33, 68, 68, 34, 0x40A0406E);
        Kawaii.rounded(d, cx - 34, cy - 34, 68, 68, 34, Kawaii.CREAM);
        Kawaii.rounded(d, cx - 31, cy - 31, 62, 62, 31, Kawaii.BLUSH);
        if (hovered >= 0) {
            Emote e = list.get(hovered);
            d.icon(Icon.emote(e), cx - 12, cy - 20, 24);
            Component name = Component.translatable("lauramod.emote." + e.animationName());
            float scale = Math.min(1F, 56F / Math.max(1, d.width(name)));
            d.push(cx, cy + 8, scale);
            d.centered(name, 0, 0, Kawaii.PLUM);
            d.pop();
        } else {
            Kawaii.heart(d, cx - 7, cy - 12, 2, Kawaii.PINK_DEEP);
            Component name = Component.literal(laura.getLauraName());
            float scale = Math.min(1F, 56F / Math.max(1, d.width(name)));
            d.push(cx, cy + 6, scale);
            d.centered(name, 0, 0, Kawaii.PLUM);
            d.pop();
        }
        // Page dots.
        int pages = pages();
        for (int p = 0; p < pages; p++) {
            int dx = cx - (pages * 8) / 2 + p * 8 + 2;
            Kawaii.rounded(d, dx, cy + 20, 4, 4, 2, p == page ? Kawaii.ROSE : 0x80A0406E);
        }
        // Emote bubbles.
        for (int i = 0; i < list.size(); i++) {
            grow[i] += ((i == hovered ? 1 : 0) - grow[i]) * Math.min(1F, 0.4F * delta);
            double a = angleOf(i, list.size());
            int size = 28 + Math.round(grow[i] * 8);
            int bx = cx + (int) Math.round(Math.cos(a) * RADIUS) - size / 2;
            int by = cy + (int) Math.round(Math.sin(a) * RADIUS) - size / 2;
            Kawaii.rounded(d, bx, by + 2, size, size, size / 2, 0x40A0406E);
            Kawaii.rounded(d, bx, by, size, size, size / 2, i == hovered ? Kawaii.PINK_DEEP : i % 2 == 0 ? Kawaii.PINK : Kawaii.LAVENDER);
            Kawaii.rounded(d, bx + 2, by + 2, size - 4, size - 4, size / 2 - 2, Kawaii.mix(Kawaii.CREAM, Kawaii.PINK, grow[i] * 0.5F));
            d.icon(Icon.emote(list.get(i)), bx + (size - 16) / 2, by + (size - 16) / 2);
        }
        Component hint = Component.translatable("lauramod.wheel.hint");
        d.push(cx, cy + RADIUS + 30, 0.75F);
        d.centered(hint, 0, 0, 0xFFFFFFFF);
        d.pop();
    }

    private static double angleOf(int index, int count) {
        return -Math.PI / 2 + index * Math.PI * 2 / Math.max(1, count);
    }

    private int sectorAt(double mx, double my, int count) {
        double dx = mx - width / 2.0;
        double dy = my - height / 2.0;
        if (count == 0 || dx * dx + dy * dy < 30 * 30) {
            return -1;
        }
        double a = Math.atan2(dy, dx) + Math.PI / 2;
        double step = Math.PI * 2 / count;
        int index = (int) Math.round(a / step);
        return Math.floorMod(index, count);
    }

    private void play(int index) {
        List<Emote> list = pageEmotes();
        if (index < 0 || index >= list.size()) {
            return;
        }
        LauraClientNetwork.action(laura.getId(), LauraAction.EMOTE, list.get(index).name());
        KWidget.pop();
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            onClose();
            return true;
        }
        int index = sectorAt(event.x(), event.y(), pageEmotes().size());
        if (index >= 0) {
            play(index);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0) {
            page = Math.floorMod(page + (scrollY < 0 ? 1 : -1), pages());
            KWidget.click();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 262 || key == 68) {
            // Right arrow or D.
            page = Math.floorMod(page + 1, pages());
            return true;
        }
        if (key == 263 || key == 65) {
            // Left arrow or A.
            page = Math.floorMod(page - 1, pages());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        // Hold and release: a quick tap keeps the wheel open for clicking.
        if (LauraClient.EMOTE_KEY.matches(event) && Util.getMillis() - openedAt > 250) {
            if (hovered >= 0) {
                play(hovered);
            } else {
                onClose();
            }
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
