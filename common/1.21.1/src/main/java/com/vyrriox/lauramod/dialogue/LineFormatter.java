package com.vyrriox.lauramod.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a dialogue line with {@code {placeholders}} into a chat component. Placeholder values can
 * be components, so item and biome names are translated by each client into its own language.
 *
 * @author vyrriox
 */
public final class LineFormatter {
    private LineFormatter() {
    }

    public static MutableComponent format(String template, Map<String, ?> values) {
        MutableComponent out = Component.empty();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i + 1) {
                    String key = template.substring(i + 1, end);
                    Object value = values.get(key);
                    if (value != null) {
                        if (!literal.isEmpty()) {
                            out.append(Component.literal(literal.toString()));
                            literal.setLength(0);
                        }
                        out.append(value instanceof Component component ? component.copy() : Component.literal(String.valueOf(value)));
                        i = end + 1;
                        continue;
                    }
                }
            }
            literal.append(c);
            i++;
        }
        if (!literal.isEmpty()) {
            out.append(Component.literal(literal.toString()));
        }
        return out;
    }

    /** Small builder for placeholder maps. */
    public static Values values() {
        return new Values();
    }

    public static final class Values extends HashMap<String, Object> {
        public Values with(String key, Object value) {
            if (value != null) {
                put(key, value);
            }
            return this;
        }
    }
}
