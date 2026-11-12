package com.vyrriox.lauramod.client.ui;

import com.vyrriox.lauramod.config.ConfigFile;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.work.ChestPurpose;
import com.vyrriox.lauramod.network.LauraAction;
import com.vyrriox.lauramod.skin.AssetKind;
import com.vyrriox.lauramod.skin.SkinRef;
import dev.dryaides.minuit.BaseComponent;
import dev.dryaides.minuit.Box;
import dev.dryaides.minuit.ButtonComponent;
import dev.dryaides.minuit.Draw;
import dev.dryaides.minuit.EntityPreviewComponent;
import dev.dryaides.minuit.FillColor;
import dev.dryaides.minuit.FloatingParticlesComponent;
import dev.dryaides.minuit.Icon;
import dev.dryaides.minuit.IconArt;
import dev.dryaides.minuit.ItemPickerComponent;
import dev.dryaides.minuit.ListComponent;
import dev.dryaides.minuit.Palette;
import dev.dryaides.minuit.PanelComponent;
import dev.dryaides.minuit.ProgressComponent;
import dev.dryaides.minuit.SideRailComponent;
import dev.dryaides.minuit.SliderComponent;
import dev.dryaides.minuit.TextFieldComponent;
import dev.dryaides.minuit.Theme;
import dev.dryaides.minuit.ToggleComponent;
import dev.dryaides.minuit.ToggleGroupComponent;
import dev.dryaides.minuit.Words;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Laura's menu on minuit: a header with her name, mood and affection, one page per tab on a rail
 * down the left edge, and a footer line. The screen builds it again whenever a tab, a page or her
 * state changes; what must survive that lives in {@link MenuState}.
 *
 * @author vyrriox
 */
public class LauraPanel extends PanelComponent {
    public static final int W = 304;
    public static final int H = 216;
    private static final int CX = 8;
    private static final int CY = 34;
    private static final int CW = W - 16;
    private static final int CONTENT_H = 164;
    private static final int FETCH_PICKER_H = 112;
    /** Least space between two controls, in GUI px; every row and column below is laid out with it. */
    public static final int GAP = 2;
    /** Orders tab: four rows of order buttons, the combat label, the chips and the pickup toggle fill CONTENT_H. */
    private static final int ORDER_H = 26;
    private static final int LABEL_H = 6;
    private static final int ORDERS_LABEL_Y = CY + 4 * (ORDER_H + GAP) - GAP + 2 * GAP;
    private static final int ORDERS_CHIPS_Y = ORDERS_LABEL_Y + LABEL_H + 2 * GAP;
    private static final int[] FETCH_COUNTS = {1, 8, 16, 32, 64};
    /** Base colour of each need bar, in the order of Needs.Need: hunger, energy, fun, attention, hygiene. */
    private static final int[] NEED_COLORS = {0xFFFFB38A, 0xFFB39DFF, 0xFF7EE0C3, 0xFFF77FB2, 0xFF8FD3FF};
    private static final String ATLAS = "lauramod:textures/gui/kawaii_icons.png";

    private final LauraView laura;
    private final MenuState state;
    private final Runnable rebuild;
    private final FloatingParticlesComponent hearts;
    private final List<ProgressComponent> needBars = new ArrayList<>();
    private ListComponent<String> queueList;
    private ItemPickerComponent picker;
    private TextFieldComponent renameField;
    private int ticks;

    public LauraPanel(Theme theme, Words words, int left, int top, LauraView laura, MenuState state, Runnable rebuild) {
        super(theme, words, left, top);
        this.laura = laura;
        this.state = state;
        this.rebuild = rebuild;
        MenuState.lastTab = state.tab;
        this.hearts = new FloatingParticlesComponent(at(new Box("hearts", -30, -30, W + 60, H + 60)), laura.icon("HEART_FULL"))
                .count(1).lifeMs(3000).rise(H + 40);
        buildFrame();
        switch (state.tab) {
            case HOME -> buildHome();
            case ORDERS -> buildOrders();
            case EMOTES -> buildEmotes();
            case WORK -> buildWork();
            case FETCH -> buildFetch();
            case STYLE -> buildStyle();
            case SETTINGS -> buildSettings();
        }
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public int height() {
        return H;
    }

    // ------------------------------------------------------------------ state the screen reads

    /** What the buttons of the current tab show; the screen rebuilds the panel when it changes. */
    public String signature() {
        return laura.mode() + "|" + laura.combat() + "|" + laura.pickingUp() + "|" + laura.gagged() + "|"
                + laura.jobs() + "|" + laura.skinRaw() + "|" + laura.slim();
    }

    /** True while a text field or the item search has the keyboard: the screen then leaves the panel as it is. */
    public boolean typing() {
        return focus() instanceof TextFieldComponent || focus() instanceof ItemPickerComponent;
    }

    /** A fresh status came in: the task queue follows it without a rebuild. */
    public void refreshQueue() {
        if (queueList != null) {
            queueList.setItems(queueRows());
        }
    }

    /** Keeps what the player typed before the screen builds the panel again. */
    public void remember() {
        if (picker != null) {
            state.search = picker.query();
        }
        if (renameField != null) {
            state.rename = renameField.text();
        }
    }

    @Override
    public void tick() {
        // Hearts keep rising around the menu, a few at a time.
        if (++ticks % 6 == 0) {
            hearts.burst(1);
        }
    }

    // ------------------------------------------------------------------ helpers

    private Box box(String name, int x, int y, int w, int h) {
        return at(new Box(name, x, y, w, h));
    }

    private void rebuild() {
        remember();
        rebuild.run();
    }

    private void act(LauraAction action, String arg) {
        laura.act(action, arg);
    }

    /** Buttons and cards take their visual height (lip included), so they line up with fields of the same h. */
    private ButtonComponent button(int x, int y, int w, int h, String key, String icon, ButtonComponent.Role role, Runnable action) {
        ButtonComponent b = new ButtonComponent(box(key == null ? "button" : key, x, y, w, h), key == null ? "" : words.get(key), action);
        if (icon != null) {
            b.icon(laura.icon(icon));
        }
        if (role != null) {
            b.role(role);
        }
        return add(b);
    }

    private ButtonComponent iconButton(int x, int y, int w, int h, String tooltipKey, String icon, ButtonComponent.Role role, Runnable action) {
        ButtonComponent b = button(x, y, w, h, null, icon, role, action);
        b.tooltip(words.get(tooltipKey));
        return b;
    }

    private CardButton card(int x, int y, int w, int h, IconArt art, String text, Runnable action) {
        return add(new CardButton(box("card", x, y, w, h), art, text, action));
    }

    private void small(Draw d, String text, int x, int y, int w, int color) {
        d.small(text, new Box("text", left + x, top + y, Math.max(0, w), 6), color, 0.75F, d.guiScale());
    }

    private void small(Draw d, String text, int x, int y, int color) {
        small(d, text, x, y, W - 8 - x, color);
    }

    private static boolean shift() {
        return BaseComponent.keys.shift();
    }

    // ------------------------------------------------------------------ frame: rail, header, footer

    private void buildFrame() {
        add(new ButtonComponent(box("close", W - 22, 9, 16, 16), "", laura::close).icon(Icon.CLOSE).style(ButtonComponent.Style.GHOST))
                .tooltip(words.get("lauramod.menu.close"));
        if (laura.companionCount() > 1) {
            add(new ButtonComponent(box("previous", W - 78, 9, 16, 16), "", () -> laura.switchLaura(-1)).icon(Icon.PREV)
                    .style(ButtonComponent.Style.GHOST)).tooltip(words.get("lauramod.menu.previous"));
            add(new ButtonComponent(box("next", W - 40, 9, 16, 16), "", () -> laura.switchLaura(1)).icon(Icon.NEXT)
                    .style(ButtonComponent.Style.GHOST)).tooltip(words.get("lauramod.menu.next"));
        }
        add(new ButtonComponent(box("rename", 28 + nameWidth() + 4, 6, 14, 14), "", () -> {
            state.tab = MenuTab.SETTINGS;
            state.focusRename = true;
            rebuild();
        }).icon(laura.icon("PENCIL")).style(ButtonComponent.Style.GHOST)).tooltip(words.get("lauramod.menu.rename"));

        SideRailComponent rail = new SideRailComponent(bounds(), SideRailComponent.Edge.LEFT)
                .style(SideRailComponent.Style.OVERLAP);
        for (MenuTab t : MenuTab.values()) {
            rail.action(laura.icon(t.icon), words.get("lauramod.menu.tab." + t.key()), () -> {
                state.tab = t;
                rebuild();
            });
        }
        rail.selected(state.tab.ordinal(), true);
        add(rail);
    }

    private int nameWidth() {
        return Math.min(W - 28 - 90, Math.round(laura.name().length() * 6 * 1.25F));
    }

    @Override
    protected void drawBackground(Draw d, int mx, int my) {
        // The hearts rise behind the panel and show around its edges.
        hearts.draw(d, theme, mx, my, 1F);
        Palette p = theme.palette();
        theme.frame().panel(d, p, left, top, W, H);
        theme.frame().header(d, p, left + 4, top + 4, W - 8, 27);
        if (state.tab == MenuTab.HOME) {
            int rx = CX + 96;
            int rw = CW - 96;
            theme.frame().section(d, p, left + CX, top + CY, 90, CONTENT_H - 2);
            theme.frame().section(d, p, left + rx, top + CY, rw, 84);
            theme.frame().section(d, p, left + rx, top + CY + 90, rw, 42);
        }
    }

    @Override
    protected void drawForeground(Draw d, int mx, int my) {
        Palette p = theme.palette();
        drawHeader(d, p);
        switch (state.tab) {
            case HOME -> drawHome(d, p);
            case ORDERS -> small(d, words.get("lauramod.menu.combat"), CX, ORDERS_LABEL_Y, p.accent());
            case WORK -> drawWork(d, p);
            case FETCH -> drawFetch(d, p);
            case STYLE -> drawStyle(d, p);
            case SETTINGS -> drawSettings(d, p);
            case EMOTES -> {
            }
        }
        drawFooter(d, p);
    }

    private void drawHeader(Draw d, Palette p) {
        laura.icon("MOOD_" + laura.mood().name()).draw(d, left + 8, top + 8, p.text(), 1F);
        d.push(left + 28, top + 8, 1.25F);
        d.text(d.ellipsize(laura.name(), (int) ((W - 28 - 90) / 1.25F)), 0, 0, p.text());
        d.pop();
        int affection = laura.affection();
        int hx = 28;
        for (int i = 0; i < 10; i++) {
            int threshold = (i + 1) * 100;
            String heart = affection >= threshold ? "HEART_FULL" : affection >= threshold - 50 ? "HEART_HALF" : "HEART_EMPTY";
            d.push(left + hx + i * 9, top + 20, 0.5F);
            laura.icon(heart).draw(d, 0, 0, p.accent(), 1F);
            d.pop();
        }
        int count = laura.companionCount();
        if (count > 1) {
            String counter = (laura.companionIndex() + 1) + "/" + count;
            d.centered(counter, left + W - 51, top + 13, p.textSoft());
        }
        // Tab name on the right, mood in what is left between the hearts and it.
        String tabName = words.get("lauramod.menu.tab." + state.tab.key());
        int tabRight = W - 26 - (count > 1 ? 56 : 0);
        int tabWidth = Math.min(Math.round(d.width(tabName) * 0.75F) + 1, 70);
        small(d, tabName, tabRight - tabWidth, 21, tabWidth, p.accent());
        small(d, words.get("lauramod.mood." + laura.mood().key()), hx + 94, 21, tabRight - tabWidth - 4 - (hx + 94), p.textSoft());
    }

    private void drawFooter(Draw d, Palette p) {
        String upload = state.tab == MenuTab.STYLE ? laura.uploadMessage() : null;
        // The first day counts as day 1, not "together for 0 days".
        String line = upload != null ? upload
                : words.get("lauramod.menu.footer", laura.daysTogether() + 1, words.get("lauramod.mode." + laura.mode().key()),
                Math.round(laura.health()), Math.round(laura.maxHealth()));
        small(d, line, CX + 2, H - 13, upload != null && !laura.uploadOk() ? p.bad() : p.textSoft());
    }

    // ------------------------------------------------------------------ tab: home

    private void buildHome() {
        add(new EntityPreviewComponent(box("laura", CX + 4, CY + 4, 82, 132), words, laura.entity()).followMouse());
        ProgressComponent health = add(new ProgressComponent(box("health", CX + 19, CY + 145, 64, 8)));
        health.fraction = Math.min(1F, laura.health() / Math.max(1F, laura.maxHealth()));
        health.fillColor = (f, p) -> p.bad();

        int rx = CX + 96;
        int rw = CW - 96;
        int[] values = laura.needs();
        for (int i = 0; i < values.length; i++) {
            int base = NEED_COLORS[i % NEED_COLORS.length];
            ProgressComponent bar = add(new ProgressComponent(box("need", rx + 78, CY + 17 + i * 13, rw - 108, 8)));
            bar.fraction = values[i] / 100F;
            bar.fillColor = needColor(base);
            bar.visible = laura.needsEnabled();
            needBars.add(bar);
        }

        int x = rx;
        int y = CY + 138;
        int bw = (rw - 4 * GAP) / 5;
        iconButton(x, y, bw, 22, "lauramod.menu.hug", "HUG", null, () -> act(LauraAction.HUG, ""));
        iconButton(x + (bw + GAP), y, bw, 22, "lauramod.menu.kiss", "KISS", null, () -> act(LauraAction.KISS, ""));
        iconButton(x + (bw + GAP) * 2, y, bw, 22, "lauramod.menu.compliment", "COMPLIMENT", null, () -> act(LauraAction.COMPLIMENT, ""));
        iconButton(x + (bw + GAP) * 3, y, bw, 22, "lauramod.menu.inventory", "INVENTORY", null, () -> {
            act(LauraAction.INVENTORY, "");
            laura.close();
        });
        iconButton(x + (bw + GAP) * 4, y, bw, 22, "lauramod.menu.info", "TAB_INFO", null, () -> act(LauraAction.INFO, ""));
    }

    /** A need under 20 shows in the palette's bad colour, under 45 in its warning colour. */
    private static FillColor needColor(int base) {
        return (fraction, palette) -> fraction < 0.20F ? palette.bad() : fraction < 0.45F ? palette.warn() : base;
    }

    private void drawHome(Draw d, Palette p) {
        laura.icon("HEALTH").draw(d, left + CX + 3, top + CY + 141, p.text(), 1F);
        int rx = CX + 96;
        int rw = CW - 96;
        d.text(words.get("lauramod.menu.needs"), left + rx + 6, top + CY + 5, p.accent());
        int[] values = laura.needs();
        String[] keys = laura.needKeys();
        for (int i = 0; i < values.length; i++) {
            int ny = CY + 18 + i * 13;
            d.push(left + rx + 5, top + ny - 3, 0.75F);
            laura.icon("NEED_" + keys[i].toUpperCase(Locale.ROOT)).draw(d, 0, 0, p.text(), 1F);
            d.pop();
            small(d, words.get("lauramod.need." + keys[i]), rx + 20, ny, 56, p.text());
            small(d, values[i] + "%", rx + rw - 25, ny, 24, p.textSoft());
        }
        if (!laura.needsEnabled()) {
            String off = words.get("lauramod.menu.needs_off");
            d.centered(d.ellipsize(off, rw - 10), left + rx + rw / 2, top + CY + 44, p.textSoft());
        }

        int cy = CY + 90;
        laura.icon("DESIRE").draw(d, left + rx + 5, top + cy + 6, p.text(), 1F);
        String desire = laura.desire();
        if (desire != null) {
            IconArt icon = laura.desireIcon();
            if (icon != null) {
                icon.draw(d, left + rx + rw - 22, top + cy + 13, p.text(), 1F);
            }
            String text = laura.desireText() != null ? laura.desireText() : "?";
            int room = rw - 24 - (icon == null ? 6 : 26);
            small(d, words.get("lauramod.menu.desire"), rx + 24, cy + 6, room, p.accent());
            if (d.width(text) <= room) {
                d.text(text, left + rx + 24, top + cy + 16, p.text());
            } else {
                // Long wishes (and long languages) wrap on two smaller lines instead of leaving the card.
                d.smallWrapped(text, new Box("desire", left + rx + 24, top + cy + 14, room, 14), 2, p.text(), 0.75F);
            }
            long seconds = laura.desireSeconds();
            small(d, words.get("lauramod.menu.desire_time", seconds / 60, String.format(Locale.ROOT, "%02d", seconds % 60)),
                    rx + 24, cy + 29, room, p.textSoft());
        } else {
            small(d, words.get("lauramod.menu.no_desire"), rx + 24, cy + 16, rw - 30, p.textSoft());
        }
    }

    // ------------------------------------------------------------------ tab: orders

    private void buildOrders() {
        record Order(String key, String icon, LauraAction action, ButtonComponent.Role role) {
        }
        ButtonComponent.Role reset = ButtonComponent.Role.DESTRUCTIVE;
        List<Order> orders = List.of(
                new Order("lauramod.menu.follow", "FOLLOW", LauraAction.FOLLOW, null),
                new Order("lauramod.menu.stay", "STAY", LauraAction.STAY, null),
                new Order("lauramod.menu.wander", "WANDER", LauraAction.WANDER, null),
                new Order("lauramod.menu.come", "COME", LauraAction.COME, null),
                new Order("lauramod.menu.go_home", "GO_HOME", LauraAction.HOME, null),
                new Order("lauramod.menu.set_home", "SET_HOME", LauraAction.SET_HOME, null),
                new Order("lauramod.menu.sleep", "SLEEP", LauraAction.SLEEP, null),
                new Order("lauramod.menu.wake_up", "WAKE", LauraAction.WAKE_UP, null),
                new Order("lauramod.menu.eat", "EAT", LauraAction.EAT, null),
                new Order("lauramod.menu.stop", "STOP", LauraAction.STOP, reset),
                new Order("lauramod.menu.ungag", "UNGAG", LauraAction.UNGAG, reset),
                new Order("lauramod.menu.clear_home", "TRASH", LauraAction.CLEAR_HOME, reset));
        int bw = (CW - 2 * GAP) / 3;
        LauraMode mode = laura.mode();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            ButtonComponent b = button(CX + (i % 3) * (bw + GAP), CY + (i / 3) * (ORDER_H + GAP), bw, ORDER_H, o.key(), o.icon(), o.role(), () -> act(o.action(), ""));
            if (o.action() == LauraAction.UNGAG) {
                b.active = laura.gagged();
            }
            b.selected = o.action() == LauraAction.FOLLOW && mode == LauraMode.FOLLOW
                    || o.action() == LauraAction.STAY && mode == LauraMode.STAY
                    || o.action() == LauraAction.WANDER && mode == LauraMode.WANDER
                    || o.action() == LauraAction.HOME && mode == LauraMode.HOME;
        }
        int cy = ORDERS_CHIPS_Y;
        int chipW = (CW - 2 * GAP) / 3;
        for (CombatMode combat : CombatMode.values()) {
            ButtonComponent chip = button(CX + combat.ordinal() * (chipW + GAP), cy, chipW, 18, "lauramod.combat." + combat.key(),
                    combat == CombatMode.PASSIVE ? "COMBAT_PASSIVE" : "COMBAT_FIGHT", null, () -> act(LauraAction.COMBAT, combat.name()));
            chip.selected = laura.combat() == combat;
            chip.tooltip(words.get("lauramod.combat." + combat.key() + ".help"));
        }
        add(new ToggleComponent(box("pickup", CX, cy + 18 + 2 * GAP, CW, 16), words.get("lauramod.menu.pickup"), laura.pickingUp(),
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
        int bw = (CW - (cols - 1) * GAP) / cols;
        int bh = Math.min(40, (CONTENT_H - (rows - 1) * GAP) / rows);
        for (int i = 0; i < emotes.size(); i++) {
            Emote e = emotes.get(i);
            String name = words.get("lauramod.emote." + e.animationName());
            card(CX + (i % cols) * (bw + GAP), CY + (i / cols) * (bh + GAP), bw, bh, laura.icon("EMOTE_" + e.name()), name,
                    () -> act(LauraAction.EMOTE, e.name())).tooltip(name);
        }
    }

    // ------------------------------------------------------------------ tab: work

    private void buildWork() {
        String[] pages = {"jobs", "errands", "chests"};
        int pw = (CW - 2 * GAP) / 3;
        for (int i = 0; i < pages.length; i++) {
            int page = i;
            button(CX + i * (pw + GAP), CY, pw, 16, "lauramod.menu.work." + pages[i], null, null, () -> {
                MenuState.workPage = page;
                rebuild();
            }).selected(MenuState.workPage == i);
        }
        int y = CY + 22;
        switch (MenuState.workPage) {
            case 0 -> buildJobs(CX, y);
            case 1 -> buildErrands(CX, y);
            default -> buildChests(CX, y);
        }
    }

    private void buildJobs(int x, int y) {
        String jobs = laura.jobs();
        int jw = (CW - 2 * GAP) / 3;
        LauraJob[] list = {LauraJob.LUMBERJACK, LauraJob.FARMER, LauraJob.COOK};
        String[] icons = {"JOB_LUMBERJACK", "JOB_FARMER", "JOB_COOK"};
        for (int i = 0; i < list.length; i++) {
            LauraJob job = list[i];
            boolean on = jobs.contains(job.name());
            CardButton b = card(x + i * (jw + GAP), y, jw, 60, laura.icon(icons[i]), words.get("lauramod.job." + job.key()), () -> {
                act(LauraAction.JOB, job.name() + (on ? ":off" : ":on:" + MenuState.jobRadius));
                laura.requestStatus();
            });
            b.selected = on;
            b.tooltip(words.get("lauramod.job." + job.key() + ".help"), words.get(on ? "lauramod.menu.job.stop" : "lauramod.menu.job.start"));
        }
        add(new SliderComponent(box("radius", x, y + 68, CW, 20), 3, 24, MenuState.jobRadius, v -> MenuState.jobRadius = v)
                .step(1).label(v -> words.get("lauramod.menu.job.radius", v)));
        int half = (CW - GAP) / 2;
        button(x, y + 96, half, 22, "lauramod.menu.job.back_to_work", "PLUS", ButtonComponent.Role.PRIMARY, () -> act(LauraAction.WORK, ""));
        button(x + half + GAP, y + 96, half, 22, "lauramod.menu.job.stop_all", "STOP", ButtonComponent.Role.DESTRUCTIVE, () -> act(LauraAction.JOB, "ALL:off"));
    }

    private void buildErrands(int x, int y) {
        int bw = (CW - 2 * GAP) / 3;
        String[] tasks = {"CHOP_TREE", "HARVEST", "COOK"};
        String[] keys = {"lauramod.task.chop_tree", "lauramod.task.harvest", "lauramod.task.cook"};
        String[] icons = {"JOB_LUMBERJACK", "JOB_FARMER", "JOB_COOK"};
        for (int i = 0; i < tasks.length; i++) {
            String task = tasks[i];
            button(x + i * (bw + GAP), y, bw, 22, keys[i], icons[i], null,
                    () -> act(LauraAction.TASK, task + ":" + (shift() ? "queue" : "now") + ":" + MenuState.jobRadius))
                    .tooltip(words.get("lauramod.menu.shift_hint"));
        }
        queueList = add(new ListComponent<String>(box("queue", x, y + 28, CW - 58 - GAP, 94), 12,
                (d, theme, row, box, hovered, selected) -> d.textFit(row, box.x() + 2, box.y() + 2, box.w() - 4, theme.palette().text()),
                (row, index, mx, my) -> {
                    int queueIndex = laura.taskCurrent() != null ? index : index + 1;
                    if (queueIndex > 0) {
                        act(LauraAction.QUEUE, "remove:" + queueIndex);
                        laura.requestStatus();
                    }
                }));
        queueList.tooltip(words.get("lauramod.menu.queue.help"));
        queueList.setItems(queueRows());
        button(x + CW - 58, y + 28, 58, 20, "lauramod.menu.queue.clear", "TRASH", ButtonComponent.Role.DESTRUCTIVE, () -> act(LauraAction.QUEUE, "clear"));
        button(x + CW - 58, y + 28 + 20 + GAP, 58, 20, "lauramod.menu.stop", "STOP", ButtonComponent.Role.DESTRUCTIVE, () -> act(LauraAction.STOP, ""));
    }

    private List<String> queueRows() {
        List<String> rows = new ArrayList<>();
        if (laura.taskCurrent() != null) {
            rows.add("> " + laura.taskCurrent());
        }
        int n = 1;
        for (String task : laura.taskQueue()) {
            rows.add(n++ + ". " + task);
        }
        return rows;
    }

    private void buildChests(int x, int y) {
        int bw = (CW - 3 * GAP) / 4;
        boolean looking = laura.lookedAtBlock() != null;
        ChestPurpose[] purposes = ChestPurpose.values();
        for (int i = 0; i < purposes.length; i++) {
            ChestPurpose p = purposes[i];
            CardButton b = card(x + (i % 4) * (bw + GAP), y + (i / 4) * (36 + GAP), bw, 36, laura.icon("CHEST_" + p.name()),
                    words.get("lauramod.chest." + p.key()), () -> act(LauraAction.CHEST, p.name()));
            b.active = looking;
            b.tooltip(words.get("lauramod.chest." + p.key() + ".help"));
        }
        int half = (CW - GAP) / 2;
        button(x, y + 80, half, 20, "lauramod.menu.chest.remove", "TRASH", ButtonComponent.Role.DESTRUCTIVE,
                () -> act(LauraAction.CHEST, "remove")).active = looking;
        button(x + half + GAP, y + 80, half, 20, "lauramod.menu.chest.list", "QUEUE", null, () -> laura.sendCommand("laura chest list"));
    }

    private void drawWork(Draw d, Palette p) {
        int y = CY + 22;
        switch (MenuState.workPage) {
            case 0 -> small(d, words.get("lauramod.menu.job.help"), CX, y + 124, p.textSoft());
            case 1 -> small(d, words.get("lauramod.menu.shift_hint"), CX, y + 128, p.textSoft());
            default -> {
                int[] block = laura.lookedAtBlock();
                String hint = block != null ? words.get("lauramod.menu.chest.looking", block[0], block[1], block[2])
                        : words.get("lauramod.menu.chest.look_first");
                small(d, hint, CX, y + 106, block != null ? p.text() : p.bad());
                small(d, words.get("lauramod.menu.chest.help"), CX, y + 118, p.textSoft());
                if (laura.chests() >= 0) {
                    small(d, words.get("lauramod.menu.chest.count", laura.chests()), CX, y + 130, p.textSoft());
                }
            }
        }
    }

    // ------------------------------------------------------------------ tab: fetch

    private void buildFetch() {
        picker = add(new ItemPickerComponent(box("fetch", CX, CY, CW, FETCH_PICKER_H), words, id -> {
            state.fetchItem = id;
            rebuild();
        }));
        if (state.fetchItem != null) {
            picker.selected(state.fetchItem);
        }
        if (!state.search.isEmpty()) {
            picker.search(state.search);
        }
        int cy = CY + FETCH_PICKER_H + GAP;
        List<String> labels = new ArrayList<>();
        int chosen = 0;
        for (int i = 0; i < FETCH_COUNTS.length; i++) {
            labels.add(String.valueOf(FETCH_COUNTS[i]));
            if (FETCH_COUNTS[i] == MenuState.fetchCount) {
                chosen = i;
            }
        }
        add(new ToggleGroupComponent(box("count", CX, cy, FETCH_COUNTS.length * 26, 18), labels, ToggleGroupComponent.Mode.SINGLE,
                Set.of(chosen), on -> on.stream().findFirst().ifPresent(i -> MenuState.fetchCount = FETCH_COUNTS[i])).required(true));
        int by = cy + 18 + GAP;
        int bw = (CW - 2 * GAP) / 3;
        boolean can = state.fetchItem != null && laura.fetchEnabled();
        button(CX, by, bw, 18, "lauramod.menu.fetch.go", "FOLLOW", ButtonComponent.Role.PRIMARY, () -> fetch(shift())).active = can;
        button(CX + bw + GAP, by, bw, 18, "lauramod.menu.fetch.queue", "QUEUE", null, () -> fetch(true)).active = can;
        ButtonComponent held = button(CX + 2 * (bw + GAP), by, CW - 2 * (bw + GAP), 18, "lauramod.menu.fetch.held", "INVENTORY", null, () -> {
            act(LauraAction.FETCH, "held|" + MenuState.fetchCount);
            laura.close();
        });
        held.active = laura.fetchEnabled();
        held.tooltip(words.get("lauramod.menu.fetch.held.help"));
    }

    private void fetch(boolean queue) {
        if (state.fetchItem == null) {
            return;
        }
        act(LauraAction.FETCH, state.fetchItem + "|" + MenuState.fetchCount + (queue ? "|queue" : ""));
        laura.close();
    }

    private void drawFetch(Draw d, Palette p) {
        String line = state.fetchItem != null
                ? words.get("lauramod.menu.fetch.selected", laura.itemName(state.fetchItem), MenuState.fetchCount)
                : words.get("lauramod.menu.fetch.pick");
        int chipsEnd = FETCH_COUNTS.length * 26 + 6;
        int lineY = CY + FETCH_PICKER_H + GAP;
        d.smallWrapped(line, new Box("selected", left + CX + chipsEnd, top + lineY + 2, CW - chipsEnd, 16), 2,
                state.fetchItem != null ? p.text() : p.textSoft(), 0.75F);
        if (!laura.fetchEnabled()) {
            small(d, words.get("lauramod.menu.fetch.disabled"), CX, lineY + 18 + GAP + 18 + 6, p.bad());
        }
    }

    // ------------------------------------------------------------------ tab: style

    private void buildStyle() {
        String[] pages = {"skins", "models"};
        String[] icons = {"SKIN", "MODEL"};
        int pw = (CW - GAP) / 2;
        for (int i = 0; i < pages.length; i++) {
            int page = i;
            button(CX + i * (pw + GAP), CY, pw, 16, "lauramod.menu.style." + pages[i], icons[i], null, () -> {
                MenuState.stylePage = page;
                rebuild();
            }).selected(MenuState.stylePage == i);
        }
        if (MenuState.stylePage == 0) {
            buildSkins(CX, CY + 20);
        } else {
            buildModels(CX, CY + 20);
        }
    }

    private void buildSkins(int x, int y) {
        List<String> builtin = SkinRef.BUILTIN;
        int tw = (CW - (builtin.size() - 1) * GAP) / builtin.size();
        for (int i = 0; i < builtin.size(); i++) {
            String name = builtin.get(i);
            CardButton b = card(x + i * (tw + GAP), y, tw, 30, laura.skinFace(name), null, () -> laura.setSkin("builtin:" + name, true));
            b.tooltip(words.get("lauramod.skin.builtin." + name));
            b.selected = laura.skinRaw().equals("builtin:" + name);
        }
        int half = (CW - GAP) / 2;
        ListComponent<String[]> files = add(new ListComponent<String[]>(box("server skins", x, y + 42, half, 44), 11,
                (d, theme, item, row, hovered, selected) -> d.textFit(item[0], row.x() + 2, row.y() + 2, row.w() - 4, theme.palette().text()),
                (Consumer<String[]>) item -> laura.setSkin("server:" + item[0], laura.slim())));
        files.setItems(laura.serverFiles(AssetKind.SKIN));
        files.tooltip(words.get("lauramod.menu.skin.server_files.help"));
        ListComponent<Path> local = add(new ListComponent<Path>(box("local skins", x + half + GAP, y + 42, half, 44), 11,
                (d, theme, item, row, hovered, selected) -> d.textFit(item.getFileName().toString(), row.x() + 2, row.y() + 2, row.w() - 4, theme.palette().text()),
                (Consumer<Path>) laura::useLocalSkin));
        local.setItems(laura.localFiles("skins", ".png"));
        local.tooltip(words.get("lauramod.menu.skin.local_files.help"));
        add(new ToggleComponent(box("slim", x, y + 44 + 42 + GAP, half, 16), words.get("lauramod.menu.skin.slim"), laura.slim(),
                v -> laura.setSkin(laura.skinRaw(), v)));
        button(x + half + GAP, y + 44 + 42 + GAP, half, 16, "lauramod.menu.skin.reset", "REFRESH", ButtonComponent.Role.DESTRUCTIVE,
                () -> laura.setSkin("reset", true));

        TextFieldComponent url = add(new TextFieldComponent(box("url", x, y + 104 + GAP, CW - 56 - GAP, 18), state.url, text -> state.url = text)
                .placeholder(words.get("lauramod.menu.skin.url")).maxLength(512).onSubmit(text -> applyUrl()));
        url.active = laura.allowUrlSkins();
        button(x + CW - 56, y + 104 + GAP, 56, 18, "lauramod.menu.apply", "CHECK", ButtonComponent.Role.PRIMARY, this::applyUrl).active = laura.allowUrlSkins();
        TextFieldComponent player = add(new TextFieldComponent(box("player", x, y + 104 + GAP + 18 + GAP, CW - 56 - GAP, 18), state.player, text -> state.player = text)
                .placeholder(words.get("lauramod.menu.skin.player")).maxLength(16).onSubmit(text -> applyPlayer()));
        player.active = laura.allowPlayerNameSkins();
        button(x + CW - 56, y + 104 + GAP + 18 + GAP, 56, 18, "lauramod.menu.apply", "CHECK", ButtonComponent.Role.PRIMARY, this::applyPlayer).active = laura.allowPlayerNameSkins();
    }

    private void applyUrl() {
        if (!state.url.isBlank()) {
            laura.setSkin("url:" + state.url.trim(), laura.slim());
        }
    }

    private void applyPlayer() {
        if (!state.player.isBlank()) {
            laura.setSkin("player:" + state.player.trim(), true);
        }
    }

    private void buildModels(int x, int y) {
        List<String> models = new ArrayList<>();
        models.add("");
        for (String[] e : laura.serverFiles(AssetKind.MODEL)) {
            models.add("server:" + e[0]);
        }
        List<Path> uploads = !laura.singleplayer() && laura.allowModelUploads() ? laura.localFiles("models", ".bbmodel") : List.of();
        int listH = uploads.isEmpty() ? 104 : 60;
        ListComponent<String> list = add(new ListComponent<String>(box("models", x, y, CW, listH), 12,
                (d, theme, item, row, hovered, selected) -> d.textFit(item.isEmpty() ? words.get("lauramod.menu.model.default")
                        : item.substring(item.indexOf(':') + 1), row.x() + 2, row.y() + 2, row.w() - 4, theme.palette().text()),
                (Consumer<String>) item -> laura.setModel(item.isEmpty() ? "reset" : item)));
        list.setItems(models);
        list.active = laura.allowCustomModels();
        if (!uploads.isEmpty()) {
            ListComponent<Path> local = add(new ListComponent<Path>(box("model uploads", x, y + 60 + GAP, CW, 42), 11,
                    (d, theme, item, row, hovered, selected) -> d.textFit(words.get("lauramod.menu.model.upload", item.getFileName().toString()),
                            row.x() + 2, row.y() + 2, row.w() - 4, theme.palette().text()),
                    (Consumer<Path>) item -> laura.upload(AssetKind.MODEL, item)));
            local.setItems(uploads);
        }
        int half = (CW - GAP) / 2;
        button(x, y + 104 + GAP, half, 20, "lauramod.menu.model.folder", "MODEL", null, () -> laura.openFolder("models"));
        button(x + half + GAP, y + 104 + GAP, half, 20, "lauramod.menu.skin.folder", "SKIN", null, () -> laura.openFolder("skins"));
    }

    private void drawStyle(Draw d, Palette p) {
        int y = CY + 20;
        if (MenuState.stylePage == 0) {
            int half = (CW - GAP) / 2;
            small(d, words.get("lauramod.menu.skin.server_files"), CX, y + 34, half, p.accent());
            small(d, words.get("lauramod.menu.skin.local_files"), CX + half + GAP, y + 34, half, p.accent());
        } else {
            small(d, words.get("lauramod.menu.model.hint"), CX, y + 134, p.textSoft());
        }
    }

    // ------------------------------------------------------------------ tab: settings

    private void buildSettings() {
        String name = state.rename != null ? state.rename : laura.name();
        renameField = add(new TextFieldComponent(box("name", CX, CY, CW - 70 - GAP, 18), name, text -> state.rename = text)
                .placeholder(words.get("lauramod.menu.rename.hint")).maxLength(32).onSubmit(text -> rename()));
        button(CX + CW - 70, CY, 70, 18, "lauramod.menu.rename", "PENCIL", ButtonComponent.Role.PRIMARY, this::rename);
        if (state.focusRename) {
            state.focusRename = false;
            focus(renameField);
            renameField.moveCursor(0, false);
            renameField.moveCursor(name.length(), true);
        }
        int ty = CY + 26;
        int half = (CW - 8) / 2;
        List<ConfigFile.BoolValue> toggles = laura.clientToggles();
        for (int i = 0; i < toggles.size(); i++) {
            ConfigFile.BoolValue value = toggles.get(i);
            add(new ToggleComponent(box(value.key(), CX + (i % 2) * (half + 8), ty + (i / 2) * 17, half, 14),
                    words.get("lauramod.client." + value.key()), value.get(), v -> {
                value.set(v);
                laura.saveClientConfig();
            })).tooltip(words.get("lauramod.client." + value.key() + ".help"));
        }
        int by = ty + ((toggles.size() + 1) / 2) * 17 + 6;
        int third = (CW - 2 * GAP) / 3;
        button(CX, by, third, 20, "lauramod.menu.config_folder", "TAB_SETTINGS", null, () -> laura.openFolder(""));
        button(CX + third + GAP, by, third, 20, "lauramod.menu.call", "CALL", ButtonComponent.Role.PRIMARY, () -> act(LauraAction.COME, ""));
        button(CX + 2 * (third + GAP), by, third, 20, state.dismissArmed ? "lauramod.menu.dismiss_confirm" : "lauramod.menu.dismiss",
                "TRASH", ButtonComponent.Role.DESTRUCTIVE, () -> {
                    if (!state.dismissArmed) {
                        state.dismissArmed = true;
                        rebuild();
                    } else {
                        laura.sendCommand("laura dismiss");
                        laura.close();
                    }
                }).tooltip(words.get("lauramod.menu.dismiss.help"));
    }

    private void rename() {
        String name = state.rename == null ? "" : state.rename.trim();
        if (!name.isEmpty() && !name.equals(laura.name())) {
            act(LauraAction.RENAME, name);
            state.rename = null;
            BaseComponent.click();
        }
    }

    private void drawSettings(Draw d, Palette p) {
        int y = CY + 26 + 4 * 17 + 32;
        List<String> lines = new ArrayList<>();
        if (laura.owner() != null) {
            lines.add(words.get("lauramod.menu.owner", laura.owner()));
        }
        lines.add(laura.home() != null ? words.get("lauramod.menu.home_at", laura.home()) : words.get("lauramod.menu.no_home"));
        if (laura.annoyance() != null) {
            lines.add(words.get("lauramod.menu.annoyance", words.get("lauramod.annoyance." + laura.annoyance().toLowerCase(Locale.ROOT))));
        }
        for (int i = 0; i < lines.size(); i++) {
            small(d, lines.get(i), CX, y + i * 9, p.textSoft());
        }
    }
}
