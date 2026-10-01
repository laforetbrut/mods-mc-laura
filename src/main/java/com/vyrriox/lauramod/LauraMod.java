package com.vyrriox.lauramod;

import com.mojang.logging.LogUtils;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.DialogueManager;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.platform.ClientBridge;
import com.vyrriox.lauramod.platform.Platform;
import com.vyrriox.lauramod.skin.ServerAssetStore;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * My Girlfriend Laura: common entry point shared by every loader.
 * <p>
 * Loader glue calls {@link #init(Platform)} as early as possible, then registers content through
 * {@link com.vyrriox.lauramod.registry.LauraRegistries}.
 *
 * @author vyrriox
 */
public final class LauraMod {
    public static final String MODID = "lauramod";
    public static final String NAME = "My Girlfriend Laura";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Platform platform;
    private static volatile ClientBridge clientBridge = ClientBridge.NOOP;

    private LauraMod() {
    }

    public static void init(Platform loader) {
        platform = loader;
        LOGGER.info("{} {} starting on {}", NAME, loader.modVersion(), loader.loaderName());
        LauraConfig.load();
        ServerAssetStore.init();
    }

    /**
     * Reloads every server-side file from the config folder. Called when a server starts and by
     * {@code /laura reload}. Item-based files are read here, after registries are frozen.
     */
    public static void reloadAll() {
        reload("lauramod-common.json", LauraConfig::load);
        reload("gifts.json", GiftTable::load);
        reload("desires.json", com.vyrriox.lauramod.desire.DesireTable::load);
        reload("recipes.json", com.vyrriox.lauramod.cooking.Cookbook::load);
        reload("the cooking cache", com.vyrriox.lauramod.cooking.CookingSupport::clearCache);
        reload("the dialogues", DialogueManager::reload);
        reload("the skin and model folders", ServerAssetStore::rescan);
    }

    /**
     * One file (or a script entry added to it) with a value nobody expected must not stop the others
     * from loading, the server from starting or a server tick: the previous content stays in use.
     */
    private static void reload(String what, Runnable loader) {
        try {
            loader.run();
        } catch (RuntimeException e) {
            LOGGER.error("Could not load {}, keeping what was loaded before", what, e);
        }
    }

    public static Platform platform() {
        if (platform == null) {
            throw new IllegalStateException(NAME + " platform used before init");
        }
        return platform;
    }

    public static boolean isInitialized() {
        return platform != null;
    }

    public static ClientBridge client() {
        return clientBridge;
    }

    public static void setClientBridge(ClientBridge bridge) {
        clientBridge = bridge == null ? ClientBridge.NOOP : bridge;
    }

    /** Root of every file this mod reads or writes: {@code config/lauramod}. */
    public static Path configDir() {
        return platform().configDir().resolve(MODID);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
