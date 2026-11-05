package com.vyrriox.lauramod.entity;

/**
 * How Laura reacts to hostile mobs.
 *
 * @author vyrriox
 */
public enum CombatMode {
    /** Avoids monsters and never attacks. */
    PASSIVE,
    /** Attacks whatever hurts her partner or whatever her partner attacks. */
    DEFENSIVE,
    /** Defensive, and also attacks monsters that come close. */
    AGGRESSIVE;

    public static CombatMode byId(int id) {
        CombatMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : PASSIVE;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
