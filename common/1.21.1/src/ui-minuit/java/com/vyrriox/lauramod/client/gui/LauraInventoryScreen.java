package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.ui.LauraTheme;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import dev.dryaides.minuit.Box;
import dev.dryaides.minuit.ButtonComponent;
import dev.dryaides.minuit.Draw;
import dev.dryaides.minuit.EntityPreviewComponent;
import dev.dryaides.minuit.EntityView;
import dev.dryaides.minuit.Palette;
import dev.dryaides.minuit.PanelComponent;
import dev.dryaides.minuit.PlayerInventoryComponent;
import dev.dryaides.minuit.SlotComponent;
import dev.dryaides.minuit.SlotGridComponent;
import dev.dryaides.minuit.SlotLayout;
import dev.dryaides.minuit.client.MinuitContainerScreen;
import dev.dryaides.minuit.client.MinuitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

/**
 * Laura's bag and equipment: her preview and gear on a side panel, the bag and the player's
 * inventory on the main one. The game keeps every click; the panel only places the wells.
 *
 * @author vyrriox
 */
public class LauraInventoryScreen extends MinuitContainerScreen<LauraInventoryMenu> {
    private static final int SIDE = 80;
    private static final int SIDE_H = 118;

    public LauraInventoryScreen(LauraInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, width(), height(menu.rows()));
        this.inventoryLabelX = LauraInventoryMenu.MAIN_LEFT;
        this.inventoryLabelY = LauraInventoryMenu.playerTop(menu.rows()) - 11;
        this.titleLabelX = LauraInventoryMenu.MAIN_LEFT;
        this.titleLabelY = 6;
    }

    private static int width() {
        return LauraInventoryMenu.MAIN_LEFT + 9 * 18 + 8;
    }

    private static int height(int rows) {
        return LauraInventoryMenu.playerTop(rows) + 76 + 7;
    }

    private LauraEntity laura() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity e = mc.level.getEntity(menu.entityId());
        return e instanceof LauraEntity l ? l : null;
    }

    @Override
    protected PanelComponent build(int leftPos, int topPos) {
        int rows = menu.rows();
        int w = width();
        int h = height(rows);
        return new PanelComponent(LauraTheme.get(), I18n::get, leftPos, topPos) {
            {
                if (rows > 0) {
                    add(new SlotGridComponent(at(SlotLayout.grid("bag", LauraInventoryMenu.MAIN_LEFT, LauraInventoryMenu.TOP, rows, 9)), rows, 9));
                }
                for (int i = 0; i < 4; i++) {
                    add(slot("armor", LauraInventoryMenu.ARMOR_X, LauraInventoryMenu.TOP + i * 18));
                }
                add(slot("hand", LauraInventoryMenu.HANDS_X, LauraInventoryMenu.TOP + 36));
                add(slot("offhand", LauraInventoryMenu.HANDS_X, LauraInventoryMenu.TOP + 54));
                add(slot("back", LauraInventoryMenu.HANDS_X, LauraInventoryMenu.TOP + 18)).empty = KawaiiIcons.of(Icon.BACKPACK);
                add(new PlayerInventoryComponent(at(SlotLayout.playerInventory("inventory", LauraInventoryMenu.MAIN_LEFT,
                        LauraInventoryMenu.playerTop(rows)))));
                LauraEntity laura = laura();
                if (laura != null) {
                    add(new EntityPreviewComponent(at(new Box("laura", 27, 14, 32, 76)), words,
                            new EntityView("lauramod:laura", laura, false, false)).followMouse());
                }
                add(new ButtonComponent(at(new Box("back to menu", SIDE - 22, 4, 16, 16)), "", () -> {
                    LauraEntity her = laura();
                    onClose();
                    if (her != null) {
                        MinuitScreen.show(new LauraMenuScreen(her));
                    }
                }).icon(KawaiiIcons.of(Icon.TAB_HOME)).style(ButtonComponent.Style.GHOST)).tooltip(words.get("lauramod.menu.back"));
            }

            private SlotComponent slot(String name, int x, int y) {
                return new SlotComponent(at(new Box(name, x - 1, y - 1, 18, 18)));
            }

            @Override
            public int width() {
                return w;
            }

            @Override
            public int height() {
                return h;
            }

            @Override
            protected void drawBackground(Draw d, int mx, int my) {
                Palette p = theme.palette();
                theme.frame().panel(d, p, left, top, SIDE + 4, SIDE_H);
                theme.frame().panel(d, p, left + SIDE, top, w - SIDE, h);
                theme.frame().well(d, p, left + 27, top + 14, 32, 76, false);
            }

            @Override
            protected void drawForeground(Draw d, int mx, int my) {
                LauraEntity laura = laura();
                if (laura != null) {
                    Palette p = theme.palette();
                    d.push(left + 6, top + 98, 0.75F);
                    KawaiiIcons.of(Icon.HEART_FULL).draw(d, 0, 0, p.text(), 1F);
                    d.pop();
                    d.small(Math.round(laura.getHealth()) + " / " + Math.round(laura.getMaxHealth()),
                            new Box("health", left + 18, top + 100, SIDE - 22, 6), p.text(), 0.75F, d.guiScale());
                }
            }
        };
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        boolean outsideMain = mx < left + SIDE || my < top || mx >= left + imageWidth || my >= top + imageHeight;
        boolean outsideSide = mx < left || my < top || mx >= left + SIDE + 4 || my >= top + SIDE_H;
        return outsideMain && outsideSide;
    }
}
