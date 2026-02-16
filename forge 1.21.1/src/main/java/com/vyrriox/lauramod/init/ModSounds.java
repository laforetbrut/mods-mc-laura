package com.vyrriox.lauramod.init;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(ForgeRegistries.SOUND_EVENTS, LauraMod.MODID);

    public static final RegistryObject<SoundEvent> LAURA_AMBIENT = registerSoundEvent("laura_ambient");
    public static final RegistryObject<SoundEvent> LAURA_FART = registerSoundEvent("laura_fart");
    public static final RegistryObject<SoundEvent> LAURA_HAPPY = registerSoundEvent("laura_happy");
    public static final RegistryObject<SoundEvent> LAURA_SAD = registerSoundEvent("laura_sad");
    public static final RegistryObject<SoundEvent> LAURA_ANGRY = registerSoundEvent("laura_angry");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LauraMod.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
