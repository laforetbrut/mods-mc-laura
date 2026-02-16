package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            LauraMod.MODID);

    public static final RegistryObject<EntityType<LauraEntity>> LAURA = ENTITIES.register("laura",
            () -> EntityType.Builder.of(LauraEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("laura"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
