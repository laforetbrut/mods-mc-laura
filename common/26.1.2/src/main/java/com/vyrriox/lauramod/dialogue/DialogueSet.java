package com.vyrriox.lauramod.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything Laura can say in one language: event lines, chat intents and item keywords.
 *
 * @author vyrriox
 */
public final class DialogueSet {
    private final String locale;
    private final Map<String, List<String>> lines = new LinkedHashMap<>();
    private final Map<String, Intent> intents = new LinkedHashMap<>();
    private final Map<String, List<TextMatcher.Trigger>> itemKeywords = new LinkedHashMap<>();
    private final Map<String, List<TextMatcher.Trigger>> chestKeywords = new LinkedHashMap<>();
    private final List<TextMatcher.Trigger> connectors = new ArrayList<>();
    private final List<TextMatcher.Trigger> everyone = new ArrayList<>();
    /** Spelling rules: normalized words to their usual written form (empty to drop a filler word). */
    private final Map<List<String>, List<String>> spelling = new LinkedHashMap<>();
    private int longestSpellingRule;

    public DialogueSet(String locale) {
        this.locale = locale;
    }

    public String locale() {
        return locale;
    }

    public List<String> lines(String key) {
        return lines.getOrDefault(key, Collections.emptyList());
    }

    public Map<String, List<String>> allLines() {
        return Collections.unmodifiableMap(lines);
    }

    public Map<String, Intent> intents() {
        return Collections.unmodifiableMap(intents);
    }

    public Map<String, List<TextMatcher.Trigger>> itemKeywords() {
        return Collections.unmodifiableMap(itemKeywords);
    }

    public Map<String, List<TextMatcher.Trigger>> chestKeywords() {
        return Collections.unmodifiableMap(chestKeywords);
    }

    /** Words that chain orders ("then", "puis", "danach"...). */
    public List<TextMatcher.Trigger> connectors() {
        return Collections.unmodifiableList(connectors);
    }

    /** Words addressing all companions at once ("everyone", "les filles"...). */
    public List<TextMatcher.Trigger> everyone() {
        return Collections.unmodifiableList(everyone);
    }

    public boolean hasSpelling() {
        return !spelling.isEmpty();
    }

    /**
     * The message rewritten with this language's spelling rules: chat shortcuts, frequent mistakes
     * and filler words ("tu est trop jolie" becomes "tu es jolie", "jtm" becomes "je t aime").
     * Rules are applied once, left to right, the longest rule first at each word.
     */
    public TextMatcher.Prepared respell(TextMatcher.Prepared message) {
        if (spelling.isEmpty()) {
            return message;
        }
        List<String> in = message.words();
        List<String> out = new ArrayList<>(in.size());
        int i = 0;
        while (i < in.size()) {
            List<String> replacement = null;
            int used = 0;
            for (int n = Math.min(longestSpellingRule, in.size() - i); n >= 1 && replacement == null; n--) {
                replacement = spelling.get(in.subList(i, i + n));
                used = n;
            }
            if (replacement != null) {
                out.addAll(replacement);
                i += used;
            } else {
                out.add(in.get(i++));
            }
        }
        if (out.equals(in)) {
            return message;
        }
        String text = String.join(" ", out);
        return new TextMatcher.Prepared(text, out, text.replace(" ", ""));
    }

    /** Merges a parsed JSON dialogue file into this set. */
    public void merge(JsonObject json, boolean replace) {
        if (replace) {
            lines.clear();
            intents.clear();
            itemKeywords.clear();
            chestKeywords.clear();
            connectors.clear();
            everyone.clear();
            spelling.clear();
            longestSpellingRule = 0;
        }
        JsonObject spellingObj = object(json, "spelling");
        if (spellingObj != null) {
            for (Map.Entry<String, JsonElement> e : spellingObj.entrySet()) {
                if (!e.getValue().isJsonPrimitive()) {
                    continue;
                }
                List<String> from = TextMatcher.tokens(TextMatcher.normalize(e.getKey()));
                if (from.isEmpty()) {
                    continue;
                }
                spelling.put(List.copyOf(from), List.copyOf(TextMatcher.tokens(TextMatcher.normalize(e.getValue().getAsString()))));
                longestSpellingRule = Math.max(longestSpellingRule, from.size());
            }
        }
        for (String word : strings(json.get("everyone"))) {
            TextMatcher.Trigger t = TextMatcher.Trigger.of(word);
            if (!t.isEmpty()) {
                everyone.add(t);
            }
        }
        JsonObject chestObj = object(json, "chests");
        if (chestObj != null) {
            for (Map.Entry<String, JsonElement> e : chestObj.entrySet()) {
                List<TextMatcher.Trigger> list = chestKeywords.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
                for (String keyword : strings(e.getValue())) {
                    TextMatcher.Trigger t = TextMatcher.Trigger.of(keyword);
                    if (!t.isEmpty()) {
                        list.add(t);
                    }
                }
            }
        }
        for (String connector : strings(json.get("connectors"))) {
            TextMatcher.Trigger t = TextMatcher.Trigger.of(connector);
            if (!t.isEmpty()) {
                connectors.add(t);
            }
        }
        JsonObject lineObj = object(json, "lines");
        if (lineObj != null) {
            for (Map.Entry<String, JsonElement> e : lineObj.entrySet()) {
                List<String> values = strings(e.getValue());
                if (!values.isEmpty()) {
                    lines.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(values);
                }
            }
        }
        JsonObject intentObj = object(json, "intents");
        if (intentObj != null) {
            for (Map.Entry<String, JsonElement> e : intentObj.entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                boolean replaceIntent = o.has("replace") && o.get("replace").getAsBoolean();
                Intent intent = replaceIntent ? null : intents.get(e.getKey());
                if (intent == null) {
                    intent = new Intent(e.getKey(), new ArrayList<>(), new ArrayList<>());
                    intents.put(e.getKey(), intent);
                }
                for (String trigger : strings(o.get("triggers"))) {
                    TextMatcher.Trigger t = TextMatcher.Trigger.of(trigger);
                    if (!t.isEmpty()) {
                        intent.triggers().add(t);
                    }
                }
                intent.responses().addAll(strings(o.get("responses")));
            }
        }
        JsonObject itemObj = object(json, "items");
        if (itemObj != null) {
            for (Map.Entry<String, JsonElement> e : itemObj.entrySet()) {
                List<TextMatcher.Trigger> list = itemKeywords.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
                for (String keyword : strings(e.getValue())) {
                    TextMatcher.Trigger t = TextMatcher.Trigger.of(keyword);
                    if (!t.isEmpty()) {
                        list.add(t);
                    }
                }
            }
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    static List<String> strings(JsonElement element) {
        List<String> out = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return out;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (item.isJsonPrimitive()) {
                    String s = item.getAsString();
                    if (!s.isBlank()) {
                        out.add(s);
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String s = element.getAsString();
            if (!s.isBlank()) {
                out.add(s);
            }
        }
        return out;
    }

    /** A chat intent: trigger phrases and possible answers. */
    public record Intent(String id, List<TextMatcher.Trigger> triggers, List<String> responses) {
    }
}
