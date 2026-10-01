package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.LauraInventoryScreen;
import com.vyrriox.lauramod.client.gui.NeedsHud;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client entry point. Only loaded on the physical client.
 *
 * @author vyrriox
 */
final class LauraForgeClient {
    private LauraForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            LauraClient.init();
            LauraClient.afterRegistries();
            MenuScreens.register(LauraForge.INVENTORY_MENU.get(), LauraInventoryScreen::new);
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(LauraForge.LAURA.get(), LauraRenderer::new));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            for (KeyMapping key : LauraClient.keyMappings()) {
                event.register(key);
            }
        });
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) manager -> LauraClient.onResourceReload()));
        // GUI layers are Forge overlays on Minecraft 1.20.1.
        modBus.addListener((RegisterGuiOverlaysEvent event) ->
                event.registerAboveAll("needs_hud", (gui, graphics, partialTick, width, height) -> NeedsHud.render(graphics)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                LauraClient.tick();
            }
        });
    }
}
