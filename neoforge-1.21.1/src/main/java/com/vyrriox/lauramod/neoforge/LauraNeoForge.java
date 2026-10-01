package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.block.LauraGraveBlock;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.event.LauraEvents;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import com.vyrriox.lauramod.item.LauraHeartItem;
import com.vyrriox.lauramod.network.LauraNetwork;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets.
 *
 * @author vyrriox
 */
@Mod(LauraMod.MODID)
public final class LauraNeoForge {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, LauraMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LauraMod.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LauraMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LauraMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, LauraMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LauraMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LauraEntity>> LAURA = ENTITY_TYPES.register(LauraRegistries.LAURA_ID,
            () -> LauraRegistries.lauraType().build(LauraMod.id(LauraRegistries.LAURA_ID).toString()));
    public static final DeferredBlock<LauraGraveBlock> GRAVE = BLOCKS.register(LauraRegistries.GRAVE_ID,
            () -> new LauraGraveBlock(LauraRegistries.graveProperties()));
    public static final DeferredItem<BlockItem> GRAVE_ITEM = ITEMS.registerSimpleBlockItem(LauraRegistries.GRAVE_ID, GRAVE);
    public static final DeferredItem<LauraHeartItem> HEART = ITEMS.register(LauraRegistries.HEART_ID,
            () -> new LauraHeartItem(LauraRegistries.heartProperties()));
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register(LauraRegistries.SPAWN_EGG_ID,
            () -> new DeferredSpawnEggItem(LAURA, LauraRegistries.EGG_PRIMARY, LauraRegistries.EGG_SECONDARY, new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<LauraInventoryMenu>> INVENTORY_MENU = MENUS.register(LauraRegistries.INVENTORY_MENU_ID,
            LauraRegistries::inventoryMenuType);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(LauraRegistries.TAB_ID, () -> CreativeModeTab.builder()
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

    public LauraNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        LauraMod.init(new NeoForgePlatform(container));
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);

        modBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(LAURA.get(), LauraEntity.createAttributes().build()));
        modBus.addListener(RegisterPayloadHandlersEvent.class, LauraNeoForge::registerPayloads);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG.get());
            }
        });

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerStartingEvent.class, event -> LauraEvents.onServerStarting(event.getServer()));
        game.addListener(ServerStartedEvent.class, event -> LauraEvents.onServerStarted(event.getServer()));
        game.addListener(ServerStoppedEvent.class, event -> LauraEvents.onServerStopped(event.getServer()));
        game.addListener(ServerTickEvent.Post.class, event -> LauraEvents.onServerTick(event.getServer()));
        game.addListener(RegisterCommandsEvent.class, event -> LauraEvents.registerCommands(event.getDispatcher()));
        game.addListener(ServerChatEvent.class, event -> LauraEvents.onChat(event.getPlayer(), event.getRawText()));
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerJoin(player);
            }
        });
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerLeave(player);
            }
        });
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.server.getLevel(event.getFrom());
                LauraEvents.onPlayerChangedDimension(player, from);
            }
        });
        game.addListener(LivingDeathEvent.class, event -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerDeath(player);
            } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                LauraEvents.onEntityKilled(event.getEntity(), killer);
            }
        });
        game.addListener(BlockEvent.BreakEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                LauraEvents.onBlockBroken(player, event.getState());
            }
        });

        if (dist.isClient()) {
            LauraNeoForgeClient.init(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(String.valueOf(LauraNetwork.PROTOCOL));
        registrar.playBidirectional(LauraPayload.TYPE, LauraPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> LauraNetwork.handleServer(player, payload.data()));
            } else {
                context.enqueueWork(() -> LauraMod.client().handlePacket(payload.data()));
            }
        });
    }
}
