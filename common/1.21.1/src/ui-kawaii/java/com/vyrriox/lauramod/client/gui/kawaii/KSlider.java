package com.vyrriox.lauramod.client.gui.kawaii;

import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * An integer slider with a heart knob.
 *
 * @author vyrriox
 */
public class KSlider extends KWidget {
    private final int min;
    private final int max;
    private final Function<Integer, Component> label;
    private final IntConsumer onChange;
    public int value;
    private boolean dragging;

    public KSlider(int x, int y, int w, int min, int max, int value, Function<Integer, Component> label, IntConsumer onChange) {
        super(x, y, w, 20);
        this.min = min;
        this.max = max;
        this.value = value;
        this.label = label;
        this.onChange = onChange;
    }

    @Override
    protected void render(KDraw d, int mx, int my, boolean hovered) {
        d.textFit(label.apply(value), x, y, w, Kawaii.PLUM);
        int ty = y + 12;
        Kawaii.rounded(d, x, ty, w, 6, 3, Kawaii.DISABLED);
        float t = max == min ? 0 : (float) (value - min) / (max - min);
        int filled = Math.round(t * w);
        Kawaii.rounded(d, x, ty, Math.max(6, filled), 6, 3, Kawaii.PINK);
        Kawaii.heart(d, x + filled - 7, ty - 3, 2, hovered || dragging ? Kawaii.ROSE : Kawaii.PINK_DEEP);
    }

    private void update(double mx) {
        float t = (float) Math.max(0, Math.min(1, (mx - x) / w));
        int v = min + Math.round(t * (max - min));
        if (v != value) {
            value = v;
            onChange.accept(v);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!active || !isOver(mx, my) || button != 0) {
            return false;
        }
        dragging = true;
        update(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            update(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }
}
