package com.vyrriox.lauramod.client.gui;

import com.vyrriox.lauramod.client.gui.kawaii.Icon;
import com.vyrriox.lauramod.client.gui.kawaii.KButton;
import com.vyrriox.lauramod.client.gui.kawaii.KDraw;
import com.vyrriox.lauramod.client.gui.kawaii.Kawaii;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Laura's bag and equipment, drawn with the pastel theme of her menu.
 *
 * @author vyrriox
 */
public class LauraInventoryScreen extends AbstractContainerScreen<LauraInventoryMenu> {
    private static final int SIDE = 80;
    private KButton backButton;

    public LauraInventoryScreen(LauraInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = LauraInventoryMenu.MAIN_LEFT + 9 * 18 + 8;
        this.imageHeight = LauraInventoryMenu.playerTop(menu.rows()) + 76 + 7;
        this.inventoryLabelX = LauraInventoryMenu.MAIN_LEFT;
        this.inventoryLabelY = LauraInventoryMenu.playerTop(menu.rows()) - 11;
        this.titleLabelX = LauraInventoryMenu.MAIN_LEFT;
        this.titleLabelY = 6;
    }

    private LauraEntity laura() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Entity e = minecraft.level.getEntity(menu.entityId());
        return e instanceof LauraEntity l ? l : null;
    }

    @Override
    protected void init() {
        super.init();
        backButton = new KButton(leftPos + SIDE - 22, topPos + 4, 16, 16, null, c -> {
            LauraEntity laura = laura();
            onClose();
            if (laura != null && minecraft != null) {
                minecraft.setScreen(new LauraMenuScreen(laura));
            }
        }).icon(Icon.TAB_HOME).style(KButton.Style.GHOST);
        backButton.tooltip(Component.translatable("lauramod.menu.back"));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        KDraw d = new KDraw(graphics);
        int x = leftPos;
        int y = topPos;
        // Side panel: preview and equipment.
        Kawaii.panel(d, x, y, SIDE + 4, 118, Kawaii.LAVENDER, 0xFFF6F0FF);
        // Main panel: bag and player inventory.
        Kawaii.panel(d, x + SIDE, y, imageWidth - SIDE, imageHeight, Kawaii.PINK, Kawaii.CREAM);
        int playerTop = LauraInventoryMenu.playerTop(menu.rows());
        Kawaii.rounded(d, x + LauraInventoryMenu.MAIN_LEFT - 3, y + LauraInventoryMenu.TOP - 3, 9 * 18 + 6, menu.rows() * 18 + 6, 4, Kawaii.BLUSH);
        Kawaii.rounded(d, x + LauraInventoryMenu.MAIN_LEFT - 3, y + playerTop - 3, 9 * 18 + 6, 3 * 18 + 6, 4, 0xFFF3ECFF);
        Kawaii.rounded(d, x + LauraInventoryMenu.MAIN_LEFT - 3, y + playerTop + 55, 9 * 18 + 6, 18 + 6, 4, 0xFFF3ECFF);
        Kawaii.rounded(d, x + 27, y + 14, 32, 76, 4, 0xFFFFFFFF);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            Kawaii.rounded(d, sx, sy, 18, 18, 3, 0xFFE9C6D9);
            Kawaii.rounded(d, sx + 1, sy + 1, 16, 16, 2, 0xFFFFF7FB);
        }
        Slot back = menu.slots.get(menu.bagSlots() + LauraInventoryMenu.BACK_INDEX);
        if (!back.hasItem()) {
            d.iconFaded(Icon.BACKPACK, x + back.x, y + back.y, 0.35F);
        }
        LauraEntity laura = laura();
        if (laura != null) {
            d.entity(laura, x + 27, y + 14, x + 59, y + 90, 30, mouseX, mouseY);
            Kawaii.heart(d, x + 8, y + 100, 1, Kawaii.PINK_DEEP);
            d.push(x + 18, y + 100, 0.75F);
            d.text(Component.literal(Math.round(laura.getHealth()) + " / " + Math.round(laura.getMaxHealth())), 0, 0, Kawaii.PLUM);
            d.pop();
        }
        backButton.draw(d, mouseX, mouseY, 1F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Long names and long translations are cut at the edge of the bag panel.
        KDraw d = new KDraw(graphics);
        d.textFit(title, titleLabelX, titleLabelY, 9 * 18, Kawaii.PLUM);
        d.textFit(playerInventoryTitle, inventoryLabelX, inventoryLabelY, 9 * 18, Kawaii.PLUM_SOFT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (backButton.isOver(mouseX, mouseY) && backButton.tooltip != null) {
            new KDraw(graphics).tooltip(backButton.tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (backButton.mouseClicked(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        boolean outsideMain = mx < left + SIDE || my < top || mx >= left + imageWidth || my >= top + imageHeight;
        boolean outsideSide = mx < left || my < top || mx >= left + SIDE + 4 || my >= top + 118;
        return outsideMain && outsideSide;
    }
}
