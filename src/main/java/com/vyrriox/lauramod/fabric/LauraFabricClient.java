package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.LauraClient;
import com.vyrriox.lauramod.client.gui.LauraInventoryScreen;
import com.vyrriox.lauramod.client.gui.NeedsHud;
import com.vyrriox.lauramod.client.render.LauraRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.SpawnEggItem;

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

        EntityRendererRegistry.register(LauraFabric.laura, LauraRenderer::new);
        MenuScreens.register(LauraFabric.inventoryMenu, LauraInventoryScreen::new);
        for (KeyMapping key : LauraClient.keyMappings()) {
            KeyBindingHelper.registerKeyBinding(key);
        }
        SpawnEggItem egg = LauraFabric.spawnEgg;
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> FastColor.ARGB32.opaque(egg.getColor(tintIndex)), egg);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            private final ResourceLocation id = LauraMod.id("client_resources");

            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                LauraClient.onResourceReload();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> LauraClient.tick());
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> NeedsHud.render(graphics));

        LauraClient.init();
        LauraClient.afterRegistries();
    }
}
