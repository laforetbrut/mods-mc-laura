package com.vyrriox.lauramod.client.sound;

import com.vyrriox.lauramod.LauraCore;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.registry.LauraRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Lets a player silence some of Laura's sound files on their own game: muted files are taken out
 * of the sound events of the mod on this client only, so she simply picks another one (or stays
 * quiet when every file of an event is muted). The list lives in {@code sounds.muted} of the
 * client configuration and is edited from her menu (Settings, Sounds), where each file can also be
 * played to find the one to mute.
 *
 * @author vyrriox
 */
public final class LauraSounds {
    /** One sound file of the mod: {@code id} is its name, like {@code laura_ambient_3}. */
    public record Clip(String id, ResourceLocation event, Sound sound, int number) {
        public Component label() {
            return Component.translatable("lauramod.menu.sounds.clip", Component.translatable("subtitles.lauramod." + event.getPath()), number);
        }
    }

    /** Files of each live event before muting; a resource reload builds new events and drops the old ones. */
    private static final Map<WeighedSoundEvents, List<Weighted<Sound>>> ORIGINALS = new WeakHashMap<>();
    private static SoundInstance lastPreview;
    private static Field listField;
    private static boolean broken;
    private static boolean dirty = true;
    private static int ticks;

    private LauraSounds() {
    }

    public static boolean isMuted(String id) {
        return LauraClientConfig.mutedSounds.get().contains(id);
    }

    /** Mutes or unmutes a file, saves the client configuration and applies it at once. */
    public static void setMuted(String id, boolean muted) {
        Set<String> set = new LinkedHashSet<>(LauraClientConfig.mutedSounds.get());
        if (muted ? set.add(id) : set.remove(id)) {
            LauraClientConfig.mutedSounds.set(new ArrayList<>(set));
            LauraClientConfig.file().save();
        }
        dirty = true;
        apply();
    }

    /** Called every client tick: applies the list again after a resource reload or a config change. */
    public static void tick() {
        if (++ticks % 20 == 0 || dirty) {
            apply();
        }
    }

    public static void configChanged() {
        dirty = true;
    }

    /** Every sound file of the mod's events, in the order of the events, each file once. */
    public static List<Clip> clips() {
        List<Clip> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (LauraRegistries.Sound s : LauraRegistries.Sound.values()) {
            ResourceLocation event = LauraMod.id(s.id());
            WeighedSoundEvents events = manager.getSoundEvent(event);
            if (events == null) {
                continue;
            }
            int number = 0;
            for (Weighted<Sound> w : originals(events)) {
                if (w instanceof Sound sound) {
                    number++;
                    String id = idOf(sound);
                    if (seen.add(id)) {
                        out.add(new Clip(id, event, sound, number));
                    }
                }
            }
        }
        return out;
    }

    /** Plays exactly this file, even when it is muted, so the player can tell which one it is. */
    public static void preview(Clip clip) {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        if (lastPreview != null) {
            manager.stop(lastPreview);
        }
        lastPreview = new ClipInstance(clip.event(), clip.sound());
        manager.play(lastPreview);
    }

    /** How many times a file can be picked by the mod's events right now (0 when muted). For the client test. */
    public static int audibleCount(String id) {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        int count = 0;
        for (LauraRegistries.Sound s : LauraRegistries.Sound.values()) {
            WeighedSoundEvents events = manager.getSoundEvent(LauraMod.id(s.id()));
            List<Weighted<Sound>> list = events == null ? null : list(events);
            if (list != null) {
                for (Weighted<Sound> w : list) {
                    if (w instanceof Sound sound && idOf(sound).equals(id)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static String idOf(Sound sound) {
        return sound.getLocation().getPath().toLowerCase(Locale.ROOT);
    }

    private static void apply() {
        dirty = false;
        if (broken) {
            return;
        }
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Set<String> muted = new LinkedHashSet<>(LauraClientConfig.mutedSounds.get());
        for (LauraRegistries.Sound s : LauraRegistries.Sound.values()) {
            ResourceLocation event = LauraMod.id(s.id());
            WeighedSoundEvents events = manager.getSoundEvent(event);
            if (events == null) {
                continue;
            }
            List<Weighted<Sound>> list = list(events);
            if (list == null) {
                return;
            }
            List<Weighted<Sound>> kept = new ArrayList<>();
            for (Weighted<Sound> w : originals(events)) {
                if (!(w instanceof Sound sound) || !muted.contains(idOf(sound))) {
                    kept.add(w);
                }
            }
            if (kept.isEmpty()) {
                // Nothing left: she stays quiet, without the "empty sound event" warning or a subtitle.
                kept.add(SoundManager.INTENTIONALLY_EMPTY_SOUND);
            }
            if (!kept.equals(list)) {
                list.clear();
                list.addAll(kept);
            }
        }
    }

    /** The files of an event as the resource packs define them, before any muting. */
    private static List<Weighted<Sound>> originals(WeighedSoundEvents events) {
        List<Weighted<Sound>> known = ORIGINALS.get(events);
        if (known != null) {
            return known;
        }
        List<Weighted<Sound>> list = list(events);
        List<Weighted<Sound>> copy = list == null ? List.of() : List.copyOf(list);
        ORIGINALS.put(events, copy);
        return copy;
    }

    /**
     * The list of files inside a vanilla sound event. Found by its type rather than its name, which
     * differs between the development and the released game.
     */
    @SuppressWarnings("unchecked")
    private static List<Weighted<Sound>> list(WeighedSoundEvents events) {
        try {
            if (listField == null) {
                for (Field f : WeighedSoundEvents.class.getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        listField = f;
                        break;
                    }
                }
            }
            if (listField == null) {
                throw new NoSuchFieldException("no list in WeighedSoundEvents");
            }
            return (List<Weighted<Sound>>) listField.get(events);
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            LauraCore.LOGGER.warn("Muting Laura's sounds is not available in this game: {}", e.toString());
            return null;
        }
    }

    /** A sound instance that always plays the given file of its event, next to the listener. */
    private static final class ClipInstance extends AbstractSoundInstance {
        private final Sound clip;

        ClipInstance(ResourceLocation event, Sound clip) {
            super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.clip = clip;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            WeighedSoundEvents events = super.resolve(manager);
            this.sound = clip;
            return events;
        }
    }
}
