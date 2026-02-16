package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(BuiltInRegistries.SOUND_EVENT, LauraMod.MODID);

    public static final Supplier<SoundEvent> LAURA_AMBIENT = registerSoundEvent("laura_ambient");
    public static final Supplier<SoundEvent> LAURA_FART = registerSoundEvent("laura_fart");
    public static final Supplier<SoundEvent> LAURA_HAPPY = registerSoundEvent("laura_happy");
    public static final Supplier<SoundEvent> LAURA_SAD = registerSoundEvent("laura_sad");
    public static final Supplier<SoundEvent> LAURA_ANGRY = registerSoundEvent("laura_angry");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LauraMod.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
