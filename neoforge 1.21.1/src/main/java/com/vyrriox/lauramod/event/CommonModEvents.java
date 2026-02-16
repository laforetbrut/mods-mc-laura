package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.command.LauraCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = LauraMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CommonModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LauraCommand.register(event.getDispatcher());
    }
}
