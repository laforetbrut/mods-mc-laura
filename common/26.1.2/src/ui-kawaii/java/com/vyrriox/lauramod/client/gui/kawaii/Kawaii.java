package com.vyrriox.lauramod.client.gui.kawaii;

/**
 * The pastel palette and the drawing primitives of Laura's screens: rounded panels, pills, pixel
 * hearts, soft shadows, progress bars.
 *
 * @author vyrriox
 */
public final class Kawaii {
    // Palette
    public static final int CREAM = 0xFFFFF7FB;
    public static final int BLUSH = 0xFFFFE4F1;
    public static final int PINK = 0xFFFFB6D5;
    public static final int PINK_HOVER = 0xFFFF9CC6;
    public static final int PINK_DEEP = 0xFFF77FB2;
    public static final int ROSE = 0xFFE0558F;
    public static final int PLUM = 0xFF5B2A4D;
    public static final int PLUM_SOFT = 0xFF9C6B8A;
    public static final int LAVENDER = 0xFFD9C8FF;
    public static final int LAVENDER_DEEP = 0xFFB39DFF;
    public static final int MINT = 0xFFC4F2E2;
    public static final int MINT_DEEP = 0xFF7EE0C3;
    public static final int PEACH = 0xFFFFD6B8;
    public static final int PEACH_DEEP = 0xFFFFB38A;
    public static final int SKY = 0xFFCDE8FF;
    public static final int SKY_DEEP = 0xFF8FD3FF;
    public static final int LEMON = 0xFFFFF4B8;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SHADOW = 0x40A0406E;
    public static final int DISABLED = 0xFFE8D5DF;
    public static final int DISABLED_TEXT = 0xFFB9A0B0;

    private Kawaii() {
    }

    /** A filled rectangle with rounded corners (radius in pixels, 0 to 4). */
    public static void rounded(KDraw d, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        d.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            d.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            d.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** Horizontal inset of row {@code row} (0 = top) of a corner of radius r. */
    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5;
        double dx = Math.sqrt(Math.max(0, r * r - dy * dy));
        return (int) Math.round(r - dx);
    }

    /** A rounded outline of 1 pixel. */
    public static void roundedBorder(KDraw d, int x, int y, int w, int h, int r, int color) {
        rounded(d, x, y, w, h, r, color);
    }

    /** A soft panel: shadow, colored border, light fill, top highlight. */
    public static void panel(KDraw d, int x, int y, int w, int h, int border, int fill) {
        rounded(d, x + 2, y + 3, w, h, 6, SHADOW);
        rounded(d, x, y, w, h, 6, border);
        rounded(d, x + 2, y + 2, w - 4, h - 4, 5, fill);
        d.fill(x + 6, y + 3, x + w - 6, y + 4, 0x66FFFFFF);
    }

    /** A card inside a panel. */
    public static void card(KDraw d, int x, int y, int w, int h, int fill) {
        rounded(d, x, y + 1, w, h, 4, 0x22A0406E);
        rounded(d, x, y, w, h, 4, fill);
    }

    /** A pill-shaped surface with a light top line. */
    public static void pill(KDraw d, int x, int y, int w, int h, int color, boolean highlight) {
        rounded(d, x, y + 1, w, h, Math.min(4, h / 2), 0x33A0406E);
        rounded(d, x, y, w, h, Math.min(4, h / 2), color);
        if (highlight) {
            d.fill(x + 3, y + 1, x + w - 3, y + 2, 0x80FFFFFF);
        }
    }

    /** A pixel heart; size 1 draws 7x6 pixels, size 2 draws 14x12... */
    public static void heart(KDraw d, int x, int y, int size, int color) {
        String[] shape = {
                ".XX.XX.",
                "XXXXXXX",
                "XXXXXXX",
                ".XXXXX.",
                "..XXX..",
                "...X..."};
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length(); col++) {
                if (shape[row].charAt(col) == 'X') {
                    d.fill(x + col * size, y + row * size, x + (col + 1) * size, y + (row + 1) * size, color);
                }
            }
        }
    }

    /** A rounded progress bar with a shine line. */
    public static void bar(KDraw d, int x, int y, int w, int h, float value, int color, int background) {
        rounded(d, x, y, w, h, h / 2, background);
        int filled = Math.round(Math.max(0, Math.min(1, value)) * (w - 2));
        if (filled > 0) {
            rounded(d, x + 1, y + 1, Math.max(filled, h - 2), h - 2, (h - 2) / 2, color);
            d.fill(x + 2, y + 2, x + 1 + Math.max(filled, h - 2) - 1, y + 3, 0x66FFFFFF);
        }
    }

    /** Color of a need bar from its value (0 to 100). */
    public static int needColor(float value, int base) {
        if (value < 20) {
            return 0xFFFF6B8A;
        }
        if (value < 45) {
            return 0xFFFFB86B;
        }
        return base;
    }

    /** Darkens a color (factor 0 to 1). */
    public static int darker(int color, float factor) {
        int a = color >>> 24;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Blends two colors (t = 0 gives a, 1 gives b). */
    public static int mix(int a, int b, float t) {
        int aa = a >>> 24;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = b >>> 24;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
