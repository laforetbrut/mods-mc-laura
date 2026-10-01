package com.vyrriox.lauramod.config;

import com.vyrriox.lauramod.LauraMod;

/**
 * Client-only settings, stored in {@code config/lauramod/lauramod-client.json}.
 *
 * @author vyrriox
 */
public final class LauraClientConfig {
    private static ConfigFile file;

    public static ConfigFile.BoolValue speechBubbles;
    public static ConfigFile.IntValue bubbleSeconds;
    public static ConfigFile.BoolValue thoughtBubbles;
    public static ConfigFile.BoolValue animations;
    public static ConfigFile.BoolValue customModels;
    public static ConfigFile.BoolValue remoteSkins;
    public static ConfigFile.BoolValue particles;
    public static ConfigFile.BoolValue menuOnRightClick;
    public static ConfigFile.BoolValue showNeedsHud;
    public static ConfigFile.IntValue maxSkinDownloadKb;
    public static ConfigFile.IntValue maxModelDownloadKb;

    private LauraClientConfig() {
    }

    public static synchronized void load() {
        if (file == null) {
            file = build();
        }
        file.load();
    }

    public static ConfigFile file() {
        if (file == null) {
            load();
        }
        return file;
    }

    private static ConfigFile build() {
        ConfigFile f = new ConfigFile(LauraMod.configDir().resolve("lauramod-client.json"),
                "My Girlfriend Laura - client configuration (only affects your game).");

        ConfigFile.Section display = f.section("display", "What you see.");
        speechBubbles = display.bool("speechBubbles", true, "Show what Laura says in a bubble above her head.");
        bubbleSeconds = display.integer("bubbleSeconds", 6, 1, 60, "How long a speech bubble stays visible.");
        thoughtBubbles = display.bool("thoughtBubbles", true, "Show what she desires (an item, a place, an activity) above her head.");
        animations = display.bool("animations", true, "Play her animations (dances, emotes, idle movements).");
        customModels = display.bool("customModels", true, "Render custom Blockbench models. If false she always uses the default model.");
        remoteSkins = display.bool("remoteSkins", true, "Download skins from the internet. If false, URL skins show the default skin.");
        particles = display.bool("particles", true, "Hearts, tears, sparkles and other particles.");
        showNeedsHud = display.bool("showNeedsHud", false, "Show her needs in a small overlay while she is near you.");

        ConfigFile.Section controls = f.section("controls", "Interaction.");
        menuOnRightClick = controls.bool("menuOnRightClick", true, "Right click opens her menu. Sneak + right click always opens her inventory.");

        ConfigFile.Section network = f.section("network", "Downloads.");
        maxSkinDownloadKb = network.integer("maxSkinDownloadKb", 512, 8, 8192, "Largest skin you accept to download, in kilobytes.");
        maxModelDownloadKb = network.integer("maxModelDownloadKb", 4096, 16, 32768, "Largest model you accept to receive from a server, in kilobytes.");
        return f;
    }
}
