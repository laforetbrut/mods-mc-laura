package com.vyrriox.lauramod.client.gui.kawaii;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * A soft pill button with an optional icon (atlas icon or item) that bounces when hovered.
 * Shift click is reported to the action (used to queue orders instead of running them now).
 *
 * @author vyrriox
 */
public class KButton extends KWidget {
    public enum Style {
        PINK(Kawaii.PINK, Kawaii.PINK_HOVER, Kawaii.PLUM),
        LAVENDER(Kawaii.LAVENDER, Kawaii.LAVENDER_DEEP, Kawaii.PLUM),
        MINT(Kawaii.MINT, Kawaii.MINT_DEEP, 0xFF1F5E4B),
        PEACH(Kawaii.PEACH, Kawaii.PEACH_DEEP, 0xFF6B3A1F),
        SKY(Kawaii.SKY, Kawaii.SKY_DEEP, 0xFF1F4A6B),
        GHOST(0x00FFFFFF, 0x40FFB6D5, Kawaii.PLUM);

        final int base;
        final int hover;
        final int text;

        Style(int base, int hover, int text) {
            this.base = base;
            this.hover = hover;
            this.text = text;
        }
    }

    /** What the action receives. */
    public record Click(int button, boolean shift) {
    }

    public Component label;
    public Icon icon;
    public ItemStack item = ItemStack.EMPTY;
    public Style style = Style.PINK;
    public boolean selected;
    public boolean vertical;
    private final Consumer<Click> action;

    public KButton(int x, int y, int w, int h, Component label, Consumer<Click> action) {
        super(x, y, w, h);
        this.label = label;
        this.action = action;
    }

    public KButton icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public KButton item(ItemStack item) {
        this.item = item;
        return this;
    }

    public KButton style(Style style) {
        this.style = style;
        return this;
    }

    public KButton vertical() {
        this.vertical = true;
        return this;
    }

    public KButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    protected void render(KDraw d, int mx, int my, boolean hovered) {
        int lift = Math.round(hoverAnim * 1.5F);
        int color;
        if (!active) {
            color = Kawaii.DISABLED;
        } else if (selected) {
            color = Kawaii.darker(style.hover, 0.95F);
        } else {
            color = Kawaii.mix(style.base, style.hover, hoverAnim);
        }
        int top = y - lift;
        if (style == Style.GHOST && !selected && hoverAnim < 0.05F) {
            // Invisible until hovered.
        } else {
            Kawaii.pill(d, x, top, w, h, color, active);
        }
        if (selected) {
            Kawaii.rounded(d, x, top + h - 2, w, 2, 1, Kawaii.ROSE);
        }
        int textColor = active ? style.text : Kawaii.DISABLED_TEXT;
        boolean hasIcon = icon != null || !item.isEmpty();
        if (vertical) {
            int iconY = top + (label == null ? (h - 16) / 2 : 3);
            if (hasIcon) {
                drawIcon(d, x + (w - 16) / 2, iconY);
            }
            if (label != null) {
                // Long names shrink, then get cut, so they never leave the button.
                float scale = Math.min(0.75F, (w - 4F) / Math.max(1, d.width(label)));
                Component text = label;
                if (scale < 0.5F) {
                    scale = 0.5F;
                    text = d.ellipsize(label, (int) ((w - 4) / scale));
                }
                d.push(x + w / 2F, top + h - 3 - 8 * scale, scale);
                d.centered(text, 0, 0, textColor);
                d.pop();
            }
            return;
        }
        int iconPart = hasIcon ? (label != null ? 19 : 16) : 0;
        Component text = label;
        float scale = 1F;
        if (label != null) {
            int room = w - 6 - iconPart;
            scale = Math.min(1F, room / (float) Math.max(1, d.width(label)));
            if (scale < 0.55F) {
                scale = 0.55F;
                text = d.ellipsize(label, (int) (room / scale));
            }
        }
        int textWidth = text == null ? 0 : Math.round(d.width(text) * scale);
        int cx = x + (w - iconPart - textWidth) / 2;
        if (hasIcon) {
            drawIcon(d, cx, top + (h - 16) / 2);
            cx += iconPart;
        }
        if (text != null) {
            if (scale < 1F) {
                d.push(cx, top + (h - 8 * scale) / 2F + 0.5F, scale);
                d.text(text, 0, 0, textColor);
                d.pop();
            } else {
                d.text(text, cx, top + (h - 8) / 2 + 1, textColor);
            }
        }
    }

    private void drawIcon(KDraw d, int ix, int iy) {
        if (icon != null) {
            d.icon(icon, ix, iy);
        } else {
            d.item(item, ix, iy);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!active || !isOver(mx, my) || button > 1) {
            return false;
        }
        click();
        action.accept(new Click(button, Screen.hasShiftDown()));
        return true;
    }
}
