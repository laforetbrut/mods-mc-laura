package com.vyrriox.lauramod.entity;

import java.util.Locale;

/**
 * Work Laura can do on her own.
 *
 * @author vyrriox
 */
public enum LauraJob {
    NONE,
    /** Fells natural trees, replants saplings, stores the wood. */
    LUMBERJACK,
    /** Harvests and replants the crops of a field, stores the harvest. */
    FARMER,
    /** Cooks raw food in furnaces, smokers and campfires, prepares meals, stores them. */
    COOK;

    public static LauraJob byId(int id) {
        LauraJob[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static LauraJob byName(String name) {
        for (LauraJob job : values()) {
            if (job.name().equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
