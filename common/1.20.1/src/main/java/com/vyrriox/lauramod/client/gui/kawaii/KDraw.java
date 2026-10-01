package com.vyrriox.lauramod.client.gui.kawaii;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Thin drawing adapter over the vanilla GUI renderer. Every screen of the mod draws through it, so
 * a Minecraft version change only touches this class.
 *
 * @author vyrriox
 */
public final class KDraw {
    public static final ResourceLocation ICONS = LauraMod.id("textures/gui/kawaii_icons.png");

    private final GuiGraphics g;
    private final Font font;

    public KDraw(GuiGraphics g) {
        this.g = g;
        this.font = Minecraft.getInstance().font;
    }

    public GuiGraphics raw() {
        return g;
    }

    public Font font() {
        return font;
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        if (x1 > x0 && y1 > y0) {
            g.fill(x0, y0, x1, y1, argb);
        }
    }

    public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        if (x1 > x0 && y1 > y0) {
            g.fillGradient(x0, y0, x1, y1, top, bottom);
        }
    }

    public void text(Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public void text(String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public void text(FormattedCharSequence text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public void textShadow(Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, true);
    }

    public void centered(Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    public int width(Component text) {
        return font.width(text);
    }

    public int width(String text) {
        return font.width(text);
    }

    /** Text cut with "..." so it never goes past {@code maxWidth} pixels. */
    public void textFit(Component text, int x, int y, int maxWidth, int color) {
        text(ellipsize(text, maxWidth), x, y, color);
    }

    public void textFit(String text, int x, int y, int maxWidth, int color) {
        textFit(Component.literal(text), x, y, maxWidth, color);
    }

    /** The text cut to fit {@code maxWidth} pixels, with "..." when it was too long. */
    public Component ellipsize(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("...")));
        return Component.literal(cut.stripTrailing() + "...").withStyle(text.getStyle());
    }

    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, x, y);
    }

    public void itemDecorations(ItemStack stack, int x, int y) {
        g.renderItemDecorations(font, stack, x, y);
    }

    /** A 16x16 icon from the kawaii atlas (or its vanilla item). */
    public void icon(Icon icon, int x, int y) {
        ItemStack stack = icon.item();
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
            return;
        }
        g.blit(ICONS, x, y, icon.u, icon.v, 16, 16, 256, 256);
    }

    /** A see-through atlas icon (hints in empty slots). */
    public void iconFaded(Icon icon, int x, int y, float alpha) {
        RenderSystem.enableBlend();
        g.setColor(1F, 1F, 1F, alpha);
        g.blit(ICONS, x, y, icon.u, icon.v, 16, 16, 256, 256);
        g.setColor(1F, 1F, 1F, 1F);
    }

    /** An icon drawn at another size (8, 12, 24, 32...). */
    public void icon(Icon icon, int x, int y, int size) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float s = size / 16F;
        g.pose().scale(s, s, 1);
        icon(icon, 0, 0);
        g.pose().popPose();
    }

    /** The face of a 64x64 player skin (with its hat layer). */
    public void face(ResourceLocation skin, int x, int y, int size) {
        g.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
        g.blit(skin, x, y, size, size, 40, 8, 8, 8, 64, 64);
    }

    /** A whole texture stretched over a rectangle. */
    public void texture(ResourceLocation texture, int x, int y, int w, int h, int texW, int texH) {
        g.blit(texture, x, y, w, h, 0, 0, texW, texH, texW, texH);
    }

    public void scissor(int x0, int y0, int x1, int y1) {
        g.enableScissor(x0, y0, x1, y1);
    }

    public void endScissor() {
        g.disableScissor();
    }

    public void push(float x, float y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
    }

    public void pop() {
        g.pose().popPose();
    }

    /** Draws a living entity that looks at the mouse, like the player in the inventory. */
    public void entity(LivingEntity entity, int x0, int y0, int x1, int y1, int scale, float mouseX, float mouseY) {
        // Same framing as later versions: the entity is centered in the box and cut at its edges.
        float centerX = (x0 + x1) / 2F;
        float centerY = (y0 + y1) / 2F;
        int feetY = Math.round(centerY + scale * (entity.getBbHeight() / 2F + 0.0625F));
        g.enableScissor(x0, y0, x1, y1);
        LauraRenderer.preview(() -> InventoryScreen.renderEntityInInventoryFollowsMouse(g, Math.round(centerX), feetY, scale, centerX - mouseX, centerY - mouseY, entity));
        g.disableScissor();
    }

    public void tooltip(Component text, int x, int y) {
        g.renderTooltip(font, text, x, y);
    }

    public void tooltip(java.util.List<Component> lines, int x, int y) {
        g.renderComponentTooltip(font, lines, x, y);
    }
}
