package com.vyrriox.lauramod.skin;

/**
 * Files the server can share with clients: skins (PNG) and models (Blockbench).
 *
 * @author vyrriox
 */
public enum AssetKind {
    SKIN("skins"),
    MODEL("models");

    public final String folder;

    AssetKind(String folder) {
        this.folder = folder;
    }

    public static AssetKind byId(int id) {
        AssetKind[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
