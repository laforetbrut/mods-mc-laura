package com.vyrriox.lauramod.registry;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registered objects, filled by the loader glue. Common code only reads the suppliers.
 *
 * @author vyrriox
 */
public final class LauraRegistries {
    public static final String LAURA_ID = "laura";
    public static final String SPAWN_EGG_ID = "laura_spawn_egg";
    public static final String HEART_ID = "laura_heart";
    public static final String GRAVE_ID = "laura_grave";
    public static final String INVENTORY_MENU_ID = "laura_inventory";
    public static final String TAB_ID = "laura";

    public static Supplier<EntityType<LauraEntity>> LAURA;
    public static Supplier<Item> SPAWN_EGG;
    public static Supplier<Item> HEART;
    public static Supplier<Block> GRAVE;
    public static Supplier<Item> GRAVE_ITEM;
    public static Supplier<MenuType<LauraInventoryMenu>> INVENTORY_MENU;
    public static final Map<Sound, Supplier<SoundEvent>> SOUNDS = new EnumMap<>(Sound.class);

    /** Blocks Laura may break when fetching (logs, flowers, crops...). */
    public static final TagKey<Block> FETCH_HARVESTABLE = TagKey.create(Registries.BLOCK, LauraMod.id("fetch_harvestable"));

    private LauraRegistries() {
    }

    /** Sound events, all backed by the entries of {@code assets/lauramod/sounds.json}. */
    public enum Sound {
        AMBIENT, FART, HAPPY, SAD, ANGRY, MUFFLED, LAUGH, KISS, YAWN, CLAP, SNAP, TAP, WHOOSH, HICCUP, CHATTER, HMPH, SIGH, SPARKLE;

        public String id() {
            return "laura_" + name().toLowerCase(Locale.ROOT);
        }

        public SoundEvent get() {
            Supplier<SoundEvent> supplier = SOUNDS.get(this);
            return supplier == null ? null : supplier.get();
        }
    }

    /** Entity type builder shared by every loader. */
    public static EntityType.Builder<LauraEntity> lauraType() {
        return EntityType.Builder.<LauraEntity>of(LauraEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .eyeHeight(1.62F)
                .clientTrackingRange(10);
    }

    /** Menu type shared by every loader. The client side instance is built without extra data. */
    public static MenuType<LauraInventoryMenu> inventoryMenuType() {
        return new MenuType<>(LauraInventoryMenu::clientSide, FeatureFlags.DEFAULT_FLAGS);
    }

    /** Laura's Heart: a rare item, 16 per stack. */
    public static Item.Properties heartProperties() {
        return new Item.Properties().stacksTo(16).rarity(Rarity.RARE);
    }

    /** The grave: stone-like, mined with a pickaxe, not a full cube. */
    public static BlockBehaviour.Properties graveProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    public static ResourceLocation soundId(Sound sound) {
        return LauraMod.id(sound.id());
    }

    /** Spawn egg colors: pink and dark brown. */
    public static final int EGG_PRIMARY = 0xFFB6C8;
    public static final int EGG_SECONDARY = 0x5A2E1E;
}
