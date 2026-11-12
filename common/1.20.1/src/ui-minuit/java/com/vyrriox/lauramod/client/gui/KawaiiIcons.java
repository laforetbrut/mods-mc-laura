package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import dev.dryaides.minuit.AtlasIcon;
import dev.dryaides.minuit.Draw;
import dev.dryaides.minuit.IconArt;
import dev.dryaides.minuit.ItemView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * The mod's icons for minuit: a cell of {@code kawaii_icons.png}, or the vanilla item an
 * {@link Icon} names, so the rail, the buttons and the HUD show the same pictures as before.
 *
 * @author vyrriox
 */
public final class KawaiiIcons {
    private static final String ATLAS = "lauramod:textures/gui/kawaii_icons.png";
    private static final Map<Icon, IconArt> CACHE = new EnumMap<>(Icon.class);

    private KawaiiIcons() {
    }

    public static IconArt of(Icon icon) {
        return CACHE.computeIfAbsent(icon, i -> {
            ItemStack stack = i.item();
            return stack.isEmpty() ? AtlasIcon.cell16(ATLAS, i.ordinal()) : item(stack);
        });
    }

    /** The icon named like an {@link Icon} constant; a sparkle for a name the atlas does not have. */
    public static IconArt of(String name) {
        try {
            return of(Icon.valueOf(name));
        } catch (IllegalArgumentException e) {
            return of(Icon.SPARKLE);
        }
    }

    public static IconArt item(ItemStack stack) {
        return new ItemView(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 1, stack);
    }

    /** The face of a skin texture, hat layer on top, as {@code size} pixels. */
    public static IconArt face(String texture, int size) {
        return new IconArt() {
            @Override
            public int width() {
                return size;
            }

            @Override
            public int height() {
                return size;
            }

            @Override
            public void draw(Draw d, int x, int y, int color, float alpha) {
                d.blitRegion(texture, x, y, size, size, 8, 8, 8, 8, 64, 64, alpha);
                d.blitRegion(texture, x, y, size, size, 40, 8, 8, 8, 64, 64, alpha);
            }
        };
    }
}
