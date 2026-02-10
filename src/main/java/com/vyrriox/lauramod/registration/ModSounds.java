package com.vyrriox.lauramod.registration;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,
            LauraMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LAURA_AMBIENT = SOUNDS.register("laura_ambient",
            () -> SoundEvent
                    .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LauraMod.MODID, "laura_ambient")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
