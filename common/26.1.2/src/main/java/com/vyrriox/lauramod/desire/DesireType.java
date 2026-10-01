package com.vyrriox.lauramod.desire;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * The kinds of desires and the fixed list of activities.
 *
 * @author vyrriox
 */
public final class DesireType {
    private DesireType() {
    }

    public enum Kind {
        ITEM, PLACE, ACTIVITY;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Activities Laura can crave. Detection lives in {@link com.vyrriox.lauramod.entity.brain.LauraBrain}. */
    public enum Activity {
        DANCE(Items.JUKEBOX, 0),
        HUG(Items.PINK_WOOL, 0),
        KISS(Items.PINK_DYE, 0),
        COMPLIMENT(Items.WRITABLE_BOOK, 0),
        SLEEP_TOGETHER(Items.PINK_BED, 0),
        SUNSET(Items.CLOCK, 20),
        STARGAZE(Items.SPYGLASS, 30),
        MUSIC(Items.MUSIC_DISC_CAT, 20),
        BOAT_RIDE(Items.OAK_BOAT, 15),
        SWIM(Items.WATER_BUCKET, 8),
        PET(Items.BONE, 5),
        NEW_OUTFIT(Items.LEATHER_CHESTPLATE, 0),
        FIREWORKS(Items.FIREWORK_ROCKET, 0),
        CAMPFIRE(Items.CAMPFIRE, 20),
        FLOWERS(Items.PEONY, 0),
        WALK(Items.LEATHER_BOOTS, 0);

        /** Icon shown in her thought bubble. */
        public final Item icon;
        /** Seconds the condition must hold (0 = instant event). */
        public final int seconds;

        Activity(Item icon, int seconds) {
            this.icon = icon;
            this.seconds = seconds;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Activity byName(String name) {
            for (Activity a : values()) {
                if (a.name().equalsIgnoreCase(name) || a.key().equals(name)) {
                    return a;
                }
            }
            return null;
        }
    }
}
