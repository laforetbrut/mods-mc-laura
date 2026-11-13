package com.vyrriox.lauramod.client.ui;

import com.vyrriox.lauramod.config.ConfigFile;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.Mood;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.skin.AssetKind;
import dev.dryaides.minuit.EntityView;
import dev.dryaides.minuit.IconArt;

import java.nio.file.Path;
import java.util.List;

/**
 * Laura and the client around her, as the minuit menu sees them: plain values and the actions the
 * menu sends. Each Minecraft version implements it once over LauraEntity and the mod's packets, so
 * the panels in this folder are the same code on every version.
 *
 * @author vyrriox
 */
public interface LauraView {

    // ------------------------------------------------------------------ her

    String name();

    EntityView entity();

    Mood mood();

    LauraMode mode();

    CombatMode combat();

    int affection();

    float health();

    float maxHealth();

    /** Days since she joined; the first day is 0. */
    int daysTogether();

    boolean pickingUp();

    boolean gagged();

    String skinRaw();

    boolean slim();

    /** Need values 0 to 100, in the order of {@link #needKeys}. */
    int[] needs();

    String[] needKeys();

    // ------------------------------------------------------------------ her status, sent by the server

    /** The jobs she has on, as the server writes them ("LUMBERJACK,COOK"), or "". */
    String jobs();

    /** What she does now, or null. */
    String taskCurrent();

    List<String> taskQueue();

    /** The thought id of her wish ("item:minecraft:cake"), or null when she wishes nothing. */
    String desire();

    /** The wish as an item or a place icon, or null. */
    IconArt desireIcon();

    String desireText();

    long desireSeconds();

    /** Chests she knows, or -1 when the server did not say. */
    int chests();

    String owner();

    String home();

    String annoyance();

    // ------------------------------------------------------------------ her companions

    int companionCount();

    /** Her place among {@link #companionCount}, from 0. */
    int companionIndex();

    /** Switches the menu to the previous (-1) or next (1) companion. */
    void switchLaura(int dir);

    // ------------------------------------------------------------------ the server's rules

    boolean needsEnabled();

    boolean fetchEnabled();

    boolean allowUrlSkins();

    boolean allowPlayerNameSkins();

    boolean allowCustomModels();

    boolean allowModelUploads();

    boolean singleplayer();

    /** The last upload's answer, or null. */
    String uploadMessage();

    boolean uploadOk();

    // ------------------------------------------------------------------ the world and the client

    /** The block the player looked at when the menu opened, {x, y, z}, or null. */
    int[] lookedAtBlock();

    String itemName(String itemId);

    /** The atlas icon or vanilla item named like the kawaii {@code Icon} constant. */
    IconArt icon(String name);

    /** The face of a built-in skin. */
    IconArt skinFace(String builtin);

    List<String[]> serverFiles(AssetKind kind);

    List<Path> localFiles(String folder, String extension);

    List<ConfigFile.BoolValue> clientToggles();

    void saveClientConfig();

    // ------------------------------------------------------------------ actions

    void act(LauraAction action, String arg);

    void requestStatus();

    void setSkin(String ref, boolean slim);

    void setModel(String ref);

    /** A skin file of the player's skins folder: used in place on a single player world, else uploaded. */
    void useLocalSkin(Path file);

    void upload(AssetKind kind, Path file);

    /** Opens a folder of the mod's config folder; "" opens the config folder itself. */
    void openFolder(String folder);

    void sendCommand(String command);

    void close();
}
