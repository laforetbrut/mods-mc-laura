package com.vyrriox.lauramod.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.kawaii.FloatingHearts;
import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.gui.kawaii.KButton;
import com.vyrriox.lauramod.client.gui.kawaii.KDraw;
import com.vyrriox.lauramod.client.gui.kawaii.KList;
import com.vyrriox.lauramod.client.gui.kawaii.KSlider;
import com.vyrriox.lauramod.client.gui.kawaii.KToggle;
import com.vyrriox.lauramod.client.gui.kawaii.KWidget;
import com.vyrriox.lauramod.client.gui.kawaii.Kawaii;
import com.vyrriox.lauramod.client.network.LauraClientNetwork;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.config.ConfigFile;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.SkinRef;
import com.vyrriox.lauramod.util.TextCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Laura's menu: a pastel screen with tabs to see how she feels and to give her every order the
 * commands and the chat can give. Opened with the menu key (K by default) or by right clicking her.
 *
 * @author vyrriox
 */
public class LauraMenuScreen extends Screen {
    public enum Tab {
        HOME(Icon.TAB_HOME), ORDERS(Icon.TAB_ORDERS), EMOTES(Icon.TAB_EMOTES), WORK(Icon.TAB_WORK),
        FETCH(Icon.TAB_FETCH), STYLE(Icon.TAB_STYLE), SETTINGS(Icon.TAB_SETTINGS);

        final Icon icon;

        Tab(Icon icon) {
            this.icon = icon;
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int W = 320;
    private static final int H = 216;
    private static final int CONTENT_H = 164;
    /** Four rows of 18 px items plus the grid padding. */
    private static final int FETCH_GRID_H = 78;
    private static final int[] FETCH_COUNTS = {1, 8, 16, 32, 64};
    private static final int FETCH_CHIP = 24;
    private static Tab lastTab = Tab.HOME;
    private static int workPage;
    private static int stylePage;
    private static int jobRadius = 10;
    private static int fetchCount = 16;

    private LauraEntity laura;
    private Tab tab;
    private final List<KWidget> widgets = new ArrayList<>();
    private final FloatingHearts hearts = new FloatingHearts();
    private final BlockPos lookedAtBlock;
    private int left;
    private int top;
    private long lastFrame = Util.getMillis();
    private int ticks;
    private boolean focusRename;
    private boolean dismissArmed;
    private EditBox renameBox;
    private EditBox urlBox;
    private EditBox playerBox;
    private ItemGrid itemGrid;
    private KList<Component> queueList;
    private Item fetchItem;
    private String searchText = "";
    private String urlText = "";
    private String playerText = "";
    private String builtSignature = "";

    public LauraMenuScreen(LauraEntity laura) {
        this(laura, lastTab);
    }

    public LauraMenuScreen(LauraEntity laura, Tab tab) {
        super(Component.translatable("lauramod.menu.title"));
        this.laura = laura;
        this.tab = tab;
        HitResult hit = Minecraft.getInstance().hitResult;
        this.lookedAtBlock = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block.getBlockPos().immutable() : null;
        LauraClientNetwork.requestStatus(laura.getId());
        LauraClientNetwork.requestList(AssetKind.SKIN);
        LauraClientNetwork.requestList(AssetKind.MODEL);
    }

    // ------------------------------------------------------------------ server updates

    /** A new status arrived: refresh what depends on it, unless the player is typing. */
    public void onStatus(int entityId) {
        if (entityId != laura.getId() || getFocused() instanceof EditBox) {
            return;
        }
        if (queueList != null) {
            queueList.setItems(queueRows());
        }
        refreshIfChanged();
    }

    /** What the buttons of the current tab show; the tab is rebuilt when it changes. */
    private String signature() {
        JsonObject s = status();
        return laura.getMode() + "|" + laura.getCombatMode() + "|" + laura.isPickingUpItems() + "|" + laura.isGagged() + "|"
                + (s.has("jobs") ? s.get("jobs").getAsString() : "") + "|" + laura.getSkinRaw() + "|" + laura.isSlim();
    }

    private void refreshIfChanged() {
        if (!(getFocused() instanceof EditBox) && !signature().equals(builtSignature)) {
            rebuild();
        }
    }

    /** The server sent a list of skin or model files. */
    public void onAssetList() {
        if (tab == Tab.STYLE && !(getFocused() instanceof EditBox)) {
            rebuild();
        }
    }

    // ------------------------------------------------------------------ status helpers

    private JsonObject status() {
        JsonObject s = ClientState.status(laura.getId());
        return s == null ? new JsonObject() : s;
    }

    private Component statusText(JsonElement element) {
        if (minecraft == null || minecraft.level == null) {
            return Component.literal("?");
        }
        try {
            return TextCodec.fromJson(element.getAsString(), minecraft.level.registryAccess());
        } catch (RuntimeException e) {
            return Component.literal("?");
        }
    }

    /** Companions the player can switch between, in a stable order. */
    private static List<LauraEntity> companions() {
        List<LauraEntity> list = new ArrayList<>(LauraClient.ownedNearby(64));
        list.sort(Comparator.comparingInt(LauraEntity::getId));
        return list;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        widgets.clear();
        clearWidgets();
        renameBox = null;
        urlBox = null;
        playerBox = null;
        itemGrid = null;
        queueList = null;
        lastTab = tab;
        builtSignature = signature();

        KButton close = new KButton(left + W - 22, top + 9, 16, 16, null, c -> onClose()).icon(Icon.CLOSE).style(KButton.Style.GHOST);
        close.tooltip(Component.translatable("lauramod.menu.close"));
        widgets.add(close);
        List<LauraEntity> mine = companions();
        if (mine.size() > 1) {
            KButton prev = new KButton(left + W - 78, top + 9, 16, 16, null, c -> switchLaura(-1)).icon(Icon.PREV).style(KButton.Style.GHOST);
            prev.tooltip(Component.translatable("lauramod.menu.previous"));
            KButton next = new KButton(left + W - 40, top + 9, 16, 16, null, c -> switchLaura(1)).icon(Icon.NEXT).style(KButton.Style.GHOST);
            next.tooltip(Component.translatable("lauramod.menu.next"));
            widgets.add(prev);
            widgets.add(next);
        }
        KButton pencil = new KButton(left + 34 + nameWidth() + 4, top + 7, 14, 14, null, c -> {
            tab = Tab.SETTINGS;
            focusRename = true;
            rebuild();
        }).icon(Icon.PENCIL).style(KButton.Style.GHOST);
        pencil.tooltip(Component.translatable("lauramod.menu.rename"));
        widgets.add(pencil);

        int ty = top + 36;
        for (Tab t : Tab.values()) {
            KButton button = new KButton(left + 7, ty, 24, 22, null, c -> {
                tab = t;
                rebuild();
            }).icon(t.icon).style(KButton.Style.LAVENDER).selected(t == tab);
            button.tooltip(Component.translatable("lauramod.menu.tab." + t.key()));
            widgets.add(button);
            ty += 23;
        }
        switch (tab) {
            case HOME -> buildHome();
            case ORDERS -> buildOrders();
            case EMOTES -> buildEmotes();
            case WORK -> buildWork();
            case FETCH -> buildFetch();
            case STYLE -> buildStyle();
            case SETTINGS -> buildSettings();
        }
    }

    private void switchLaura(int dir) {
        List<LauraEntity> mine = companions();
        if (mine.isEmpty()) {
            return;
        }
        int index = mine.indexOf(laura);
        laura = mine.get(Math.floorMod(index + dir, mine.size()));
        LauraClientNetwork.requestStatus(laura.getId());
        dismissArmed = false;
        rebuild();
    }

    private int nameWidth() {
        return Math.round(font.width(laura.getLauraName()) * 1.25F);
    }

    private int cx0() {
        return left + 38;
    }

    private int cy0() {
        return top + 34;
    }

    private int cw() {
        return W - 46;
    }

    private void act(LauraAction action, String arg) {
        LauraClientNetwork.action(laura.getId(), action, arg);
    }

    private KButton add(KButton button) {
        widgets.add(button);
        return button;
    }

    private KButton button(int x, int y, int w, int h, String key, Icon icon, KButton.Style style, Consumer<KButton.Click> action) {
        KButton b = new KButton(x, y, w, h, key == null ? null : Component.translatable(key), action).style(style);
        if (icon != null) {
            b.icon(icon);
        }
        return add(b);
    }

    private KButton iconButton(int x, int y, int w, int h, String tooltipKey, Icon icon, KButton.Style style, Consumer<KButton.Click> action) {
        KButton b = button(x, y, w, h, null, icon, style, action);
        b.tooltip(Component.translatable(tooltipKey));
        return b;
    }

    private EditBox box(int x, int y, int w, String hint, int maxLength) {
        EditBox box = new EditBox(font, x + 4, y + 5, w - 8, 10, Component.translatable(hint));
        box.setHint(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
        box.setBordered(false);
        box.setMaxLength(maxLength);
        box.setTextColor(Kawaii.PLUM);
        addRenderableWidget(box);
        return box;
    }

    // ------------------------------------------------------------------ tab: home

    private void buildHome() {
        int x = cx0() + 96;
        int y = cy0() + 138;
        int bw = (cw() - 96 - 4 * 3) / 5;
        iconButton(x, y, bw, 22, "lauramod.menu.hug", Icon.HUG, KButton.Style.PINK, c -> act(LauraAction.HUG, ""));
        iconButton(x + (bw + 3), y, bw, 22, "lauramod.menu.kiss", Icon.KISS, KButton.Style.PINK, c -> act(LauraAction.KISS, ""));
        iconButton(x + (bw + 3) * 2, y, bw, 22, "lauramod.menu.compliment", Icon.COMPLIMENT, KButton.Style.PINK, c -> act(LauraAction.COMPLIMENT, ""));
        iconButton(x + (bw + 3) * 3, y, bw, 22, "lauramod.menu.inventory", Icon.INVENTORY, KButton.Style.LAVENDER, c -> {
            act(LauraAction.INVENTORY, "");
            onClose();
        });
        iconButton(x + (bw + 3) * 4, y, bw, 22, "lauramod.menu.info", Icon.TAB_INFO, KButton.Style.LAVENDER, c -> act(LauraAction.INFO, ""));
    }

    // ------------------------------------------------------------------ tab: orders

    private void buildOrders() {
        record Order(String key, Icon icon, LauraAction action, KButton.Style style) {
        }
        List<Order> orders = List.of(
                new Order("lauramod.menu.follow", Icon.FOLLOW, LauraAction.FOLLOW, KButton.Style.PINK),
                new Order("lauramod.menu.stay", Icon.STAY, LauraAction.STAY, KButton.Style.PINK),
                new Order("lauramod.menu.wander", Icon.WANDER, LauraAction.WANDER, KButton.Style.PINK),
                new Order("lauramod.menu.come", Icon.COME, LauraAction.COME, KButton.Style.LAVENDER),
                new Order("lauramod.menu.go_home", Icon.GO_HOME, LauraAction.HOME, KButton.Style.LAVENDER),
                new Order("lauramod.menu.set_home", Icon.SET_HOME, LauraAction.SET_HOME, KButton.Style.LAVENDER),
                new Order("lauramod.menu.sleep", Icon.SLEEP, LauraAction.SLEEP, KButton.Style.SKY),
                new Order("lauramod.menu.wake_up", Icon.WAKE, LauraAction.WAKE_UP, KButton.Style.SKY),
                new Order("lauramod.menu.eat", Icon.EAT, LauraAction.EAT, KButton.Style.SKY),
                new Order("lauramod.menu.stop", Icon.STOP, LauraAction.STOP, KButton.Style.PEACH),
                new Order("lauramod.menu.ungag", Icon.UNGAG, LauraAction.UNGAG, KButton.Style.PEACH),
                new Order("lauramod.menu.clear_home", Icon.TRASH, LauraAction.CLEAR_HOME, KButton.Style.PEACH));
        int x = cx0();
        int y = cy0();
        int bw = (cw() - 8) / 3;
        LauraMode mode = laura.getMode();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            KButton b = button(x + (i % 3) * (bw + 4), y + (i / 3) * 25, bw, 22, o.key(), o.icon(), o.style(), c -> act(o.action(), ""));
            if (o.action() == LauraAction.UNGAG) {
                b.active = laura.isGagged();
            }
            b.selected = o.action() == LauraAction.FOLLOW && mode == LauraMode.FOLLOW
                    || o.action() == LauraAction.STAY && mode == LauraMode.STAY
                    || o.action() == LauraAction.WANDER && mode == LauraMode.WANDER
                    || o.action() == LauraAction.HOME && mode == LauraMode.HOME;
        }
        int cy = y + 4 * 25 + 12;
        int chipW = (cw() - 8) / 3;
        for (CombatMode combat : CombatMode.values()) {
            KButton chip = button(x + combat.ordinal() * (chipW + 4), cy, chipW, 18, "lauramod.combat." + combat.key(),
                    combat == CombatMode.PASSIVE ? Icon.COMBAT_PASSIVE : Icon.COMBAT_FIGHT, KButton.Style.MINT, c -> act(LauraAction.COMBAT, combat.name()));
            chip.selected = laura.getCombatMode() == combat;
            chip.tooltip(Component.translatable("lauramod.combat." + combat.key() + ".help"));
        }
        widgets.add(new KToggle(x, cy + 24, cw(), Component.translatable("lauramod.menu.pickup"), laura.isPickingUpItems(),
                v -> act(LauraAction.PICKUP, v ? "on" : "off")));
    }

    // ------------------------------------------------------------------ tab: emotes

    private void buildEmotes() {
        List<Emote> emotes = new ArrayList<>();
        for (Emote e : Emote.values()) {
            if (e.playerTriggered) {
                emotes.add(e);
            }
        }
        int cols = 6;
        int rows = (emotes.size() + cols - 1) / cols;
        int bw = (cw() - (cols - 1) * 3) / cols;
        int bh = Math.min(30, (CONTENT_H - 12 - (rows - 1) * 2) / rows);
        for (int i = 0; i < emotes.size(); i++) {
            Emote e = emotes.get(i);
            Component name = Component.translatable("lauramod.emote." + e.animationName());
            KButton b = new KButton(cx0() + (i % cols) * (bw + 3), cy0() + (i / cols) * (bh + 2), bw, bh, name, c -> act(LauraAction.EMOTE, e.name()))
                    .icon(Icon.emote(e)).vertical().style(i % 2 == 0 ? KButton.Style.PINK : KButton.Style.LAVENDER);
            b.tooltip(name);
            widgets.add(b);
        }
    }

    // ------------------------------------------------------------------ tab: work

    private void buildWork() {
        String[] pages = {"jobs", "errands", "chests"};
        int pw = (cw() - 8) / 3;
        for (int i = 0; i < pages.length; i++) {
            int page = i;
            button(cx0() + i * (pw + 4), cy0(), pw, 16, "lauramod.menu.work." + pages[i], null, KButton.Style.LAVENDER, c -> {
                workPage = page;
                rebuild();
            }).selected(workPage == i);
        }
        int x = cx0();
        int y = cy0() + 22;
        switch (workPage) {
            case 0 -> buildJobs(x, y);
            case 1 -> buildErrands(x, y);
            default -> buildChests(x, y);
        }
    }

    private void buildJobs(int x, int y) {
        String jobs = status().has("jobs") ? status().get("jobs").getAsString() : "";
        int jw = (cw() - 8) / 3;
        LauraJob[] list = {LauraJob.LUMBERJACK, LauraJob.FARMER, LauraJob.COOK};
        for (int i = 0; i < list.length; i++) {
            LauraJob job = list[i];
            boolean on = jobs.contains(job.name());
            KButton b = button(x + i * (jw + 4), y, jw, 40, "lauramod.job." + job.key(), Icon.job(job), on ? KButton.Style.MINT : KButton.Style.PINK, c -> {
                act(LauraAction.JOB, job.name() + (on ? ":off" : ":on:" + jobRadius));
                LauraClientNetwork.requestStatus(laura.getId());
            }).vertical().selected(on);
            b.tooltip(Component.translatable("lauramod.job." + job.key() + ".help"),
                    Component.translatable(on ? "lauramod.menu.job.stop" : "lauramod.menu.job.start").withStyle(ChatFormatting.GRAY));
        }
        widgets.add(new KSlider(x, y + 48, cw(), 3, 24, jobRadius, v -> Component.translatable("lauramod.menu.job.radius", v), v -> jobRadius = v));
        int half = (cw() - 4) / 2;
        button(x, y + 76, half, 20, "lauramod.menu.job.back_to_work", Icon.PLUS, KButton.Style.MINT, c -> act(LauraAction.WORK, ""));
        button(x + half + 4, y + 76, half, 20, "lauramod.menu.job.stop_all", Icon.STOP, KButton.Style.PEACH, c -> act(LauraAction.JOB, "ALL:off"));
    }

    private void buildErrands(int x, int y) {
        int bw = (cw() - 8) / 3;
        KButton chop = button(x, y, bw, 22, "lauramod.task.chop_tree", Icon.JOB_LUMBERJACK, KButton.Style.PINK,
                c -> act(LauraAction.TASK, "CHOP_TREE:" + (c.shift() ? "queue" : "now") + ":" + jobRadius));
        KButton harvest = button(x + bw + 4, y, bw, 22, "lauramod.task.harvest", Icon.JOB_FARMER, KButton.Style.PINK,
                c -> act(LauraAction.TASK, "HARVEST:" + (c.shift() ? "queue" : "now") + ":" + jobRadius));
        KButton cook = button(x + 2 * (bw + 4), y, bw, 22, "lauramod.task.cook", Icon.JOB_COOK, KButton.Style.PINK,
                c -> act(LauraAction.TASK, "COOK:" + (c.shift() ? "queue" : "now") + ":" + jobRadius));
        for (KButton b : List.of(chop, harvest, cook)) {
            b.tooltip(Component.translatable("lauramod.menu.shift_hint").withStyle(ChatFormatting.GRAY));
        }
        queueList = new KList<>(x, y + 28, cw() - 62, 94, 12,
                (d, row, rx, ry, rw, rh, hovered, index) -> d.textFit(row, rx, ry + 2, rw, Kawaii.PLUM),
                (row, index, lx, button) -> {
                    int queueIndex = status().has("taskCurrent") ? index : index + 1;
                    if (queueIndex > 0) {
                        act(LauraAction.QUEUE, "remove:" + queueIndex);
                        LauraClientNetwork.requestStatus(laura.getId());
                    }
                });
        queueList.tooltip(Component.translatable("lauramod.menu.queue.help"));
        queueList.setItems(queueRows());
        widgets.add(queueList);
        button(x + cw() - 58, y + 28, 58, 20, "lauramod.menu.queue.clear", Icon.TRASH, KButton.Style.PEACH, c -> act(LauraAction.QUEUE, "clear"));
        button(x + cw() - 58, y + 52, 58, 20, "lauramod.menu.stop", Icon.STOP, KButton.Style.PEACH, c -> act(LauraAction.STOP, ""));
    }

    private List<Component> queueRows() {
        List<Component> rows = new ArrayList<>();
        JsonObject s = status();
        if (s.has("taskCurrent")) {
            rows.add(Component.literal("> ").append(statusText(s.get("taskCurrent"))).withStyle(ChatFormatting.BOLD));
        }
        if (s.has("taskQueue")) {
            int n = 1;
            for (JsonElement e : s.getAsJsonArray("taskQueue")) {
                rows.add(Component.literal(n++ + ". ").append(statusText(e)));
            }
        }
        return rows;
    }

    private void buildChests(int x, int y) {
        int bw = (cw() - 12) / 4;
        ChestPurpose[] purposes = ChestPurpose.values();
        for (int i = 0; i < purposes.length; i++) {
            ChestPurpose p = purposes[i];
            KButton b = button(x + (i % 4) * (bw + 4), y + (i / 4) * 34, bw, 30, "lauramod.chest." + p.key(), Icon.chest(p), KButton.Style.PINK,
                    c -> act(LauraAction.CHEST, p.name())).vertical();
            b.active = lookedAtBlock != null;
            b.tooltip(Component.translatable("lauramod.chest." + p.key() + ".help"));
        }
        int half = (cw() - 4) / 2;
        KButton remove = button(x, y + 72, half, 20, "lauramod.menu.chest.remove", Icon.TRASH, KButton.Style.PEACH, c -> act(LauraAction.CHEST, "remove"));
        remove.active = lookedAtBlock != null;
        button(x + half + 4, y + 72, half, 20, "lauramod.menu.chest.list", Icon.QUEUE, KButton.Style.LAVENDER, c -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.connection.sendCommand("laura chest list");
            }
        });
    }

    // ------------------------------------------------------------------ tab: fetch

    private void buildFetch() {
        int x = cx0();
        int y = cy0();
        EditBox search = box(x, y, cw(), "lauramod.menu.fetch.search", 64);
        search.setValue(searchText);
        search.setResponder(text -> {
            searchText = text;
            filterItems();
        });
        int cols = (cw() - 8) / 18;
        itemGrid = new ItemGrid(x, y + 22, cw(), FETCH_GRID_H, cols);
        widgets.add(itemGrid);
        filterItems();
        // Row 1: how many, with the current choice written next to the chips (see drawFetch).
        int cy = y + 22 + FETCH_GRID_H + 4;
        for (int i = 0; i < FETCH_COUNTS.length; i++) {
            int count = FETCH_COUNTS[i];
            KButton b = add(new KButton(x + i * (FETCH_CHIP + 2), cy, FETCH_CHIP, 18, Component.literal(String.valueOf(count)), c -> {
                fetchCount = count;
                rebuild();
            }).style(KButton.Style.LAVENDER));
            b.selected = fetchCount == count;
        }
        // Row 2: the three actions share the whole width, so long translations still fit.
        int by = cy + 22;
        int bw = (cw() - 8) / 3;
        KButton go = button(x, by, bw, 18, "lauramod.menu.fetch.go", Icon.FOLLOW, KButton.Style.PINK, c -> fetch(c.shift()));
        go.active = fetchItem != null && ClientState.fetchEnabled;
        KButton later = button(x + bw + 4, by, bw, 18, "lauramod.menu.fetch.queue", Icon.QUEUE, KButton.Style.LAVENDER, c -> fetch(true));
        later.active = go.active;
        KButton held = button(x + 2 * (bw + 4), by, cw() - 2 * (bw + 4), 18, "lauramod.menu.fetch.held", Icon.INVENTORY, KButton.Style.MINT, c -> {
            act(LauraAction.FETCH, "held|" + fetchCount);
            onClose();
        });
        held.active = ClientState.fetchEnabled;
        held.tooltip(Component.translatable("lauramod.menu.fetch.held.help"));
    }

    private void fetch(boolean queue) {
        if (fetchItem == null) {
            return;
        }
        act(LauraAction.FETCH, BuiltInRegistries.ITEM.getKey(fetchItem) + "|" + fetchCount + (queue ? "|queue" : ""));
        onClose();
    }

    private void filterItems() {
        if (itemGrid == null) {
            return;
        }
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            if (q.isEmpty() || BuiltInRegistries.ITEM.getKey(item).toString().contains(q)
                    || Component.translatable(item.getDescriptionId()).getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(item);
            }
        }
        itemGrid.setItemList(out);
    }

    /** A scrolling grid of items to pick what she must bring. */
    private final class ItemGrid extends KWidget {
        private final int cols;
        private List<Item> all = new ArrayList<>();
        private int scrollRows;

        ItemGrid(int x, int y, int w, int h, int cols) {
            super(x, y, w, h);
            this.cols = cols;
        }

        void setItemList(List<Item> items) {
            this.all = items;
            this.scrollRows = 0;
        }

        private int rows() {
            return (all.size() + cols - 1) / cols;
        }

        private int visibleRows() {
            return (h - 4) / 18;
        }

        @Override
        protected void render(KDraw d, int mx, int my, boolean hovered) {
            Kawaii.rounded(d, x, y, w, h, 4, 0xFFFFF0F6);
            Item hoveredItem = null;
            int ox = x + (w - cols * 18) / 2;
            for (int r = 0; r < visibleRows(); r++) {
                for (int c = 0; c < cols; c++) {
                    int i = (r + scrollRows) * cols + c;
                    if (i >= all.size()) {
                        break;
                    }
                    Item item = all.get(i);
                    int ix = ox + c * 18;
                    int iy = y + 2 + r * 18;
                    boolean over = mx >= ix && mx < ix + 18 && my >= iy && my < iy + 18;
                    if (item == fetchItem) {
                        Kawaii.rounded(d, ix, iy, 18, 18, 3, Kawaii.PINK);
                    } else if (over) {
                        Kawaii.rounded(d, ix, iy, 18, 18, 3, Kawaii.BLUSH);
                    }
                    if (over) {
                        hoveredItem = item;
                    }
                    d.item(new ItemStack(item), ix + 1, iy + 1);
                }
            }
            int max = Math.max(0, rows() - visibleRows());
            if (max > 0) {
                int track = h - 4;
                int thumb = Math.max(10, track * visibleRows() / Math.max(1, rows()));
                int ty = y + 2 + (track - thumb) * scrollRows / max;
                Kawaii.rounded(d, x + w - 4, y + 2, 3, track, 1, 0x30A0406E);
                Kawaii.rounded(d, x + w - 4, ty, 3, thumb, 1, Kawaii.PINK_DEEP);
            }
            if (all.isEmpty()) {
                d.centered(Component.translatable("lauramod.menu.fetch.nothing"), x + w / 2, y + h / 2 - 4, Kawaii.PLUM_SOFT);
            }
            tooltip = hoveredItem != null ? List.of(Component.translatable(hoveredItem.getDescriptionId())) : null;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double amount) {
            if (!isOver(mx, my)) {
                return false;
            }
            int max = Math.max(0, rows() - visibleRows());
            scrollRows = Math.max(0, Math.min(max, scrollRows - (int) Math.signum(amount)));
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isOver(mx, my)) {
                return false;
            }
            int ox = x + (w - cols * 18) / 2;
            int c = (int) Math.floor((mx - ox) / 18);
            int r = (int) Math.floor((my - y - 2) / 18);
            int i = (r + scrollRows) * cols + c;
            if (c >= 0 && c < cols && r >= 0 && r < visibleRows() && i < all.size()) {
                fetchItem = all.get(i);
                click();
                int keepScroll = scrollRows;
                rebuild();
                if (itemGrid != null) {
                    itemGrid.scrollRows = keepScroll;
                }
                return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ tab: style

    private void buildStyle() {
        String[] pages = {"skins", "models"};
        int pw = (cw() - 4) / 2;
        for (int i = 0; i < pages.length; i++) {
            int page = i;
            button(cx0() + i * (pw + 4), cy0(), pw, 16, "lauramod.menu.style." + pages[i], i == 0 ? Icon.SKIN : Icon.MODEL, KButton.Style.LAVENDER, c -> {
                stylePage = page;
                rebuild();
            }).selected(stylePage == i);
        }
        if (stylePage == 0) {
            buildSkins(cx0(), cy0() + 20);
        } else {
            buildModels(cx0(), cy0() + 20);
        }
    }

    private void buildSkins(int x, int y) {
        List<String> builtin = SkinRef.BUILTIN;
        int tw = (cw() - (builtin.size() - 1) * 3) / builtin.size();
        for (int i = 0; i < builtin.size(); i++) {
            String name = builtin.get(i);
            SkinButton b = new SkinButton(x + i * (tw + 3), y, tw, 30, name);
            b.style(KButton.Style.PINK);
            b.tooltip(Component.translatable("lauramod.skin.builtin." + name));
            b.selected = laura.getSkinRaw().equals("builtin:" + name);
            widgets.add(b);
        }
        int half = (cw() - 4) / 2;
        KList<String[]> files = new KList<>(x, y + 42, half, 44, 11,
                (d, item, rx, ry, rw, rh, hovered, index) -> d.textFit(item[0], rx, ry + 2, rw, Kawaii.PLUM),
                (item, index, lx, button) -> LauraClientNetwork.setSkin(laura.getId(), "server:" + item[0], laura.isSlim()));
        files.setItems(ClientState.assetList(AssetKind.SKIN));
        files.tooltip(Component.translatable("lauramod.menu.skin.server_files.help"));
        widgets.add(files);
        KList<Path> local = new KList<>(x + half + 4, y + 42, half, 44, 11,
                (d, item, rx, ry, rw, rh, hovered, index) -> d.textFit(item.getFileName().toString(), rx, ry + 2, rw, Kawaii.PLUM),
                (item, index, lx, button) -> useLocalSkin(item));
        local.setItems(localFiles("skins", ".png"));
        local.tooltip(Component.translatable("lauramod.menu.skin.local_files.help"));
        widgets.add(local);
        widgets.add(new KToggle(x, y + 90, half, Component.translatable("lauramod.menu.skin.slim"), laura.isSlim(),
                v -> LauraClientNetwork.setSkin(laura.getId(), laura.getSkinRaw(), v)));
        button(x + half + 4, y + 89, half, 16, "lauramod.menu.skin.reset", Icon.REFRESH, KButton.Style.PEACH,
                c -> LauraClientNetwork.setSkin(laura.getId(), "reset", true));
        urlBox = box(x, y + 108, cw() - 60, "lauramod.menu.skin.url", 512);
        urlBox.setValue(urlText);
        urlBox.setResponder(text -> urlText = text);
        KButton applyUrl = button(x + cw() - 56, y + 108, 56, 18, "lauramod.menu.apply", Icon.CHECK, KButton.Style.MINT, c -> applyUrl());
        applyUrl.active = ClientState.allowUrlSkins;
        urlBox.setEditable(ClientState.allowUrlSkins);
        playerBox = box(x, y + 128, cw() - 60, "lauramod.menu.skin.player", 16);
        playerBox.setValue(playerText);
        playerBox.setResponder(text -> playerText = text);
        KButton applyPlayer = button(x + cw() - 56, y + 128, 56, 18, "lauramod.menu.apply", Icon.CHECK, KButton.Style.MINT, c -> applyPlayer());
        applyPlayer.active = ClientState.allowPlayerNameSkins;
        playerBox.setEditable(ClientState.allowPlayerNameSkins);
    }

    private void applyUrl() {
        if (urlBox != null && !urlBox.getValue().isBlank()) {
            LauraClientNetwork.setSkin(laura.getId(), "url:" + urlBox.getValue().trim(), laura.isSlim());
        }
    }

    private void applyPlayer() {
        if (playerBox != null && !playerBox.getValue().isBlank()) {
            LauraClientNetwork.setSkin(laura.getId(), "player:" + playerBox.getValue().trim(), true);
        }
    }

    private void buildModels(int x, int y) {
        List<String> models = new ArrayList<>();
        models.add("");
        for (String[] e : ClientState.assetList(AssetKind.MODEL)) {
            models.add("server:" + e[0]);
        }
        KList<String> list = new KList<>(x, y, cw(), 104, 12,
                (d, item, rx, ry, rw, rh, hovered, index) -> d.textFit(item.isEmpty() ? Component.translatable("lauramod.menu.model.default")
                        : Component.literal(item.substring(item.indexOf(':') + 1)), rx, ry + 2, rw, Kawaii.PLUM),
                (item, index, lx, button) -> LauraClientNetwork.setModel(laura.getId(), item.isEmpty() ? "reset" : item));
        list.setItems(models);
        list.active = ClientState.allowCustomModels;
        widgets.add(list);
        if (minecraft != null && !minecraft.hasSingleplayerServer() && ClientState.allowModelUploads) {
            List<Path> files = localFiles("models", ".bbmodel");
            if (!files.isEmpty()) {
                list.h = 60;
                KList<Path> local = new KList<>(x, y + 64, cw(), 40, 11,
                        (d, item, rx, ry, rw, rh, hovered, index) -> d.textFit(Component.translatable("lauramod.menu.model.upload", item.getFileName().toString()), rx, ry + 2, rw, Kawaii.PLUM),
                        (item, index, lx, button) -> upload(AssetKind.MODEL, item));
                local.setItems(files);
                widgets.add(local);
            }
        }
        int half = (cw() - 4) / 2;
        button(x, y + 110, half, 20, "lauramod.menu.model.folder", Icon.MODEL, KButton.Style.LAVENDER,
                c -> Util.getPlatform().openFile(LauraMod.configDir().resolve("models").toFile()));
        button(x + half + 4, y + 110, half, 20, "lauramod.menu.skin.folder", Icon.SKIN, KButton.Style.LAVENDER,
                c -> Util.getPlatform().openFile(LauraMod.configDir().resolve("skins").toFile()));
    }

    /** A button that shows the face of a built-in skin. */
    private final class SkinButton extends KButton {
        private final String name;

        SkinButton(int x, int y, int w, int h, String name) {
            super(x, y, w, h, null, c -> LauraClientNetwork.setSkin(laura.getId(), "builtin:" + name, true));
            this.name = name;
        }

        @Override
        protected void render(KDraw d, int mx, int my, boolean hovered) {
            super.render(d, mx, my, hovered);
            int size = Math.min(20, h - 8);
            d.face(SkinTextures.builtin(name), x + (w - size) / 2, y + (h - size) / 2 - Math.round(hoverAnim * 1.5F), size);
        }
    }

    private List<Path> localFiles(String folder, String extension) {
        List<Path> out = new ArrayList<>();
        Path dir = LauraMod.configDir().resolve(folder);
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)).sorted().forEach(out::add);
            } catch (Exception e) {
                LauraMod.LOGGER.debug("Could not list {}", dir, e);
            }
        }
        return out;
    }

    private void useLocalSkin(Path file) {
        String name = file.getFileName().toString();
        if (minecraft != null && minecraft.hasSingleplayerServer()) {
            // Same folder as the integrated server: no upload needed.
            LauraClientNetwork.setSkin(laura.getId(), "server:" + name.substring(0, name.length() - 4), laura.isSlim());
            return;
        }
        upload(AssetKind.SKIN, file);
    }

    private void upload(AssetKind kind, Path file) {
        boolean allowed = kind == AssetKind.SKIN ? ClientState.allowSkinUploads : ClientState.allowModelUploads;
        if (!allowed) {
            ClientState.lastUploadMessage = Component.translatable("lauramod.upload.disabled");
            ClientState.lastUploadSuccess = false;
            return;
        }
        try {
            LauraClientNetwork.upload(kind, file.getFileName().toString(), Files.readAllBytes(file), laura.getId(), laura.isSlim());
        } catch (Exception e) {
            LauraMod.LOGGER.warn("Could not read {}", file);
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        for (Path file : files) {
            String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".png")) {
                upload(AssetKind.SKIN, file);
            } else if (n.endsWith(".bbmodel")) {
                upload(AssetKind.MODEL, file);
            } else if (n.endsWith(".geo.json")) {
                // A Bedrock model needs its texture file next to it: it cannot travel as one file.
                ClientState.lastUploadMessage = Component.translatable("lauramod.upload.bbmodel_only");
                ClientState.lastUploadSuccess = false;
            }
        }
    }

    // ------------------------------------------------------------------ tab: settings

    private void buildSettings() {
        int x = cx0();
        int y = cy0();
        renameBox = box(x, y, cw() - 74, "lauramod.menu.rename.hint", 32);
        renameBox.setValue(laura.getLauraName());
        button(x + cw() - 70, y, 70, 18, "lauramod.menu.rename", Icon.PENCIL, KButton.Style.PINK, c -> rename());
        if (focusRename) {
            focusRename = false;
            setFocused(renameBox);
            renameBox.moveCursorToEnd(false);
            renameBox.setHighlightPos(0);
        }
        int ty = y + 26;
        int half = (cw() - 8) / 2;
        List<ConfigFile.BoolValue> toggles = List.of(LauraClientConfig.speechBubbles, LauraClientConfig.thoughtBubbles, LauraClientConfig.animations,
                LauraClientConfig.particles, LauraClientConfig.customModels, LauraClientConfig.remoteSkins, LauraClientConfig.menuOnRightClick,
                LauraClientConfig.showNeedsHud);
        for (int i = 0; i < toggles.size(); i++) {
            ConfigFile.BoolValue value = toggles.get(i);
            KToggle toggle = new KToggle(x + (i % 2) * (half + 8), ty + (i / 2) * 17, half, Component.translatable("lauramod.client." + value.key()), value.get(), v -> {
                value.set(v);
                LauraClientConfig.file().save();
            });
            toggle.tooltip(Component.translatable("lauramod.client." + value.key() + ".help"));
            widgets.add(toggle);
        }
        int by = ty + 4 * 17 + 6;
        int third = (cw() - 8) / 3;
        button(x, by, third, 20, "lauramod.menu.config_folder", Icon.TAB_SETTINGS, KButton.Style.LAVENDER,
                c -> Util.getPlatform().openFile(LauraMod.configDir().toFile()));
        button(x + third + 4, by, third, 20, "lauramod.menu.call", Icon.CALL, KButton.Style.MINT, c -> act(LauraAction.COME, ""));
        KButton dismiss = button(x + 2 * (third + 4), by, third, 20, dismissArmed ? "lauramod.menu.dismiss_confirm" : "lauramod.menu.dismiss",
                Icon.TRASH, KButton.Style.PEACH, c -> {
                    if (!dismissArmed) {
                        dismissArmed = true;
                        rebuild();
                    } else if (minecraft != null && minecraft.player != null) {
                        minecraft.player.connection.sendCommand("laura dismiss");
                        onClose();
                    }
                });
        dismiss.tooltip(Component.translatable("lauramod.menu.dismiss.help"));
    }

    private void rename() {
        if (renameBox == null) {
            return;
        }
        String name = renameBox.getValue().trim();
        if (!name.isEmpty() && !name.equals(laura.getLauraName())) {
            act(LauraAction.RENAME, name);
            KWidget.pop();
        }
    }

    // ------------------------------------------------------------------ rendering

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
            rebuild();
        }
        if (++ticks % 40 == 0) {
            LauraClientNetwork.requestStatus(laura.getId());
        }
        if (ticks % 5 == 0) {
            refreshIfChanged();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        float delta = Math.min(5F, (now - lastFrame) / 50F);
        lastFrame = now;
        KDraw d = new KDraw(graphics);
        hearts.render(d, left - 30, top - 30, left + W + 30, top + H + 30, delta);
        Kawaii.panel(d, left, top, W, H, Kawaii.PINK, Kawaii.CREAM);
        Kawaii.rounded(d, left + 4, top + 4, W - 8, 27, 5, Kawaii.BLUSH);
        Kawaii.rounded(d, left + 4, top + 33, 30, 7 * 23 + 6, 5, 0xFFF3ECFF);
        drawHeader(d);
        switch (tab) {
            case HOME -> drawHome(d, mouseX, mouseY);
            case ORDERS -> drawOrders(d);
            case WORK -> drawWork(d);
            case FETCH -> drawFetch(d);
            case STYLE -> drawStyle(d);
            case SETTINGS -> drawSettings(d);
            default -> {
            }
        }
        for (KWidget w : widgets) {
            w.draw(d, mouseX, mouseY, delta);
        }
        drawFooter(d);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        KDraw d = new KDraw(graphics);
        for (int i = widgets.size() - 1; i >= 0; i--) {
            KWidget w = widgets.get(i);
            if (w.visible && w.tooltip != null && w.isOver(mouseX, mouseY)) {
                d.tooltip(w.tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    private void drawHeader(KDraw d) {
        d.icon(Icon.mood(laura.getMood()), left + 11, top + 10);
        d.push(left + 34, top + 8, 1.25F);
        d.text(d.ellipsize(Component.literal(laura.getLauraName()), (int) ((W - 34 - 64) / 1.25F)), 0, 0, Kawaii.PLUM);
        d.pop();
        int affection = laura.getAffection();
        int hx = left + 34;
        for (int i = 0; i < 10; i++) {
            int threshold = (i + 1) * 100;
            int color = affection >= threshold ? Kawaii.PINK_DEEP : affection >= threshold - 50 ? 0xFFFFB6D5 : 0x40A0406E;
            Kawaii.heart(d, hx + i * 9, top + 21, 1, color);
        }
        List<LauraEntity> mine = companions();
        if (mine.size() > 1) {
            String counter = (mine.indexOf(laura) + 1) + "/" + mine.size();
            d.centered(Component.literal(counter), left + W - 51, top + 13, Kawaii.PLUM_SOFT);
        }
        // Tab name on the right, mood in what is left between the hearts and it.
        Component tabName = Component.translatable("lauramod.menu.tab." + tab.key());
        int tabRight = left + W - 26 - (mine.size() > 1 ? 56 : 0);
        int tabWidth = Math.min(Math.round(d.width(tabName) * 0.75F), 70);
        small(d, tabName, tabRight - tabWidth, top + 21, Kawaii.ROSE, tabWidth);
        small(d, Component.translatable("lauramod.mood." + laura.getMood().key()).withStyle(ChatFormatting.ITALIC),
                hx + 94, top + 21, Kawaii.PLUM_SOFT, tabRight - tabWidth - 4 - (hx + 94));
    }

    private void drawFooter(KDraw d) {
        boolean upload = tab == Tab.STYLE && ClientState.lastUploadMessage != null;
        Component line = upload ? ClientState.lastUploadMessage
                // The first day counts as day 1, not "together for 0 days".
                : Component.translatable("lauramod.menu.footer", laura.daysTogether() + 1, Component.translatable("lauramod.mode." + laura.getMode().key()),
                Math.round(laura.getHealth()), Math.round(laura.getMaxHealth()));
        small(d, line, left + 40, top + H - 13, upload && !ClientState.lastUploadSuccess ? Kawaii.ROSE : Kawaii.PLUM_SOFT);
    }

    /** Small text that never runs past the right edge of the panel. */
    private void small(KDraw d, Component text, int x, int y, int color) {
        small(d, text, x, y, color, left + W - 8 - x);
    }

    /**
     * Small text kept inside {@code maxWidth} pixels: it shrinks a little for long translations,
     * then gets cut with "...".
     */
    private void small(KDraw d, Component text, int x, int y, int color, int maxWidth) {
        float scale = 0.75F;
        int width = d.width(text);
        Component shown = text;
        if (width * scale > maxWidth) {
            scale = Math.max(0.6F, maxWidth / (float) Math.max(1, width));
            if (width * scale > maxWidth) {
                shown = d.ellipsize(text, (int) (maxWidth / scale));
            }
        }
        d.push(x, y + (0.75F - scale) * 4, scale);
        d.text(shown, 0, 0, color);
        d.pop();
    }

    private void drawHome(KDraw d, int mx, int my) {
        int x = cx0();
        int y = cy0();
        Kawaii.card(d, x, y, 90, CONTENT_H - 2, 0xFFF3ECFF);
        for (int i = 0; i < 7; i++) {
            int sx = x + 8 + (i * 37) % 76;
            int sy = y + 8 + (i * 53) % 118;
            d.fill(sx, sy - 1, sx + 1, sy + 2, 0x90FFFFFF);
            d.fill(sx - 1, sy, sx + 2, sy + 1, 0x90FFFFFF);
        }
        d.entity(laura, x + 4, y + 4, x + 86, y + 136, 44, mx, my);
        d.icon(Icon.HEALTH, x + 5, y + 143, 12);
        Kawaii.bar(d, x + 19, y + 145, 64, 8, laura.getHealth() / Math.max(1, laura.getMaxHealth()), 0xFFFF6B8A, 0x30A0406E);

        int rx = x + 96;
        int rw = cw() - 96;
        Kawaii.card(d, rx, y, rw, 84, Kawaii.WHITE);
        d.text(Component.translatable("lauramod.menu.needs"), rx + 6, y + 5, Kawaii.ROSE);
        long packed = laura.getPackedNeeds();
        int[] colors = {Kawaii.PEACH_DEEP, Kawaii.LAVENDER_DEEP, Kawaii.MINT_DEEP, Kawaii.PINK_DEEP, Kawaii.SKY_DEEP};
        for (Needs.Need need : Needs.Need.values()) {
            int v = Needs.unpack(packed, need);
            int ny = y + 18 + need.ordinal() * 13;
            d.icon(Icon.need(need), rx + 5, ny - 3, 12);
            small(d, Component.translatable("lauramod.need." + need.key()), rx + 20, ny, Kawaii.PLUM, 56);
            Kawaii.bar(d, rx + 78, ny - 1, rw - 108, 8, v / 100F, Kawaii.needColor(v, colors[need.ordinal()]), 0x30A0406E);
            small(d, Component.literal(v + "%"), rx + rw - 25, ny, Kawaii.PLUM_SOFT);
        }
        if (!ClientState.needsEnabled) {
            Kawaii.rounded(d, rx + 2, y + 14, rw - 4, 68, 3, 0xC0FFFFFF);
            d.centered(d.ellipsize(Component.translatable("lauramod.menu.needs_off"), rw - 10), rx + rw / 2, y + 44, Kawaii.PLUM_SOFT);
        }

        Kawaii.card(d, rx, y + 90, rw, 42, 0xFFFFF4F9);
        d.icon(Icon.DESIRE, rx + 5, y + 96);
        JsonObject s = status();
        if (s.has("desire")) {
            ItemStack icon = LauraRenderer.thoughtIcon(s.get("desire").getAsString());
            if (!icon.isEmpty()) {
                d.item(icon, rx + rw - 22, y + 103);
            }
            Component text = s.has("desireText") ? statusText(s.get("desireText")) : Component.literal("?");
            int room = rw - 24 - (icon.isEmpty() ? 6 : 26);
            small(d, Component.translatable("lauramod.menu.desire"), rx + 24, y + 96, Kawaii.ROSE, room);
            if (d.width(text) <= room) {
                d.text(text, rx + 24, y + 106, Kawaii.PLUM);
            } else {
                // Long wishes (and long languages) wrap on two smaller lines instead of leaving the card.
                List<FormattedCharSequence> lines = d.font().split(text, (int) (room / 0.75F));
                for (int i = 0; i < Math.min(2, lines.size()); i++) {
                    d.push(rx + 24, y + 104 + i * 7, 0.75F);
                    d.text(lines.get(i), 0, 0, Kawaii.PLUM);
                    d.pop();
                }
            }
            long seconds = s.has("desireSeconds") ? s.get("desireSeconds").getAsLong() : 0;
            small(d, Component.translatable("lauramod.menu.desire_time", seconds / 60, String.format(Locale.ROOT, "%02d", seconds % 60)), rx + 24, y + 119, Kawaii.PLUM_SOFT, room);
        } else {
            small(d, Component.translatable("lauramod.menu.no_desire"), rx + 24, y + 106, Kawaii.PLUM_SOFT, rw - 30);
        }
    }

    private void drawOrders(KDraw d) {
        small(d, Component.translatable("lauramod.menu.combat"), cx0(), cy0() + 4 * 25 + 3, Kawaii.ROSE);
    }

    private void drawWork(KDraw d) {
        int x = cx0();
        int y = cy0() + 22;
        switch (workPage) {
            case 0 -> small(d, Component.translatable("lauramod.menu.job.help"), x, y + 102, Kawaii.PLUM_SOFT);
            case 1 -> small(d, Component.translatable("lauramod.menu.shift_hint"), x, y + 128, Kawaii.PLUM_SOFT);
            default -> {
                Component hint = lookedAtBlock != null
                        ? Component.translatable("lauramod.menu.chest.looking", lookedAtBlock.getX(), lookedAtBlock.getY(), lookedAtBlock.getZ())
                        : Component.translatable("lauramod.menu.chest.look_first");
                small(d, hint, x, y + 98, lookedAtBlock != null ? Kawaii.PLUM : Kawaii.ROSE);
                small(d, Component.translatable("lauramod.menu.chest.help"), x, y + 110, Kawaii.PLUM_SOFT);
                JsonObject s = status();
                if (s.has("chests")) {
                    small(d, Component.translatable("lauramod.menu.chest.count", s.get("chests").getAsInt()), x, y + 122, Kawaii.PLUM_SOFT);
                }
            }
        }
    }

    private void drawFetch(KDraw d) {
        Kawaii.rounded(d, cx0(), cy0(), cw(), 20, 4, Kawaii.WHITE);
        Component line = fetchItem != null
                ? Component.translatable("lauramod.menu.fetch.selected", Component.translatable(fetchItem.getDescriptionId()), fetchCount)
                : Component.translatable("lauramod.menu.fetch.pick");
        int chipsEnd = FETCH_COUNTS.length * (FETCH_CHIP + 2) + 6;
        int lineY = cy0() + 22 + FETCH_GRID_H + 4;
        int room = (int) ((cw() - chipsEnd) / 0.75F);
        List<FormattedCharSequence> lines = d.font().split(line, room);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            d.push(cx0() + chipsEnd, lineY + (lines.size() > 1 ? 2 : 6) + i * 8, 0.75F);
            d.text(lines.get(i), 0, 0, fetchItem != null ? Kawaii.PLUM : Kawaii.PLUM_SOFT);
            d.pop();
        }
        if (!ClientState.fetchEnabled) {
            small(d, Component.translatable("lauramod.menu.fetch.disabled"), cx0(), lineY + 46, Kawaii.ROSE);
        }
    }

    private void drawStyle(KDraw d) {
        int x = cx0();
        int y = cy0() + 20;
        if (stylePage == 0) {
            int half = (cw() - 4) / 2;
            small(d, Component.translatable("lauramod.menu.skin.server_files"), x, y + 34, Kawaii.ROSE, half);
            small(d, Component.translatable("lauramod.menu.skin.local_files"), x + half + 4, y + 34, Kawaii.ROSE, half);
            Kawaii.rounded(d, x, y + 108, cw() - 60, 18, 4, ClientState.allowUrlSkins ? Kawaii.WHITE : Kawaii.DISABLED);
            Kawaii.rounded(d, x, y + 128, cw() - 60, 18, 4, ClientState.allowPlayerNameSkins ? Kawaii.WHITE : Kawaii.DISABLED);
        } else {
            small(d, Component.translatable("lauramod.menu.model.hint"), x, y + 134, Kawaii.PLUM_SOFT);
        }
    }

    private void drawSettings(KDraw d) {
        Kawaii.rounded(d, cx0(), cy0(), cw() - 74, 18, 4, Kawaii.WHITE);
        JsonObject s = status();
        int y = cy0() + 26 + 4 * 17 + 32;
        List<Component> lines = new ArrayList<>();
        if (s.has("owner")) {
            lines.add(Component.translatable("lauramod.menu.owner", s.get("owner").getAsString()));
        }
        lines.add(s.has("home") ? Component.translatable("lauramod.menu.home_at", s.get("home").getAsString())
                : Component.translatable("lauramod.menu.no_home"));
        if (s.has("annoyance")) {
            lines.add(Component.translatable("lauramod.menu.annoyance",
                    Component.translatable("lauramod.annoyance." + s.get("annoyance").getAsString().toLowerCase(Locale.ROOT))));
        }
        for (int i = 0; i < lines.size(); i++) {
            small(d, lines.get(i), cx0(), y + i * 9, Kawaii.PLUM_SOFT);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        setFocused(null);
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (KWidget w : widgets) {
            w.mouseReleased(event.x(), event.y(), event.button());
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        for (KWidget w : widgets) {
            if (w.mouseDragged(event.x(), event.y(), event.button(), dx, dy)) {
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        for (KWidget w : widgets) {
            if (w.mouseScrolled(mx, my, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean typing = getFocused() instanceof EditBox;
        if (typing && (key == 257 || key == 335)) {
            // Enter validates the focused field.
            if (getFocused() == renameBox) {
                rename();
            } else if (getFocused() == urlBox) {
                applyUrl();
            } else if (getFocused() == playerBox) {
                applyPlayer();
            }
            return true;
        }
        if (!typing && LauraClient.MENU_KEY.matches(event)) {
            onClose();
            return true;
        }
        if (!typing && key >= 49 && key <= 55) {
            // Keys 1 to 7 switch tabs.
            tab = Tab.values()[key - 49];
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
