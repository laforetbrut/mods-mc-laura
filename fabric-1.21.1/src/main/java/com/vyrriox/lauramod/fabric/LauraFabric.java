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
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Fabric entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets.
 *
 * @author vyrriox
 */
public final class LauraFabric implements ModInitializer {
    static EntityType<LauraEntity> laura;
    static MenuType<LauraInventoryMenu> inventoryMenu;
    static SpawnEggItem spawnEgg;

    @Override
    public void onInitialize() {
        LauraMod.init(new FabricPlatform());
        registerContent();
        registerNetwork();
        registerEvents();
    }

    private static void registerContent() {
        EntityType<LauraEntity> lauraType = Registry.register(BuiltInRegistries.ENTITY_TYPE, LauraMod.id(LauraRegistries.LAURA_ID),
                LauraRegistries.lauraType().build(LauraMod.id(LauraRegistries.LAURA_ID).toString()));
        laura = lauraType;
        FabricDefaultAttributeRegistry.register(lauraType, LauraEntity.createAttributes());

        LauraGraveBlock grave = Registry.register(BuiltInRegistries.BLOCK, LauraMod.id(LauraRegistries.GRAVE_ID), new LauraGraveBlock(LauraRegistries.graveProperties()));
        BlockItem graveItem = Registry.register(BuiltInRegistries.ITEM, LauraMod.id(LauraRegistries.GRAVE_ID), new BlockItem(grave, new Item.Properties()));
        LauraHeartItem heart = Registry.register(BuiltInRegistries.ITEM, LauraMod.id(LauraRegistries.HEART_ID), new LauraHeartItem(LauraRegistries.heartProperties()));
        SpawnEggItem egg = Registry.register(BuiltInRegistries.ITEM, LauraMod.id(LauraRegistries.SPAWN_EGG_ID),
                new SpawnEggItem(lauraType, LauraRegistries.EGG_PRIMARY, LauraRegistries.EGG_SECONDARY, new Item.Properties()));
        spawnEgg = egg;
        DispenserBlock.registerBehavior(egg, new SpawnEggDispenseBehavior());

        MenuType<LauraInventoryMenu> menu = Registry.register(BuiltInRegistries.MENU, LauraMod.id(LauraRegistries.INVENTORY_MENU_ID), LauraRegistries.inventoryMenuType());
        inventoryMenu = menu;

        for (LauraRegistries.Sound sound : LauraRegistries.Sound.values()) {
            ResourceLocation id = LauraRegistries.soundId(sound);
            SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
            LauraRegistries.SOUNDS.put(sound, () -> event);
        }

        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.lauramod"))
                .icon(() -> new ItemStack(heart))
                .displayItems((parameters, output) -> {
                    output.accept(heart);
                    output.accept(egg);
                    output.accept(graveItem);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LauraMod.id(LauraRegistries.TAB_ID), tab);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(egg));

        LauraRegistries.LAURA = () -> lauraType;
        LauraRegistries.GRAVE = () -> grave;
        LauraRegistries.GRAVE_ITEM = () -> graveItem;
        LauraRegistries.HEART = () -> heart;
        LauraRegistries.SPAWN_EGG = () -> egg;
        LauraRegistries.INVENTORY_MENU = () -> menu;
    }

    private static void registerNetwork() {
        PayloadTypeRegistry.playC2S().register(LauraPayload.TYPE, LauraPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(LauraPayload.TYPE, LauraPayload.STREAM_CODEC);
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
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> LauraEvents.onPlayerChangedDimension(player, origin));
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

    /**
     * Dispensers hatch the egg like a vanilla one. Vanilla only wires the eggs that exist when the
     * game bootstraps, before mods register theirs.
     */
    private static final class SpawnEggDispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            EntityType<?> type = ((SpawnEggItem) stack.getItem()).getType(stack);
            try {
                type.spawn(source.level(), stack, null, source.pos().relative(direction), MobSpawnType.DISPENSER, direction != Direction.UP, false);
            } catch (Exception e) {
                LauraMod.LOGGER.error("Error while dispensing spawn egg from dispenser at {}", source.pos(), e);
                return ItemStack.EMPTY;
            }
            stack.shrink(1);
            source.level().gameEvent(null, GameEvent.ENTITY_PLACE, source.pos());
            return stack;
        }
    }
}
