package com.vyrriox.lauramod.client.gui.kawaii;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A cute on/off switch with a label.
 *
 * @author vyrriox
 */
public class KToggle extends KWidget {
    private final Component label;
    private final Consumer<Boolean> onChange;
    public boolean value;
    private float knob;

    public KToggle(int x, int y, int w, Component label, boolean value, Consumer<Boolean> onChange) {
        super(x, y, w, 14);
        this.label = label;
        this.value = value;
        this.onChange = onChange;
        this.knob = value ? 1 : 0;
    }

    @Override
    protected void render(KDraw d, int mx, int my, boolean hovered) {
        knob += ((value ? 1 : 0) - knob) * 0.35F;
        int sw = 24;
        int sx = x + w - sw;
        int track = value ? Kawaii.mix(Kawaii.PINK, Kawaii.PINK_DEEP, 0.4F) : Kawaii.mix(Kawaii.DISABLED, 0xFFD8C4D0, hoverAnim);
        Kawaii.rounded(d, sx, y + 2, sw, 10, 5, track);
        int kx = sx + 1 + Math.round(knob * (sw - 10));
        Kawaii.rounded(d, kx, y + 1, 10, 12, 5, 0x33A0406E);
        Kawaii.rounded(d, kx, y, 10, 12, 5, Kawaii.WHITE);
        if (value) {
            Kawaii.heart(d, kx + 2, y + 3, 1, Kawaii.PINK_DEEP);
        }
        int color = active ? Kawaii.PLUM : Kawaii.DISABLED_TEXT;
        int room = w - sw - 4;
        int textWidth = d.width(label);
        if (textWidth <= room) {
            d.text(label, x, y + 3, color);
            return;
        }
        // Long translations shrink a little, then get cut, so they never run under the switch.
        float scale = Math.max(0.6F, room / (float) textWidth);
        Component text = scale > 0.6F ? label : d.ellipsize(label, (int) (room / scale));
        d.push(x, y + (14 - 8 * scale) / 2F, scale);
        d.text(text, 0, 0, color);
        d.pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!active || !isOver(mx, my) || button != 0) {
            return false;
        }
        click();
        value = !value;
        onChange.accept(value);
        return true;
    }
}
