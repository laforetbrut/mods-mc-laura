package com.vyrriox.lauramod.client.ui;

import dev.dryaides.minuit.Box;
import dev.dryaides.minuit.ButtonComponent;
import dev.dryaides.minuit.Draw;
import dev.dryaides.minuit.IconArt;
import dev.dryaides.minuit.Palette;
import dev.dryaides.minuit.Theme;

import java.util.List;

/**
 * A button with its icon above a small label: emotes, jobs, chest purposes, built-in skins.
 *
 * @author vyrriox
 */
final class CardButton extends ButtonComponent {
    private final IconArt art;
    private final String text;

    CardButton(Box box, IconArt art, String text, Runnable action) {
        super(box, "", action);
        this.art = art;
        this.text = text;
        content(this::drawFace);
    }

    private Theme drawnTheme;

    @Override
    protected void render(Draw d, Theme theme, int mx, int my, boolean hovered) {
        this.drawnTheme = theme;
        super.render(d, theme, mx, my, hovered);
    }

    /** Drawn by minuit inside the face box, so the head and label sink and clip with the pressed face. */
    private void drawFace(Draw d, int fx, int fy, int fw, int fh, int ink) {
        Theme theme = this.drawnTheme;
        Palette p = theme.palette();
        int color = !this.active ? p.disabledText() : this.role == Role.DESTRUCTIVE ? p.destructiveText() : p.raisedText();
        boolean label = this.text != null && !this.text.isEmpty() && fh >= 26;
        // The content lifts on hover only in the mod's own look; other themes keep minuit's plain hover.
        int lift = LauraTheme.isOwn(theme) ? Math.round(this.hoverAnim * 1.5F) : 0;
        // A label too wide for the card wraps on a second line when the card is tall enough, never cut.
        List<String> lines = label ? d.wrap(this.text, (int) ((fw - 4) / 0.75F)) : List.of();
        if (lines.size() > 1 && fh < 30) {
            lines = List.of(this.text);
        }
        int lineCount = Math.min(2, lines.size());
        if (this.art != null) {
            int iy = !label ? fy + (fh - this.art.height()) / 2
                    : lineCount > 1 ? fy + Math.max(1, (fh - 31) / 2 + 1)
                    : fy + Math.max(2, (fh - 24) / 2);
            this.art.draw(d, fx + (fw - this.art.width()) / 2, iy - lift, color, this.active ? 1F : 0.5F);
        }
        for (int i = 0; i < lineCount; i++) {
            String line = lines.get(i);
            int tw = Math.min(fw - 4, Math.round(d.width(line) * 0.75F) + 1);
            int ly = lineCount > 1 ? fy + fh - 8 - (1 - i) * 6 : fy + fh - 9;
            d.small(line, new Box("label", fx + (fw - tw) / 2, ly, tw, 6), color, 0.75F, d.guiScale());
        }
    }

    /** The tip (a skin's or an emote's name) opens under the card instead of over its face. */
    @Override
    public int tooltipY(double mx, double my) {
        return this.y + this.h + 18;
    }
}
