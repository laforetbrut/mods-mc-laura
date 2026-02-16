package com.vyrriox.lauramod;

import com.mojang.logging.LogUtils;
import com.vyrriox.lauramod.init.ModEntities;
import com.vyrriox.lauramod.init.ModItems;
import com.vyrriox.lauramod.init.ModMenus;
import com.vyrriox.lauramod.init.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LauraMod.MODID)
public class LauraMod {
    public static final String MODID = "lauramod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LauraMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);

        // Register the commonSetup method for modloading
        // modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }
}
