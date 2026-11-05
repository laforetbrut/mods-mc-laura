package com.vyrriox.lauramod.entity.work;

import java.util.Locale;

/**
 * What an assigned container is used for.
 *
 * @author vyrriox
 */
public enum ChestPurpose {
    /** Lumberjack output: logs, saplings, apples, sticks. */
    WOOD,
    /** Farmer output: crops. */
    HARVEST,
    /** Farmer input: seeds and bone meal. */
    SEEDS,
    /** Cook input: raw food and meal ingredients. */
    INGREDIENTS,
    /** Cook input: fuel for furnaces and smokers. */
    FUEL,
    /** Cook output: cooked food and meals. */
    MEALS,
    /** Her own food: she eats from here when she is hungry. */
    PANTRY,
    /** Everything else she does not know where to put. */
    STORAGE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ChestPurpose byName(String name) {
        for (ChestPurpose p : values()) {
            if (p.name().equalsIgnoreCase(name) || p.key().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
