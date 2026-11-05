package com.vyrriox.lauramod.entity;

import java.util.Locale;

/**
 * What Laura is currently told to do.
 *
 * @author vyrriox
 */
public enum LauraMode {
    /** Follows her partner everywhere. */
    FOLLOW,
    /** Stays exactly where she is, sitting. */
    STAY,
    /** Goes home and lives there. */
    HOME,
    /** Walks around freely near where she was left. */
    WANDER,
    /** Works in her work area (see {@link LauraJob}). */
    WORK;

    public static LauraMode byId(int id) {
        LauraMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : FOLLOW;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
