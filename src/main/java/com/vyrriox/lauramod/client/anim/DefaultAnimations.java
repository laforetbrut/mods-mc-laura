package com.vyrriox.lauramod.client.anim;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.ModelParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Animations of the default (player-like) model, from {@code assets/lauramod/animations/laura_humanoid.json}.
 * A file with the same name in {@code config/lauramod/animations/} replaces animations of the same
 * name. Values follow the vanilla model part convention: degrees added to the part rotations and
 * pixels added to the part positions (Y points down).
 *
 * @author vyrriox
 */
public final class DefaultAnimations {
    private static final String FILE = "laura_humanoid.json";
    private static volatile Map<String, ModelData.Animation> animations = Collections.emptyMap();

    private DefaultAnimations() {
    }

    public static Map<String, ModelData.Animation> get() {
        return animations;
    }

    public static synchronized void reload() {
        ModelData holder = new ModelData(64, 64);
        try (InputStream in = DefaultAnimations.class.getResourceAsStream("/assets/lauramod/animations/" + FILE)) {
            if (in != null) {
                ModelParser.parseAnimations(holder, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } else {
                LauraMod.LOGGER.error("Default animations are missing from the jar");
            }
        } catch (Exception e) {
            LauraMod.LOGGER.error("Could not read the default animations", e);
        }
        Path override = LauraMod.configDir().resolve("animations").resolve(FILE);
        if (Files.isRegularFile(override)) {
            try {
                ModelParser.parseAnimations(holder, Files.readString(override, StandardCharsets.UTF_8));
                LauraMod.LOGGER.info("Loaded custom default animations from {}", override);
            } catch (Exception e) {
                LauraMod.LOGGER.error("Custom animations {} are invalid: {}", override, e.getMessage());
            }
        }
        animations = Map.copyOf(holder.animations);
    }
}
