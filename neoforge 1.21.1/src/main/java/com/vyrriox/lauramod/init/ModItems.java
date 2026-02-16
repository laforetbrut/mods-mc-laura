package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LauraMod.MODID);

    public static final DeferredHolder<Item, Item> LAURA_SPAWN_EGG = ITEMS.register("laura_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LAURA, 0xFFC0CB, 0x000000, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
