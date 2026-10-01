package com.vyrriox.lauramod.dialogue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Language-agnostic matching of chat messages against trigger phrases.
 * <p>
 * Text is lower-cased, accents are stripped and punctuation becomes spaces, so "Désolée !" matches
 * the trigger "desole*". Words are compared as whole words, which avoids the 1.x bug where "ven"
 * matched inside "souvent". Scripts written without spaces (Chinese, Japanese, Thai...) are matched
 * as substrings instead.
 *
 * @author vyrriox
 */
public final class TextMatcher {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}*]+");

    private TextMatcher() {
    }

    /** Normalized form of a message or trigger. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        // Hangul syllables must stay composed, otherwise stripping marks would not matter but
        // decomposition would make words longer than typed. Only decompose non Hangul text.
        String decomposed = containsHangul(lower) ? lower : Normalizer.normalize(lower, Normalizer.Form.NFD);
        String stripped = MARKS.matcher(decomposed).replaceAll("");
        stripped = stripped.replace('ß', 's').replace('ø', 'o').replace('æ', 'a').replace('œ', 'o').replace('ł', 'l').replace('đ', 'd');
        return NON_WORD.matcher(stripped).replaceAll(" ").trim();
    }

    public static List<String> tokens(String normalized) {
        List<String> out = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out;
    }

    /** A prepared message: normalized text, its words, and the same text without spaces. */
    public record Prepared(String text, List<String> words, String compact) {
        public static Prepared of(String raw) {
            String n = normalize(raw);
            return new Prepared(n, tokens(n), n.replace(" ", ""));
        }
    }

    /** A prepared trigger phrase. */
    public record Trigger(String raw, List<String> words, String compact, boolean unspaced) {
        public static Trigger of(String raw) {
            String n = normalize(raw);
            if (n.codePoints().noneMatch(Character::isLetter)) {
                // Only digits or symbols left ("<3" becomes "3"): it would fire on any number.
                return new Trigger(raw, java.util.List.of(), "", false);
            }
            return new Trigger(raw, tokens(n), n.replace(" ", "").replace("*", ""), isUnspacedScript(n));
        }

        public boolean isEmpty() {
            return words.isEmpty();
        }

        /** Number of words, used to prefer the longest (most specific) trigger. */
        public int weight() {
            return unspaced ? compact.length() : words.size() * 4 + compact.length();
        }

        /** True if the trigger's words start exactly at {@code index} in the list. */
        public boolean matchesAt(List<String> in, int index) {
            if (unspaced || words.isEmpty() || index + words.size() > in.size()) {
                return false;
            }
            for (int i = 0; i < words.size(); i++) {
                if (!wordMatches(words.get(i), in.get(index + i))) {
                    return false;
                }
            }
            return true;
        }

        public boolean matches(Prepared message) {
            if (words.isEmpty()) {
                return false;
            }
            if (unspaced) {
                return !compact.isEmpty() && message.compact().contains(compact);
            }
            List<String> in = message.words();
            outer:
            for (int start = 0; start + words.size() <= in.size(); start++) {
                for (int i = 0; i < words.size(); i++) {
                    if (!wordMatches(words.get(i), in.get(start + i))) {
                        continue outer;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private static boolean wordMatches(String pattern, String word) {
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return word.startsWith(prefix);
        }
        return pattern.equals(word);
    }

    private static boolean containsHangul(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '가' && c <= '힯') {
                return true;
            }
        }
        return false;
    }

    /** True if the text uses a script that does not separate words with spaces. */
    public static boolean isUnspacedScript(String s) {
        for (int i = 0; i < s.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(s.codePointAt(i));
            if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.THAI
                    || script == Character.UnicodeScript.LAO || script == Character.UnicodeScript.KHMER
                    || script == Character.UnicodeScript.MYANMAR) {
                return true;
            }
        }
        return false;
    }
}
