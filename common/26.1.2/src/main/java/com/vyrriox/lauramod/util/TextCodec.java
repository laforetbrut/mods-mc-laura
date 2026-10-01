package com.vyrriox.lauramod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * Component JSON conversion. Isolated here because the vanilla API changes between versions.
 *
 * @author vyrriox
 */
public final class TextCodec {
    private TextCodec() {
    }

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow(JsonParseException::new).toString();
    }

    public static Component fromJson(String json, HolderLookup.Provider registries) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), element)
                    .getOrThrow(JsonParseException::new);
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }
}
