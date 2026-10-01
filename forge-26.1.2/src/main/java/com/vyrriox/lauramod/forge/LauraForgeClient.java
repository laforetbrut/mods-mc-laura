package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.LauraInventoryScreen;
import com.vyrriox.lauramod.client.gui.NeedsHud;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client entry point. Only loaded on the physical client.
 *
 * @author vyrriox
 */
final class LauraForgeClient {
    private LauraForgeClient() {
    }

    static void init(BusGroup modBus) {
        FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(() -> {
            MenuScreens.register(LauraForge.INVENTORY_MENU.get(), LauraInventoryScreen::new);
            LauraClient.init();
            LauraClient.afterRegistries();
        }));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(
                event -> event.registerEntityRenderer(LauraForge.LAURA.get(), LauraRenderer::new));
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            for (KeyMapping key : LauraClient.keyMappings()) {
                event.register(key);
            }
        });
        RegisterClientReloadListenersEvent.BUS.addListener(
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> LauraClient.onResourceReload()));
        // Added last to the root layer stack, so the HUD is drawn above every other layer.
        AddGuiOverlayLayersEvent.BUS.addListener(
                event -> event.getLayeredDraw().add(LauraMod.id("needs_hud"), (graphics, delta) -> NeedsHud.render(graphics)));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> LauraClient.tick());
    }
}
