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
        LauraForge.listen(modBus, FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            MenuScreens.register(LauraForge.INVENTORY_MENU.get(), LauraInventoryScreen::new);
            LauraClient.init();
            LauraClient.afterRegistries();
        }));
        LauraForge.listen(modBus, EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(LauraForge.LAURA.get(), LauraRenderer::new));
        LauraForge.listen(modBus, RegisterKeyMappingsEvent.class, event -> {
            for (KeyMapping key : LauraClient.keyMappings()) {
                event.register(key);
            }
        });
        LauraForge.listen(modBus, RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> LauraClient.onResourceReload()));
        // Added last to the root layer stack, so the HUD is drawn above every other layer.
        LauraForge.listen(modBus, AddGuiOverlayLayersEvent.class,
                event -> event.getLayeredDraw().add(LauraMod.id("needs_hud"), (graphics, delta) -> NeedsHud.render(graphics)));
        LauraForge.listen(MinecraftForge.EVENT_BUS, TickEvent.ClientTickEvent.Post.class, event -> LauraClient.tick());
    }
}
