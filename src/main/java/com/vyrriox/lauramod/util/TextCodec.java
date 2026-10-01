package com.vyrriox.lauramod.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

/**
 * Component JSON conversion. Isolated here because the vanilla API changes between versions.
 *
 * @author vyrriox
 */
public final class TextCodec {
    private TextCodec() {
    }

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return Component.Serializer.toJson(component, registries);
    }

    public static Component fromJson(String json, HolderLookup.Provider registries) {
        try {
            Component c = Component.Serializer.fromJson(json, registries);
            return c == null ? Component.empty() : c;
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }
}
