package com.vyrriox.lauramod.entity;

import java.util.Locale;

/**
 * Laura's current mood, derived from her needs, her affection and recent events.
 *
 * @author vyrriox
 */
public enum Mood {
    HAPPY,
    IN_LOVE,
    NEUTRAL,
    BORED,
    HUNGRY,
    TIRED,
    SAD,
    ANGRY,
    JEALOUS,
    SULKING;

    public static Mood byId(int id) {
        Mood[] values = values();
        return id >= 0 && id < values.length ? values[id] : NEUTRAL;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** True for moods in which she tends to refuse orders. */
    public boolean isGrumpy() {
        return this == SAD || this == ANGRY || this == JEALOUS || this == SULKING || this == HUNGRY;
    }
}
