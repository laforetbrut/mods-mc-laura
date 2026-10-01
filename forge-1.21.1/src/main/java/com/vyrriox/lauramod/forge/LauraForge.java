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
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

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

    public static final RegistryObject<EntityType<LauraEntity>> LAURA = ENTITY_TYPES.register(LauraRegistries.LAURA_ID,
            () -> LauraRegistries.lauraType().build(LauraMod.id(LauraRegistries.LAURA_ID).toString()));
    public static final RegistryObject<LauraGraveBlock> GRAVE = BLOCKS.register(LauraRegistries.GRAVE_ID,
            () -> new LauraGraveBlock(LauraRegistries.graveProperties()));
    public static final RegistryObject<BlockItem> GRAVE_ITEM = ITEMS.register(LauraRegistries.GRAVE_ID,
            () -> new BlockItem(GRAVE.get(), new Item.Properties()));
    public static final RegistryObject<LauraHeartItem> HEART = ITEMS.register(LauraRegistries.HEART_ID,
            () -> new LauraHeartItem(LauraRegistries.heartProperties()));
    public static final RegistryObject<ForgeSpawnEggItem> SPAWN_EGG = ITEMS.register(LauraRegistries.SPAWN_EGG_ID,
            () -> new ForgeSpawnEggItem(LAURA, LauraRegistries.EGG_PRIMARY, LauraRegistries.EGG_SECONDARY, new Item.Properties()));
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

        IEventBus modBus = context.getModEventBus();
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);

        listen(modBus, EntityAttributeCreationEvent.class, event -> event.put(LAURA.get(), LauraEntity.createAttributes().build()));
        listen(modBus, BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG);
            }
        });

        IEventBus game = MinecraftForge.EVENT_BUS;
        listen(game, ServerStartingEvent.class, event -> LauraEvents.onServerStarting(event.getServer()));
        listen(game, ServerStartedEvent.class, event -> LauraEvents.onServerStarted(event.getServer()));
        listen(game, ServerStoppedEvent.class, event -> LauraEvents.onServerStopped(event.getServer()));
        listen(game, TickEvent.ServerTickEvent.Post.class, event -> LauraEvents.onServerTick(event.getServer()));
        listen(game, RegisterCommandsEvent.class, event -> LauraEvents.registerCommands(event.getDispatcher()));
        listen(game, ServerChatEvent.class, event -> LauraEvents.onChat(event.getPlayer(), event.getRawText()));
        listen(game, PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerJoin(player);
            }
        });
        listen(game, PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerLeave(player);
            }
        });
        listen(game, PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.server.getLevel(event.getFrom());
                LauraEvents.onPlayerChangedDimension(player, from);
            }
        });
        listen(game, LivingDeathEvent.class, event -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerDeath(player);
            } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                LauraEvents.onEntityKilled(event.getEntity(), killer);
            }
        });
        listen(game, BlockEvent.BreakEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                LauraEvents.onBlockBroken(player, event.getState());
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            LauraForgeClient.init(modBus);
        }
    }

    /** Registers a listener with its event class given explicitly, so no generic type lookup is needed. */
    static <T extends Event> void listen(IEventBus bus, Class<T> type, Consumer<T> listener) {
        bus.addListener(EventPriority.NORMAL, false, type, listener);
    }
}
