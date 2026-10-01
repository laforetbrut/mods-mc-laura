package com.vyrriox.lauramod.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads dialogue files and answers "what does Laura say" questions.
 * <p>
 * Built-in files live in the mod jar under {@code data/lauramod/dialogues/<locale>.json}. Files
 * dropped in {@code config/lauramod/dialogues/<locale>.json} are merged on top (or replace them, see
 * {@link LauraConfig#customDialoguesReplace}). A file for a language the mod does not ship simply
 * adds that language.
 *
 * @author vyrriox
 */
public final class DialogueManager {
    private static final String BUILTIN_ROOT = "/data/lauramod/dialogues/";

    private static volatile Map<String, DialogueSet> sets = Collections.emptyMap();
    private static volatile List<IndexedTrigger> triggerIndex = Collections.emptyList();
    private static volatile List<IndexedKeyword> itemIndex = Collections.emptyList();
    private static final Map<String, String> LAST_LINE = new ConcurrentHashMap<>();

    private DialogueManager() {
    }

    public static synchronized void reload() {
        Map<String, DialogueSet> loaded = new LinkedHashMap<>();
        List<String> builtin = builtinLocales();
        for (String locale : builtin) {
            JsonObject json = readBuiltin(locale);
            if (json != null) {
                loaded.computeIfAbsent(locale, DialogueSet::new).merge(json, false);
            }
        }
        Path dir = LauraMod.configDir().resolve("dialogues");
        writeHelpFiles(dir, builtin);
        boolean replaceAll = LauraConfig.customDialoguesReplace.get();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String locale = LocaleUtil.normalize(file.getFileName().toString().replaceFirst("\\.json$", ""));
                JsonObject json = readFile(file);
                if (json == null) {
                    continue;
                }
                boolean replace = replaceAll || (json.has("mode") && "replace".equalsIgnoreCase(json.get("mode").getAsString()));
                loaded.computeIfAbsent(locale, DialogueSet::new).merge(json, replace);
                LauraMod.LOGGER.info("Loaded custom dialogues for {} from {}", locale, file.getFileName());
            }
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not list custom dialogues in {}: {}", dir, e.getMessage());
        }
        com.vyrriox.lauramod.api.ScriptData.mergeDialogues(loaded);
        sets = Collections.unmodifiableMap(loaded);
        rebuildIndexes(loaded);
        LauraMod.LOGGER.info("Dialogues ready: {} languages, {} chat triggers", loaded.size(), triggerIndex.size());
    }

    public static Map<String, DialogueSet> sets() {
        return sets;
    }

    public static List<String> languages() {
        List<String> out = new ArrayList<>(sets.keySet());
        Collections.sort(out);
        return out;
    }

    /** The dialogue language to use for a player language code. */
    public static List<String> chainFor(String playerLocale) {
        String forced = LauraConfig.forcedLanguage.get();
        String code = forced != null && !forced.isBlank() ? forced : playerLocale;
        return LocaleUtil.chain(code, LauraConfig.fallbackLanguage.get());
    }

    /** Picks a random line for the key, walking the language fallback chain. Null if none exists. */
    public static String pick(String playerLocale, String key, RandomSource random) {
        for (String locale : chainFor(playerLocale)) {
            DialogueSet set = sets.get(locale);
            if (set == null) {
                continue;
            }
            List<String> lines = set.lines(key);
            if (!lines.isEmpty()) {
                return pickFrom(locale + "|" + key, lines, random);
            }
        }
        return null;
    }

    /** Like {@link #pick} but tries the more specific keys first: pickFirst(l, r, "ambient.hungry", "ambient"). */
    public static String pickFirst(String playerLocale, RandomSource random, String... keys) {
        for (String key : keys) {
            String line = pick(playerLocale, key, random);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    /** A random response of an intent in the player's language. */
    public static String respond(String playerLocale, String intentId, RandomSource random) {
        for (String locale : chainFor(playerLocale)) {
            DialogueSet set = sets.get(locale);
            if (set == null) {
                continue;
            }
            DialogueSet.Intent intent = set.intents().get(intentId);
            if (intent != null && !intent.responses().isEmpty()) {
                return pickFrom(locale + "|intent|" + intentId, intent.responses(), random);
            }
        }
        return null;
    }

    private static String pickFrom(String memoKey, List<String> lines, RandomSource random) {
        if (lines.size() == 1) {
            return lines.get(0);
        }
        String previous = LAST_LINE.get(memoKey);
        String line = lines.get(random.nextInt(lines.size()));
        if (line.equals(previous)) {
            line = lines.get(random.nextInt(lines.size()));
        }
        LAST_LINE.put(memoKey, line);
        return line;
    }

    /**
     * The languages whose chat words she listens to for a player: the player's own language chain
     * plus English, or every language when {@link LauraConfig#matchAllLanguages} is on (or when she
     * knows no language of that player). Limiting them avoids a word of one language being taken for
     * an order in another ("dans" means "dance" in Dutch but "in" in French). Null means all.
     */
    public static Set<String> listenLocales(String playerLocale) {
        if (playerLocale == null || LauraConfig.matchAllLanguages.get()) {
            return null;
        }
        Set<String> out = new HashSet<>();
        for (String code : chainFor(playerLocale)) {
            if (sets.containsKey(code)) {
                out.add(code);
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        out.add(LocaleUtil.DEFAULT);
        return out;
    }

    private static boolean listens(Set<String> locales, String locale) {
        return locales == null || locales.contains(locale);
    }

    /**
     * A message as written plus its rewrite by the spelling rules of each language, computed once
     * per language. A trigger matches either form, so "j'ai trop faim" still matches its own
     * trigger while "tu est trop jolie" matches "tu es jolie".
     */
    private static final class Variants {
        private final TextMatcher.Prepared original;
        private final Map<String, TextMatcher.Prepared> byLocale = new HashMap<>();

        Variants(TextMatcher.Prepared original) {
            this.original = original;
        }

        boolean matches(TextMatcher.Trigger trigger, String locale) {
            if (trigger.matches(original)) {
                return true;
            }
            TextMatcher.Prepared respelled = byLocale.computeIfAbsent(locale, l -> {
                DialogueSet set = sets.get(l);
                return set == null ? original : set.respell(original);
            });
            return respelled != original && trigger.matches(respelled);
        }
    }

    /** The message rewritten with one language's spelling rules (the message itself if none apply). */
    public static TextMatcher.Prepared respell(TextMatcher.Prepared message, String locale) {
        DialogueSet set = sets.get(locale);
        return set == null ? message : set.respell(message);
    }

    /** Finds the best intent for a chat message in every language she knows. Longer triggers win. */
    public static IntentMatch match(String message) {
        return match(message, null, null);
    }

    /** Finds the best intent for a chat message in the languages she listens to for this player. */
    public static IntentMatch match(String message, String playerLocale) {
        return match(message, null, playerLocale);
    }

    /** Like {@link #match} but only considers the given intents (null = all). */
    public static IntentMatch match(String message, Set<String> allowedIntents, String playerLocale) {
        TextMatcher.Prepared prepared = TextMatcher.Prepared.of(message);
        if (prepared.words().isEmpty()) {
            return null;
        }
        Set<String> locales = listenLocales(playerLocale);
        Variants variants = new Variants(prepared);
        for (IndexedTrigger t : triggerIndex) {
            if ((allowedIntents == null || allowedIntents.contains(t.intent)) && listens(locales, t.locale) && variants.matches(t.trigger, t.locale)) {
                return new IntentMatch(t.intent, t.locale, t.trigger.raw(), prepared);
            }
        }
        return null;
    }

    /** Item specs (item ids or #tags) mentioned in a message, most specific first. */
    public static List<String> matchItems(TextMatcher.Prepared message) {
        return matchItems(message, null);
    }

    public static List<String> matchItems(TextMatcher.Prepared message, String playerLocale) {
        Set<String> locales = listenLocales(playerLocale);
        Variants variants = new Variants(message);
        List<String> out = new ArrayList<>();
        for (IndexedKeyword k : itemIndex) {
            if (!out.contains(k.spec) && listens(locales, k.locale) && variants.matches(k.trigger, k.locale)) {
                out.add(k.spec);
            }
        }
        return out;
    }

    /** The chest purpose mentioned in a message ("this chest is for wood"), or null. */
    public static String matchChestPurpose(TextMatcher.Prepared message) {
        return matchChestPurpose(message, null);
    }

    public static String matchChestPurpose(TextMatcher.Prepared message, String playerLocale) {
        Set<String> locales = listenLocales(playerLocale);
        Variants variants = new Variants(message);
        String best = null;
        int bestWeight = -1;
        for (DialogueSet set : sets.values()) {
            if (!listens(locales, set.locale())) {
                continue;
            }
            for (Map.Entry<String, List<TextMatcher.Trigger>> e : set.chestKeywords().entrySet()) {
                for (TextMatcher.Trigger t : e.getValue()) {
                    if (t.weight() > bestWeight && variants.matches(t, set.locale())) {
                        best = e.getKey();
                        bestWeight = t.weight();
                    }
                }
            }
        }
        return best;
    }

    /**
     * Splits a message into separate orders on connector words of every language:
     * "chop a tree then cook" becomes ["chop a tree", "cook"].
     */
    public static List<String> splitOrders(String message) {
        return splitOrders(message, null);
    }

    public static List<String> splitOrders(String message, String playerLocale) {
        Set<String> locales = listenLocales(playerLocale);
        TextMatcher.Prepared prepared = TextMatcher.Prepared.of(message);
        List<String> words = prepared.words();
        List<TextMatcher.Trigger> connectors = new ArrayList<>();
        for (DialogueSet set : sets.values()) {
            if (listens(locales, set.locale())) {
                connectors.addAll(set.connectors());
            }
        }
        connectors.sort(Comparator.comparingInt(TextMatcher.Trigger::weight).reversed());
        List<String> segments = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < words.size()) {
            TextMatcher.Trigger matched = null;
            for (TextMatcher.Trigger c : connectors) {
                if (c.matchesAt(words, i)) {
                    matched = c;
                    break;
                }
            }
            if (matched != null) {
                if (i > start) {
                    segments.add(String.join(" ", words.subList(start, i)));
                }
                i += matched.words().size();
                start = i;
            } else {
                i++;
            }
        }
        if (start < words.size()) {
            segments.add(String.join(" ", words.subList(start, words.size())));
        }
        // Scripts without spaces: split on the connector text itself.
        List<String> out = new ArrayList<>();
        for (String segment : segments) {
            List<String> parts = new ArrayList<>(List.of(segment));
            for (TextMatcher.Trigger c : connectors) {
                if (!c.unspaced() || c.compact().isEmpty()) {
                    continue;
                }
                List<String> next = new ArrayList<>();
                for (String part : parts) {
                    for (String piece : part.split(java.util.regex.Pattern.quote(c.compact()))) {
                        if (!piece.isBlank()) {
                            next.add(piece.trim());
                        }
                    }
                }
                parts = next;
            }
            out.addAll(parts);
        }
        return out.isEmpty() ? List.of(message) : out;
    }

    /** True if the message addresses every companion at once ("everyone", "les filles"...). */
    public static boolean mentionsEveryone(String message) {
        return mentionsEveryone(message, null);
    }

    public static boolean mentionsEveryone(String message, String playerLocale) {
        Set<String> locales = listenLocales(playerLocale);
        Variants variants = new Variants(TextMatcher.Prepared.of(message));
        for (DialogueSet set : sets.values()) {
            if (!listens(locales, set.locale())) {
                continue;
            }
            for (TextMatcher.Trigger t : set.everyone()) {
                if (variants.matches(t, set.locale())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if the message contains one of the words used to address Laura (her name included). */
    public static boolean mentions(String message, String name) {
        TextMatcher.Prepared prepared = TextMatcher.Prepared.of(message);
        TextMatcher.Trigger trigger = TextMatcher.Trigger.of(name);
        return !trigger.isEmpty() && trigger.matches(prepared);
    }

    private static void rebuildIndexes(Map<String, DialogueSet> loaded) {
        List<IndexedTrigger> triggers = new ArrayList<>();
        List<IndexedKeyword> keywords = new ArrayList<>();
        for (DialogueSet set : loaded.values()) {
            for (DialogueSet.Intent intent : set.intents().values()) {
                for (TextMatcher.Trigger trigger : intent.triggers()) {
                    triggers.add(new IndexedTrigger(trigger, intent.id(), set.locale()));
                }
            }
            for (Map.Entry<String, List<TextMatcher.Trigger>> e : set.itemKeywords().entrySet()) {
                for (TextMatcher.Trigger trigger : e.getValue()) {
                    keywords.add(new IndexedKeyword(trigger, e.getKey(), set.locale()));
                }
            }
        }
        triggers.sort(Comparator.comparingInt((IndexedTrigger t) -> t.trigger.weight()).reversed());
        keywords.sort(Comparator.comparingInt((IndexedKeyword k) -> k.trigger.weight()).reversed());
        triggerIndex = Collections.unmodifiableList(triggers);
        itemIndex = Collections.unmodifiableList(keywords);
    }

    private static List<String> builtinLocales() {
        List<String> out = new ArrayList<>();
        try (InputStream in = DialogueManager.class.getResourceAsStream(BUILTIN_ROOT + "_index.json")) {
            if (in == null) {
                LauraMod.LOGGER.error("Built-in dialogue index is missing from the jar");
                return out;
            }
            JsonElement root = JsonParser.parseReader(new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            for (String s : DialogueSet.strings(root.getAsJsonObject().get("languages"))) {
                out.add(LocaleUtil.normalize(s));
            }
        } catch (Exception e) {
            LauraMod.LOGGER.error("Could not read the built-in dialogue index", e);
        }
        return out;
    }

    private static JsonObject readBuiltin(String locale) {
        try (InputStream in = DialogueManager.class.getResourceAsStream(BUILTIN_ROOT + locale + ".json")) {
            if (in == null) {
                LauraMod.LOGGER.warn("Built-in dialogue file {} is missing", locale);
                return null;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(text, locale);
        } catch (IOException e) {
            LauraMod.LOGGER.error("Could not read built-in dialogues {}", locale, e);
            return null;
        }
    }

    private static JsonObject readFile(Path file) {
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8), file.toString());
        } catch (IOException e) {
            LauraMod.LOGGER.error("Could not read {}", file, e);
            return null;
        }
    }

    private static JsonObject parse(String text, String source) {
        try {
            String clean = !text.isEmpty() && text.charAt(0) == '﻿' ? text.substring(1) : text;
            JsonElement root = JsonParser.parseReader(new JsonReader(new StringReader(clean)));
            if (root.isJsonObject()) {
                return root.getAsJsonObject();
            }
            LauraMod.LOGGER.error("Dialogue file {} is not a JSON object", source);
        } catch (Exception e) {
            LauraMod.LOGGER.error("Dialogue file {} is invalid: {}", source, e.getMessage());
        }
        return null;
    }

    private static void writeHelpFiles(Path dir, List<String> builtin) {
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
            Path reference = dir.resolve("_builtin");
            Files.createDirectories(reference);
            for (String locale : builtin) {
                try (InputStream in = DialogueManager.class.getResourceAsStream(BUILTIN_ROOT + locale + ".json")) {
                    if (in != null) {
                        Files.write(reference.resolve(locale + ".json"), in.readAllBytes());
                    }
                }
            }
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not write dialogue help files: {}", e.getMessage());
        }
    }

    /** Statistics for /laura lang. */
    public static Map<String, Integer> lineCounts() {
        Map<String, Integer> out = new HashMap<>();
        for (DialogueSet set : sets.values()) {
            int count = 0;
            for (List<String> lines : set.allLines().values()) {
                count += lines.size();
            }
            for (DialogueSet.Intent intent : set.intents().values()) {
                count += intent.responses().size();
            }
            out.put(set.locale(), count);
        }
        return out;
    }

    private record IndexedTrigger(TextMatcher.Trigger trigger, String intent, String locale) {
    }

    private record IndexedKeyword(TextMatcher.Trigger trigger, String spec, String locale) {
    }

    /** Result of {@link #match}. */
    public record IntentMatch(String intent, String locale, String trigger, TextMatcher.Prepared message) {
        public String languageHint() {
            return locale.toLowerCase(Locale.ROOT);
        }
    }

    private static final String README = """
            My Girlfriend Laura - custom dialogues
            =======================================

            Put a file named <language>.json in this folder (for example fr_fr.json, en_us.json, ja_jp.json).
            It is merged with the built-in dialogues of that language, or replaces them if the config option
            dialogue.customDialoguesReplace is true, or if the file contains "mode": "replace".

            A file for a language the mod does not ship adds that language: players using it get your lines.

            The _builtin folder holds read-only copies of the shipped files (rewritten at every start).
            Copy one here to start from it. Reload with /laura reload.

            Format:
            {
              "lines":   { "<event key>": ["line 1", "line 2"] },
              "intents": { "<intent id>": { "triggers": ["words players type"], "responses": ["her answers"] } },
              "items":   { "<item id or #tag>": ["keywords used in fetch orders"] },
              "spelling": { "what players type": "what it means" }
            }

            Placeholders: {player} {laura} {days} everywhere, plus the ones of each line ({item} {count} {place}
            {activity} {minutes} {other}...): the shipped file of your language in _builtin shows which line uses which.
            Triggers ignore case, accents and punctuation. End a word with * to match its beginning (sorr* = sorry, sorrry).
            "spelling" rewrites chat shortcuts and frequent mistakes before triggers are searched
            ("jtm": "je t'aime", "tu est": "tu es", "ur": "you're"). An empty value drops a filler word
            ("trop": "", "so": ""). A message matches a trigger as typed or after these rewrites.
            """;
}
