package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.LauraInventoryScreen;
import com.vyrriox.lauramod.client.gui.NeedsHud;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client entry point. Only loaded on the physical client.
 *
 * @author vyrriox
 */
final class LauraNeoForgeClient {
    private LauraNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            LauraClient.init();
            LauraClient.afterRegistries();
        }));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(LauraNeoForge.LAURA.get(), LauraRenderer::new));
        modBus.addListener(RegisterMenuScreensEvent.class,
                event -> event.register(LauraNeoForge.INVENTORY_MENU.get(), LauraInventoryScreen::new));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            for (KeyMapping key : LauraClient.keyMappings()) {
                event.register(key);
            }
        });
        modBus.addListener(AddClientReloadListenersEvent.class,
                event -> event.addListener(LauraMod.id("client_reload"), (ResourceManagerReloadListener) manager -> LauraClient.onResourceReload()));
        modBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAboveAll(LauraMod.id("needs_hud"), (graphics, delta) -> NeedsHud.render(graphics)));
        modBus.addListener(RegisterClientPayloadHandlersEvent.class,
                event -> event.register(LauraPayload.TYPE, (payload, context) -> LauraMod.client().handlePacket(payload.data())));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> LauraClient.tick());
    }

    static void sendToServer(byte[] data) {
        ClientPacketDistributor.sendToServer(new LauraPayload(data));
    }
}
