package com.vyrriox.lauramod.client.gui.kawaii;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Base of the kawaii widgets: position, hover state, tooltip.
 *
 * @author vyrriox
 */
public abstract class KWidget {
    public int x;
    public int y;
    public int w;
    public int h;
    public boolean visible = true;
    public boolean active = true;
    public List<Component> tooltip;
    protected float hoverAnim;

    protected KWidget(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean isOver(double mx, double my) {
        return visible && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public KWidget tooltip(Component... lines) {
        this.tooltip = List.of(lines);
        return this;
    }

    /** Draws the widget; {@code delta} is the frame time in ticks, for animations. */
    public final void draw(KDraw d, int mx, int my, float delta) {
        if (!visible) {
            return;
        }
        boolean hovered = active && isOver(mx, my);
        hoverAnim = Math.max(0, Math.min(1, hoverAnim + (hovered ? 0.25F : -0.2F) * Math.max(0.2F, delta)));
        render(d, mx, my, hovered);
    }

    protected abstract void render(KDraw d, int mx, int my, boolean hovered);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        return false;
    }

    public static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.35F, 0.6F));
    }

    public static void pop() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 0.8F));
    }
}
