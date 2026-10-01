package com.vyrriox.lauramod.entity.brain;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

/**
 * Laura's five needs, from 0 (desperate) to 100 (satisfied).
 *
 * @author vyrriox
 */
public final class Needs {
    public enum Need {
        HUNGER, ENERGY, FUN, ATTENTION, HYGIENE;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final float MAX = 100F;

    private final float[] values = {85F, 90F, 75F, 75F, 95F};

    public float get(Need need) {
        return values[need.ordinal()];
    }

    public void set(Need need, float value) {
        values[need.ordinal()] = Math.max(0F, Math.min(MAX, value));
    }

    public void add(Need need, float delta) {
        set(need, get(need) + delta);
    }

    public void fillAll() {
        for (Need need : Need.values()) {
            set(need, MAX);
        }
    }

    /** The need with the lowest value. */
    public Need lowest() {
        Need lowest = Need.HUNGER;
        for (Need need : Need.values()) {
            if (get(need) < get(lowest)) {
                lowest = need;
            }
        }
        return lowest;
    }

    public float average() {
        float sum = 0;
        for (float v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /** Packs the five needs, rounded to 0..100, for entity data sync (7 bits each). */
    public long pack() {
        long packed = 0;
        for (int i = 0; i < values.length; i++) {
            packed |= ((long) Math.round(values[i]) & 0x7F) << (i * 7);
        }
        return packed;
    }

    /** Reads one need from a packed value. */
    public static int unpack(long packed, Need need) {
        return (int) ((packed >> (need.ordinal() * 7)) & 0x7F);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Need need : Need.values()) {
            tag.putFloat(need.name(), get(need));
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Need need : Need.values()) {
            if (tag.contains(need.name())) {
                set(need, tag.getFloat(need.name()));
            }
        }
    }
}
