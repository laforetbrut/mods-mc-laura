package com.vyrriox.lauramod.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vyrriox.lauramod.LauraCore;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.client.ui.LauraPanel;
import com.vyrriox.lauramod.client.ui.LauraTheme;
import com.vyrriox.lauramod.client.ui.LauraView;
import com.vyrriox.lauramod.client.ui.MenuState;
import com.vyrriox.lauramod.client.ui.MenuTab;
import com.vyrriox.lauramod.config.ConfigFile;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.Mood;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.util.TextCodec;
import dev.dryaides.minuit.EntityView;
import dev.dryaides.minuit.IconArt;
import dev.dryaides.minuit.ItemLookup;
import dev.dryaides.minuit.PanelComponent;
import dev.dryaides.minuit.client.MinuitScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Laura's menu: tabs to see how she feels and to give her every order the commands and the chat
 * can give. Opened with the menu key (K by default) or by right clicking her. The tabs are drawn by
 * {@link LauraPanel} on minuit; this screen feeds it her state and rebuilds it when that changes.
 *
 * @author vyrriox
 */
public class LauraMenuScreen extends MinuitScreen {
    public enum Tab {
        HOME, ORDERS, EMOTES, WORK, FETCH, STYLE, SETTINGS
    }

    private LauraEntity laura;
    private final MenuState state;
    private final BlockPos lookedAtBlock;
    private final LauraView view = new View();
    private LauraPanel menu;
    private String builtSignature = "";
    private boolean rebuildPending;
    private int ticks;

    public LauraMenuScreen(LauraEntity laura) {
        this(laura, Tab.values()[MenuState.lastTab.ordinal()]);
    }

    public LauraMenuScreen(LauraEntity laura, Tab tab) {
        super("lauramod.menu.title");
        this.laura = laura;
        this.state = new MenuState(MenuTab.values()[tab.ordinal()]);
        HitResult hit = Minecraft.getInstance().hitResult;
        this.lookedAtBlock = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block.getBlockPos().immutable() : null;
        LauraClientNetwork.requestStatus(laura.getId());
        LauraClientNetwork.requestList(AssetKind.SKIN);
        LauraClientNetwork.requestList(AssetKind.MODEL);
    }

    @Override
    protected PanelComponent build(int screenWidth, int screenHeight) {
        // The tab rail stands out of the panel's left edge: centre both together.
        int left = (screenWidth - LauraPanel.W) / 2 + 9;
        int top = (screenHeight - LauraPanel.H) / 2;
        menu = new LauraPanel(LauraTheme.get(), words(), left, top, view, state, () -> rebuildPending = true);
        builtSignature = menu.signature();
        return menu;
    }

    private void rebuild() {
        if (menu != null) {
            menu.remember();
        }
        rebuildWidgets();
    }

    // ------------------------------------------------------------------ server updates

    /** A new status arrived: refresh what depends on it, unless the player is typing. */
    public void onStatus(int entityId) {
        if (entityId != laura.getId() || menu == null || menu.typing()) {
            return;
        }
        menu.refreshQueue();
        refreshIfChanged();
    }

    private void refreshIfChanged() {
        if (menu != null && !menu.typing() && !menu.signature().equals(builtSignature)) {
            rebuildPending = true;
        }
    }

    /** The server sent a list of skin or model files. */
    public void onAssetList() {
        if (state.tab == MenuTab.STYLE && menu != null && !menu.typing()) {
            rebuildPending = true;
        }
    }

    // ------------------------------------------------------------------ screen

    @Override
    public void tick() {
        super.tick();
        if (!laura.isAlive() || laura.isRemoved()) {
            LauraEntity other = LauraClient.nearestOwned(64);
            if (other == null) {
                onClose();
                return;
            }
            laura = other;
            rebuildPending = true;
        }
        if (++ticks % 40 == 0) {
            LauraClientNetwork.requestStatus(laura.getId());
        }
        if (ticks % 5 == 0) {
            refreshIfChanged();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Rebuilt here rather than inside a click, so the panel is never replaced while it handles one.
        if (rebuildPending) {
            rebuildPending = false;
            rebuild();
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean typing = menu != null && menu.typing();
        if (!typing && LauraClient.MENU_KEY.matches(event)) {
            onClose();
            return true;
        }
        if (!typing && key >= 49 && key <= 55) {
            // Keys 1 to 7 switch tabs.
            state.tab = MenuTab.values()[key - 49];
            rebuildPending = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        for (Path file : files) {
            String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".png")) {
                view.upload(AssetKind.SKIN, file);
            } else if (n.endsWith(".bbmodel")) {
                view.upload(AssetKind.MODEL, file);
            } else if (n.endsWith(".geo.json")) {
                // A Bedrock model needs its texture file next to it: it cannot travel as one file.
                ClientState.lastUploadMessage = Component.translatable("lauramod.upload.bbmodel_only");
                ClientState.lastUploadSuccess = false;
            }
        }
    }

    // ------------------------------------------------------------------ her, for the panel

    /** Companions the player can switch between, in a stable order. */
    private static List<LauraEntity> companions() {
        List<LauraEntity> list = new ArrayList<>(LauraClient.ownedNearby(64));
        list.sort(Comparator.comparingInt(LauraEntity::getId));
        return list;
    }

    private JsonObject status() {
        JsonObject s = ClientState.status(laura.getId());
        return s == null ? new JsonObject() : s;
    }

    private String statusText(JsonElement element) {
        if (minecraft == null || minecraft.level == null) {
            return "?";
        }
        try {
            return TextCodec.fromJson(element.getAsString(), minecraft.level.registryAccess()).getString();
        } catch (RuntimeException e) {
            return "?";
        }
    }

    private String statusString(String key) {
        JsonObject s = status();
        return s.has(key) ? s.get(key).getAsString() : null;
    }

    private final class View implements LauraView {
        @Override
        public String name() {
            return laura.getLauraName();
        }

        @Override
        public EntityView entity() {
            return new EntityView("lauramod:laura", laura, false, false);
        }

        @Override
        public Mood mood() {
            return laura.getMood();
        }

        @Override
        public LauraMode mode() {
            return laura.getMode();
        }

        @Override
        public CombatMode combat() {
            return laura.getCombatMode();
        }

        @Override
        public int affection() {
            return laura.getAffection();
        }

        @Override
        public float health() {
            return laura.getHealth();
        }

        @Override
        public float maxHealth() {
            return laura.getMaxHealth();
        }

        @Override
        public int daysTogether() {
            return laura.daysTogether();
        }

        @Override
        public boolean pickingUp() {
            return laura.isPickingUpItems();
        }

        @Override
        public boolean gagged() {
            return laura.isGagged();
        }

        @Override
        public String skinRaw() {
            return laura.getSkinRaw();
        }

        @Override
        public boolean slim() {
            return laura.isSlim();
        }

        @Override
        public int[] needs() {
            long packed = laura.getPackedNeeds();
            Needs.Need[] all = Needs.Need.values();
            int[] values = new int[all.length];
            for (int i = 0; i < all.length; i++) {
                values[i] = Needs.unpack(packed, all[i]);
            }
            return values;
        }

        @Override
        public String[] needKeys() {
            Needs.Need[] all = Needs.Need.values();
            String[] keys = new String[all.length];
            for (int i = 0; i < all.length; i++) {
                keys[i] = all[i].key();
            }
            return keys;
        }

        @Override
        public String jobs() {
            String jobs = statusString("jobs");
            return jobs == null ? "" : jobs;
        }

        @Override
        public String taskCurrent() {
            JsonObject s = status();
            return s.has("taskCurrent") ? statusText(s.get("taskCurrent")) : null;
        }

        @Override
        public List<String> taskQueue() {
            List<String> rows = new ArrayList<>();
            JsonObject s = status();
            if (s.has("taskQueue")) {
                for (JsonElement e : s.getAsJsonArray("taskQueue")) {
                    rows.add(statusText(e));
                }
            }
            return rows;
        }

        @Override
        public String desire() {
            return statusString("desire");
        }

        @Override
        public IconArt desireIcon() {
            String desire = desire();
            ItemStack icon = desire == null ? ItemStack.EMPTY : LauraRenderer.thoughtIcon(desire);
            return icon.isEmpty() ? null : KawaiiIcons.item(icon);
        }

        @Override
        public String desireText() {
            JsonObject s = status();
            return s.has("desireText") ? statusText(s.get("desireText")) : null;
        }

        @Override
        public long desireSeconds() {
            JsonObject s = status();
            return s.has("desireSeconds") ? s.get("desireSeconds").getAsLong() : 0;
        }

        @Override
        public int chests() {
            JsonObject s = status();
            return s.has("chests") ? s.get("chests").getAsInt() : -1;
        }

        @Override
        public String owner() {
            return statusString("owner");
        }

        @Override
        public String home() {
            return statusString("home");
        }

        @Override
        public String annoyance() {
            return statusString("annoyance");
        }

        @Override
        public int companionCount() {
            return companions().size();
        }

        @Override
        public int companionIndex() {
            return companions().indexOf(laura);
        }

        @Override
        public void switchLaura(int dir) {
            List<LauraEntity> mine = companions();
            if (mine.isEmpty()) {
                return;
            }
            laura = mine.get(Math.floorMod(mine.indexOf(laura) + dir, mine.size()));
            LauraClientNetwork.requestStatus(laura.getId());
            state.dismissArmed = false;
            rebuildPending = true;
        }

        @Override
        public boolean needsEnabled() {
            return ClientState.needsEnabled;
        }

        @Override
        public boolean fetchEnabled() {
            return ClientState.fetchEnabled;
        }

        @Override
        public boolean allowUrlSkins() {
            return ClientState.allowUrlSkins;
        }

        @Override
        public boolean allowPlayerNameSkins() {
            return ClientState.allowPlayerNameSkins;
        }

        @Override
        public boolean allowCustomModels() {
            return ClientState.allowCustomModels;
        }

        @Override
        public boolean allowModelUploads() {
            return ClientState.allowModelUploads;
        }

        @Override
        public boolean singleplayer() {
            return minecraft != null && minecraft.hasSingleplayerServer();
        }

        @Override
        public String uploadMessage() {
            return ClientState.lastUploadMessage == null ? null : ClientState.lastUploadMessage.getString();
        }

        @Override
        public boolean uploadOk() {
            return ClientState.lastUploadSuccess;
        }

        @Override
        public int[] lookedAtBlock() {
            return lookedAtBlock == null ? null : new int[]{lookedAtBlock.getX(), lookedAtBlock.getY(), lookedAtBlock.getZ()};
        }

        @Override
        public String itemName(String itemId) {
            return ItemLookup.game().name(itemId);
        }

        @Override
        public IconArt icon(String name) {
            return KawaiiIcons.of(name);
        }

        @Override
        public IconArt skinFace(String builtin) {
            return KawaiiIcons.face(SkinTextures.builtin(builtin).toString(), 20);
        }

        @Override
        public List<String[]> serverFiles(AssetKind kind) {
            return ClientState.assetList(kind);
        }

        @Override
        public List<Path> localFiles(String folder, String extension) {
            List<Path> out = new ArrayList<>();
            Path dir = LauraMod.configDir().resolve(folder);
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)).sorted().forEach(out::add);
                } catch (Exception e) {
                    LauraCore.LOGGER.debug("Could not list {}", dir, e);
                }
            }
            return out;
        }

        @Override
        public List<ConfigFile.BoolValue> clientToggles() {
            return List.of(LauraClientConfig.speechBubbles, LauraClientConfig.thoughtBubbles, LauraClientConfig.animations,
                    LauraClientConfig.particles, LauraClientConfig.customModels, LauraClientConfig.remoteSkins, LauraClientConfig.menuOnRightClick,
                    LauraClientConfig.showNeedsHud);
        }

        @Override
        public void saveClientConfig() {
            LauraClientConfig.file().save();
        }

        @Override
        public void act(LauraAction action, String arg) {
            LauraClientNetwork.action(laura.getId(), action, arg);
        }

        @Override
        public void requestStatus() {
            LauraClientNetwork.requestStatus(laura.getId());
        }

        @Override
        public void setSkin(String ref, boolean slim) {
            LauraClientNetwork.setSkin(laura.getId(), ref, slim);
        }

        @Override
        public void setModel(String ref) {
            LauraClientNetwork.setModel(laura.getId(), ref);
        }

        @Override
        public void useLocalSkin(Path file) {
            String name = file.getFileName().toString();
            if (singleplayer()) {
                // Same folder as the integrated server: no upload needed.
                setSkin("server:" + name.substring(0, name.length() - 4), laura.isSlim());
                return;
            }
            upload(AssetKind.SKIN, file);
        }

        @Override
        public void upload(AssetKind kind, Path file) {
            boolean allowed = kind == AssetKind.SKIN ? ClientState.allowSkinUploads : ClientState.allowModelUploads;
            if (!allowed) {
                ClientState.lastUploadMessage = Component.translatable("lauramod.upload.disabled");
                ClientState.lastUploadSuccess = false;
                return;
            }
            try {
                LauraClientNetwork.upload(kind, file.getFileName().toString(), Files.readAllBytes(file), laura.getId(), laura.isSlim());
            } catch (Exception e) {
                LauraCore.LOGGER.warn("Could not read {}", file);
            }
        }

        @Override
        public void openFolder(String folder) {
            Path dir = folder.isEmpty() ? LauraMod.configDir() : LauraMod.configDir().resolve(folder);
            Util.getPlatform().openFile(dir.toFile());
        }

        @Override
        public void sendCommand(String command) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.connection.sendCommand(command);
            }
        }

        @Override
        public void close() {
            onClose();
        }
    }
}
