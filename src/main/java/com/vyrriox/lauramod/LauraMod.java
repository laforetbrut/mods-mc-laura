package com.vyrriox.lauramod;

import com.vyrriox.lauramod.client.renderer.LauraRenderer;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.registration.ModEntities;
import com.vyrriox.lauramod.registration.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LauraMod.MODID)
public class LauraMod {
    public static final String MODID = "lauramod";
    private static final Logger LOGGER = LogManager.getLogger();

    public LauraMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Laura Mod by vyrriox");

        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::registerRenderers);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LAURA.get(), LauraRenderer::new);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.LAURA.get(), LauraEntity.createAttributes().build());
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Laura Mod Common Setup");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Laura Mod Client Setup");
    }
}
