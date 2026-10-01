package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.block.LauraGraveBlock;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.event.LauraEvents;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import com.vyrriox.lauramod.item.LauraHeartItem;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.registry.LauraRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Fabric entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets.
 *
 * @author vyrriox
 */
public final class LauraFabric implements ModInitializer {
    static EntityType<LauraEntity> laura;
    static MenuType<LauraInventoryMenu> inventoryMenu;

    @Override
    public void onInitialize() {
        LauraMod.init(new FabricPlatform());
        registerContent();
        registerNetwork();
        registerEvents();
    }

    private static void registerContent() {
        Identifier lauraId = LauraMod.id(LauraRegistries.LAURA_ID);
        EntityType<LauraEntity> lauraType = Registry.register(BuiltInRegistries.ENTITY_TYPE, lauraId,
                LauraRegistries.lauraType().build(ResourceKey.create(Registries.ENTITY_TYPE, lauraId)));
        laura = lauraType;
        FabricDefaultAttributeRegistry.register(lauraType, LauraEntity.createAttributes());

        // Blocks and items carry their own id since Minecraft 1.21.2.
        Identifier graveId = LauraMod.id(LauraRegistries.GRAVE_ID);
        LauraGraveBlock grave = Registry.register(BuiltInRegistries.BLOCK, graveId,
                new LauraGraveBlock(LauraRegistries.graveProperties().setId(ResourceKey.create(Registries.BLOCK, graveId))));
        BlockItem graveItem = Registry.register(BuiltInRegistries.ITEM, graveId,
                new BlockItem(grave, new Item.Properties().setId(itemKey(LauraRegistries.GRAVE_ID)).useBlockDescriptionPrefix()));
        LauraHeartItem heart = Registry.register(BuiltInRegistries.ITEM, LauraMod.id(LauraRegistries.HEART_ID),
                new LauraHeartItem(LauraRegistries.heartProperties().setId(itemKey(LauraRegistries.HEART_ID))));
        // Vanilla dispensers hatch any spawn egg that names its entity type, modded ones included.
        SpawnEggItem egg = Registry.register(BuiltInRegistries.ITEM, LauraMod.id(LauraRegistries.SPAWN_EGG_ID),
                new SpawnEggItem(new Item.Properties().setId(itemKey(LauraRegistries.SPAWN_EGG_ID)).spawnEgg(lauraType)));

        MenuType<LauraInventoryMenu> menu = Registry.register(BuiltInRegistries.MENU, LauraMod.id(LauraRegistries.INVENTORY_MENU_ID), LauraRegistries.inventoryMenuType());
        inventoryMenu = menu;

        for (LauraRegistries.Sound sound : LauraRegistries.Sound.values()) {
            Identifier id = LauraRegistries.soundId(sound);
            SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
            LauraRegistries.SOUNDS.put(sound, () -> event);
        }

        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.lauramod"))
                .icon(() -> new ItemStack(heart))
                .displayItems((parameters, output) -> {
                    output.accept(heart);
                    output.accept(egg);
                    output.accept(graveItem);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LauraMod.id(LauraRegistries.TAB_ID), tab);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(egg));

        LauraRegistries.LAURA = () -> lauraType;
        LauraRegistries.GRAVE = () -> grave;
        LauraRegistries.GRAVE_ITEM = () -> graveItem;
        LauraRegistries.HEART = () -> heart;
        LauraRegistries.SPAWN_EGG = () -> egg;
        LauraRegistries.INVENTORY_MENU = () -> menu;
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, LauraMod.id(path));
    }

    private static void registerNetwork() {
        PayloadTypeRegistry.serverboundPlay().register(LauraPayload.TYPE, LauraPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LauraPayload.TYPE, LauraPayload.STREAM_CODEC);
        // Fabric runs play payload handlers on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(LauraPayload.TYPE,
                (payload, context) -> LauraNetwork.handleServer(context.player(), payload.data()));
    }

    private static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(LauraEvents::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(LauraEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(LauraEvents::onServerStopped);
        ServerTickEvents.END_SERVER_TICK.register(LauraEvents::onServerTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> LauraEvents.registerCommands(dispatcher));
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> LauraEvents.onChat(sender, message.signedContent()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> LauraEvents.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LauraEvents.onPlayerLeave(handler.getPlayer()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> LauraEvents.onPlayerChangedDimension(player, origin));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> LauraEvents.onEntityUnloaded(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                LauraEvents.onPlayerDeath(player);
            } else if (source.getEntity() instanceof ServerPlayer killer) {
                LauraEvents.onEntityKilled(entity, killer);
            }
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                LauraEvents.onBlockBroken(serverPlayer, state);
            }
        });
    }
}
