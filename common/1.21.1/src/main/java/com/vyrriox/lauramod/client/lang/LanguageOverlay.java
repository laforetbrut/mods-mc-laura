package com.vyrriox.lauramod.client.lang;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets players add or override translations without a resource pack: every
 * {@code config/lauramod/lang/<language>.json} file is layered on top of the game's language (for
 * this mod's keys and any other key). Installed after each resource reload.
 *
 * @author vyrriox
 */
public final class LanguageOverlay extends Language {
    private static volatile Map<String, String> entries = new HashMap<>();
    private static volatile String loadedFor = "";

    private final Language delegate;

    private LanguageOverlay(Language delegate) {
        this.delegate = delegate;
    }

    /** Wraps the current language if it is not wrapped yet. Cheap enough to call every tick. */
    public static void ensureInstalled() {
        Language current = Language.getInstance();
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        if (!selected.equals(loadedFor)) {
            load(selected);
        }
        if (!(current instanceof LanguageOverlay) && !entries.isEmpty()) {
            Language.inject(new LanguageOverlay(current));
        }
    }

    /** Forces a reload of the files (after a resource reload or /laura reload). */
    public static void reload() {
        loadedFor = "";
        ensureInstalled();
    }

    private static void load(String selected) {
        Map<String, String> map = new HashMap<>();
        Path dir = LauraMod.configDir().resolve("lang");
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // Read only folder: nothing to create.
        }
        read(dir.resolve("en_us.json"), map);
        if (!selected.equals("en_us")) {
            read(dir.resolve(selected + ".json"), map);
        }
        entries = map;
        loadedFor = selected;
    }

    private static void read(Path file, Map<String, String> map) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Language.loadFromJson(in, map::put);
            LauraMod.LOGGER.info("Loaded custom translations from {}", file.getFileName());
        } catch (Exception e) {
            LauraMod.LOGGER.error("Custom translation file {} is invalid: {}", file, e.getMessage());
        }
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        String custom = entries.get(key);
        return custom != null ? custom : delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean has(String key) {
        return entries.containsKey(key) || delegate.has(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return delegate.isDefaultRightToLeft();
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        return delegate.getVisualOrder(text);
    }

    /** NeoForge extension, kept working when the overlay is installed. */
    public Map<String, String> getLanguageData() {
        Map<String, String> merged = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> base = (Map<String, String>) Language.class.getMethod("getLanguageData").invoke(delegate);
            merged.putAll(base);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not NeoForge, or nothing to merge.
        }
        merged.putAll(entries);
        return merged;
    }

    /** NeoForge extension, kept working when the overlay is installed. */
    public net.minecraft.network.chat.Component getComponent(String key) {
        try {
            return (net.minecraft.network.chat.Component) Language.class.getMethod("getComponent", String.class).invoke(delegate, key);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static final String README = """
            My Girlfriend Laura - custom translations
            ==========================================

            Put a <language>.json file here (for example fr_fr.json, ja_jp.json, eo_uy.json) with the keys you want
            to add or change, in the usual Minecraft language file format:

              {
                "item.lauramod.laura_heart": "My custom name",
                "lauramod.menu.tab.home": "Home"
              }

            en_us.json is loaded first, then the file of the language selected in the game options.
            This works for the mod's own keys and for any other key of the game or of other mods.
            The full list of the mod's keys is in the mod jar: assets/lauramod/lang/en_us.json.

            Laura's spoken lines are not here: they live in config/lauramod/dialogues (see the README there).
            Changes are applied when resources are reloaded (F3 + T) or when you change language.
            """;
}
