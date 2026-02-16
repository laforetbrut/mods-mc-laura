package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU_TYPE,
            LauraMod.MODID);

    public static final Supplier<MenuType<LauraInventoryMenu>> LAURA_INVENTORY = MENUS.register("laura_inventory",
            () -> IMenuTypeExtension.create(LauraInventoryMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
