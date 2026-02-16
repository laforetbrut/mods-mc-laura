package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LauraMod.MODID);

    public static final RegistryObject<Item> LAURA_SPAWN_EGG = ITEMS.register("laura_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LAURA, 0xFFC0CB, 0x000000, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
