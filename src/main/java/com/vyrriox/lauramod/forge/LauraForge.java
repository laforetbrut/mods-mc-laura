package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.block.LauraGraveBlock;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.event.LauraEvents;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import com.vyrriox.lauramod.item.LauraHeartItem;
import com.vyrriox.lauramod.registry.LauraRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets.
 *
 * @author vyrriox
 */
@Mod(LauraMod.MODID)
public final class LauraForge {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, LauraMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LauraMod.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LauraMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LauraMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, LauraMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LauraMod.MODID);

    // Since 1.21.2 items and blocks carry their id in their properties, and entity types are built with their key.
    public static final RegistryObject<EntityType<LauraEntity>> LAURA = ENTITY_TYPES.register(LauraRegistries.LAURA_ID,
            () -> LauraRegistries.lauraType().build(ENTITY_TYPES.key(LauraRegistries.LAURA_ID)));
    public static final RegistryObject<LauraGraveBlock> GRAVE = BLOCKS.register(LauraRegistries.GRAVE_ID,
            () -> new LauraGraveBlock(LauraRegistries.graveProperties().setId(BLOCKS.key(LauraRegistries.GRAVE_ID))));
    public static final RegistryObject<BlockItem> GRAVE_ITEM = ITEMS.register(LauraRegistries.GRAVE_ID,
            () -> new BlockItem(GRAVE.get(), new Item.Properties().setId(ITEMS.key(LauraRegistries.GRAVE_ID)).useBlockDescriptionPrefix()));
    public static final RegistryObject<LauraHeartItem> HEART = ITEMS.register(LauraRegistries.HEART_ID,
            () -> new LauraHeartItem(LauraRegistries.heartProperties().setId(ITEMS.key(LauraRegistries.HEART_ID))));
    // Entity types are registered before items, so the type exists when the egg is built.
    public static final RegistryObject<SpawnEggItem> SPAWN_EGG = ITEMS.register(LauraRegistries.SPAWN_EGG_ID,
            () -> new SpawnEggItem(new Item.Properties().setId(ITEMS.key(LauraRegistries.SPAWN_EGG_ID)).spawnEgg(LAURA.get())));
    public static final RegistryObject<MenuType<LauraInventoryMenu>> INVENTORY_MENU = MENUS.register(LauraRegistries.INVENTORY_MENU_ID,
            LauraRegistries::inventoryMenuType);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register(LauraRegistries.TAB_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lauramod"))
            .icon(() -> new ItemStack(HEART.get()))
            .displayItems((parameters, output) -> {
                output.accept(HEART.get());
                output.accept(SPAWN_EGG.get());
                output.accept(GRAVE_ITEM.get());
            })
            .build());

    static {
        LauraRegistries.LAURA = LAURA;
        LauraRegistries.GRAVE = GRAVE::get;
        LauraRegistries.GRAVE_ITEM = GRAVE_ITEM::get;
        LauraRegistries.HEART = HEART::get;
        LauraRegistries.SPAWN_EGG = SPAWN_EGG::get;
        LauraRegistries.INVENTORY_MENU = INVENTORY_MENU;
        for (LauraRegistries.Sound sound : LauraRegistries.Sound.values()) {
            LauraRegistries.SOUNDS.put(sound, SOUNDS.register(sound.id(), () -> SoundEvent.createVariableRangeEvent(LauraRegistries.soundId(sound))));
        }
    }

    public LauraForge(FMLJavaModLoadingContext context) {
        LauraMod.init(new ForgePlatform(context.getContainer()));
        LauraChannel.init();

        BusGroup modBus = context.getModBusGroup();
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);

        // Forge 64 gives every event its own bus; the mod bus group only carries the lifecycle events.
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(LAURA.get(), LauraEntity.createAttributes().build()));
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG);
            }
        });

        ServerStartingEvent.BUS.addListener(event -> LauraEvents.onServerStarting(event.getServer()));
        ServerStartedEvent.BUS.addListener(event -> LauraEvents.onServerStarted(event.getServer()));
        ServerStoppedEvent.BUS.addListener(event -> LauraEvents.onServerStopped(event.getServer()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> LauraEvents.onServerTick(event.server()));
        RegisterCommandsEvent.BUS.addListener(event -> LauraEvents.registerCommands(event.getDispatcher()));
        ServerChatEvent.BUS.addListener(event -> {
            LauraEvents.onChat(event.getPlayer(), event.getRawText());
        });
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerJoin(player);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerLeave(player);
            }
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.level().getServer().getLevel(event.getFrom());
                LauraEvents.onPlayerChangedDimension(player, from);
            }
        });
        EntityLeaveLevelEvent.BUS.addListener(event -> {
            if (!event.getLevel().isClientSide()) {
                LauraEvents.onEntityUnloaded(event.getEntity());
            }
        });
        LivingDeathEvent.BUS.addListener(event -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerDeath(player);
            } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                LauraEvents.onEntityKilled(event.getEntity(), killer);
            }
        });
        BlockEvent.BreakEvent.BUS.addListener(event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                LauraEvents.onBlockBroken(player, event.getState());
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            LauraForgeClient.init(modBus);
        }
    }
}
