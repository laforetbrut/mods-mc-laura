package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            LauraMod.MODID);

    public static final RegistryObject<MenuType<LauraInventoryMenu>> LAURA_INVENTORY = MENUS.register("laura_inventory",
            () -> IForgeMenuType.create(LauraInventoryMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
