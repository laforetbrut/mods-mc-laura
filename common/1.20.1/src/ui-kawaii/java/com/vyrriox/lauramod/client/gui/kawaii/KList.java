package com.vyrriox.lauramod.client.gui.kawaii;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrolling list of rows drawn by a callback, with a cute scrollbar.
 *
 * @author vyrriox
 */
public class KList<T> extends KWidget {
    /** Draws one row; returns nothing. */
    public interface RowRenderer<T> {
        void draw(KDraw d, T item, int x, int y, int w, int h, boolean hovered, int index);
    }

    /** Called when a row is clicked. */
    public interface RowClick<T> {
        void clicked(T item, int index, double localX, int button);
    }

    private final int rowHeight;
    private final RowRenderer<T> renderer;
    private final RowClick<T> onClick;
    private List<T> items = new ArrayList<>();
    private double scroll;
    public T selected;

    public KList(int x, int y, int w, int h, int rowHeight, RowRenderer<T> renderer, RowClick<T> onClick) {
        super(x, y, w, h);
        this.rowHeight = rowHeight;
        this.renderer = renderer;
        this.onClick = onClick;
    }

    public void setItems(List<T> items) {
        this.items = new ArrayList<>(items);
        scroll = Math.min(scroll, maxScroll());
    }

    public List<T> items() {
        return items;
    }

    private double maxScroll() {
        return Math.max(0, items.size() * rowHeight - h);
    }

    @Override
    protected void render(KDraw d, int mx, int my, boolean hovered) {
        Kawaii.rounded(d, x, y, w, h, 4, 0xFFFFF0F6);
        d.scissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int first = (int) (scroll / rowHeight);
        for (int i = first; i < items.size(); i++) {
            int ry = y + i * rowHeight - (int) scroll;
            if (ry > y + h) {
                break;
            }
            boolean rowHover = hovered && my >= ry && my < ry + rowHeight && mx < x + w - 6;
            if (items.get(i) == selected) {
                Kawaii.rounded(d, x + 2, ry + 1, w - 10, rowHeight - 2, 3, Kawaii.PINK);
            } else if (rowHover) {
                Kawaii.rounded(d, x + 2, ry + 1, w - 10, rowHeight - 2, 3, Kawaii.BLUSH);
            }
            renderer.draw(d, items.get(i), x + 4, ry, w - 14, rowHeight, rowHover, i);
        }
        d.endScissor();
        double max = maxScroll();
        if (max > 0) {
            int track = h - 4;
            int thumb = Math.max(12, (int) (track * (h / (double) (items.size() * rowHeight))));
            int ty = y + 2 + (int) ((track - thumb) * (scroll / max));
            Kawaii.rounded(d, x + w - 5, y + 2, 3, track, 1, 0x30A0406E);
            Kawaii.rounded(d, x + w - 5, ty, 3, thumb, 1, Kawaii.PINK_DEEP);
        }
        if (items.isEmpty()) {
            Kawaii.heart(d, x + w / 2 - 7, y + h / 2 - 6, 2, 0x40F77FB2);
        }
        if (!active) {
            Kawaii.rounded(d, x, y, w, h, 4, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - amount * rowHeight));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!active || !isOver(mx, my) || mx >= x + w - 6) {
            return false;
        }
        int index = (int) ((my - y + scroll) / rowHeight);
        if (index >= 0 && index < items.size()) {
            selected = items.get(index);
            click();
            onClick.clicked(items.get(index), index, mx - x, button);
            return true;
        }
        return false;
    }
}
