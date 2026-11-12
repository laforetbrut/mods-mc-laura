package com.vyrriox.lauramod.client.ui;

import java.util.Locale;

/**
 * The tabs of her menu, in rail order. Keys 1 to 7 pick them.
 *
 * @author vyrriox
 */
public enum MenuTab {
    HOME("TAB_HOME"), ORDERS("TAB_ORDERS"), EMOTES("TAB_EMOTES"), WORK("TAB_WORK"),
    FETCH("TAB_FETCH"), STYLE("TAB_STYLE"), SETTINGS("TAB_SETTINGS");

    /** Name of the kawaii atlas icon. */
    public final String icon;

    MenuTab(String icon) {
        this.icon = icon;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
