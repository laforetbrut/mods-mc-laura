package com.vyrriox.lauramod;

import com.mojang.logging.LogUtils;
import com.vyrriox.lauramod.init.ModEntities;
import com.vyrriox.lauramod.init.ModItems;
import com.vyrriox.lauramod.init.ModMenus;
import com.vyrriox.lauramod.init.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LauraMod.MODID)
public class LauraMod {
    public static final String MODID = "lauramod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LauraMod(IEventBus modEventBus) {
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
    }
}
