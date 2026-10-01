package com.vyrriox.lauramod.neoforge;

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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * NeoForge entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets. NeoForge for Minecraft 1.20.1 is the 47.1 fork of Forge and keeps the
 * net.minecraftforge packages.
 *
 * @author vyrriox
 */
@Mod(LauraMod.MODID)
public final class LauraNeoForge {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LauraMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LauraMod.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LauraMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LauraMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LauraMod.MODID);
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

    public LauraNeoForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LauraMod.init(new NeoForgePlatform(ModLoadingContext.get().getActiveContainer()));
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        LauraChannel.register();

        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(LAURA.get(), LauraEntity.createAttributes().build()));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG);
            }
        });

        IEventBus game = MinecraftForge.EVENT_BUS;
        game.addListener((ServerStartingEvent event) -> LauraEvents.onServerStarting(event.getServer()));
        game.addListener((ServerStartedEvent event) -> LauraEvents.onServerStarted(event.getServer()));
        game.addListener((ServerStoppedEvent event) -> LauraEvents.onServerStopped(event.getServer()));
        game.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                LauraEvents.onServerTick(event.getServer());
            }
        });
        game.addListener((RegisterCommandsEvent event) -> LauraEvents.registerCommands(event.getDispatcher()));
        game.addListener((ServerChatEvent event) -> LauraEvents.onChat(event.getPlayer(), event.getRawText()));
        game.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerJoin(player);
            }
        });
        game.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerLeave(player);
            }
        });
        game.addListener((PlayerEvent.Clone event) -> {
            if (event.getEntity() instanceof ServerPlayer player && event.getOriginal() instanceof ServerPlayer original) {
                NeoForgePlatform.carryClientSettings(original, player);
            }
        });
        game.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.server.getLevel(event.getFrom());
                LauraEvents.onPlayerChangedDimension(player, from);
            }
        });
        game.addListener((LivingDeathEvent event) -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                LauraEvents.onPlayerDeath(player);
            } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                LauraEvents.onEntityKilled(event.getEntity(), killer);
            }
        });
        game.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                LauraEvents.onBlockBroken(player, event.getState());
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            LauraNeoForgeClient.init(modBus);
        }
    }
}
