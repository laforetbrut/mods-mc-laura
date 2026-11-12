package com.vyrriox.lauramod.client.ui;

/**
 * What the menu remembers while it is rebuilt: the static fields last until the game closes, the
 * others as long as one menu stays open.
 *
 * @author vyrriox
 */
public final class MenuState {
    public static MenuTab lastTab = MenuTab.HOME;
    public static int workPage;
    public static int stylePage;
    public static int jobRadius = 10;
    public static int fetchCount = 16;

    public MenuTab tab;
    public String fetchItem;
    public String search = "";
    public String url = "";
    public String player = "";
    public String rename;
    public boolean focusRename;
    public boolean dismissArmed;

    public MenuState(MenuTab tab) {
        this.tab = tab;
    }
}
