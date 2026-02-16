package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.command.LauraCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LauraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LauraCommand.register(event.getDispatcher());
    }
}
