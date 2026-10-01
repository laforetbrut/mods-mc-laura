package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.LauraInventoryScreen;
import com.vyrriox.lauramod.client.gui.NeedsHud;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Fabric client entry point. Only loaded on the physical client.
 *
 * @author vyrriox
 */
public final class LauraFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricPlatform.setClientSender(data -> ClientPlayNetworking.send(new LauraPayload(data)));
        FabricPlatform.setServerChannelCheck(() -> ClientPlayNetworking.canSend(LauraPayload.TYPE));
        // Fabric runs play payload handlers on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(LauraPayload.TYPE, (payload, context) -> LauraMod.client().handlePacket(payload.data()));

        // Fabric API widens these vanilla registries for mods.
        EntityRenderers.register(LauraFabric.laura, LauraRenderer::new);
        MenuScreens.register(LauraFabric.inventoryMenu, LauraInventoryScreen::new);
        for (KeyMapping key : LauraClient.keyMappings()) {
            KeyMappingHelper.registerKeyMapping(key);
        }

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(LauraMod.id("client_reload"),
                (ResourceManagerReloadListener) manager -> LauraClient.onResourceReload());
        ClientTickEvents.END_CLIENT_TICK.register(client -> LauraClient.tick());
        HudElementRegistry.addLast(LauraMod.id("needs_hud"), (graphics, deltaTracker) -> NeedsHud.render(graphics));

        LauraClient.init();
        LauraClient.afterRegistries();
    }
}
