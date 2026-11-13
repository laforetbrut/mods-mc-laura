package com.vyrriox.lauramod.client.ui;

import dev.dryaides.minuit.FrameStyles;
import dev.dryaides.minuit.Palette;
import dev.dryaides.minuit.Theme;
import dev.dryaides.minuit.Themes;

/**
 * Laura's own look in minuit: the pastel colours of the old kawaii widgets on the soft family. It is
 * the default until the player picks another theme for the mod on minuit's theme page.
 *
 * @author vyrriox
 */
public final class LauraTheme {
    public static final String ID = "lauramod";

    private static final Palette KAWAII = new Palette(
            0xFFFFB6D5, 0xFFF77FB2, 0xFFFFE4F1, 0xFFE0558F,
            0xFFFFF7FB, 0xFFFFB6D5, 0xFFF3ECFF, 0xFFFFFFFF, 0xFFE9C6D9,
            0xFF5B2A4D, 0xFF9C6B8A, 0xFFB9A0B0,
            0xFFFFE4F1, 0xFFFFB6D5, 0xFF5B2A4D,
            0xFFF77FB2, 0xFFE0558F, 0xFFFFFFFF,
            0xFFE0558F, 0xFFA0406E,
            0xFF7EE0C3, 0xFFFFB86B, 0xFFFF6B8A,
            0xFFFF6B8A, 0xFFE0556F, 0xFF5B2A4D,
            0x30A0406E, 0xFFE8D5DF, 0xFFB9A0B0, 0x40A0406E);

    private static boolean registered;

    private LauraTheme() {
    }

    /** Registers the look once; every minuit screen and HUD part of the mod calls it first. */
    public static void register() {
        if (!registered) {
            registered = true;
            Themes.register(new Theme(ID, KAWAII, FrameStyles.SOFT));
        }
    }

    /** True when {@code theme} resolves to the mod's own pastel look (its own palette on the soft family). */
    public static boolean isOwn(Theme theme) {
        return theme != null && KAWAII.equals(theme.palette()) && theme.frame() == FrameStyles.SOFT;
    }

    /** The look the player picked for the mod, or the pastel one. */
    public static Theme get() {
        register();
        return Themes.byId(ID);
    }
}
