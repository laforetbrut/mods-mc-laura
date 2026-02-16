package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.init.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

@EventBusSubscriber(modid = LauraMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ChatEvents {

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString().toLowerCase();
        ServerPlayer player = event.getPlayer();

        if (message.contains("je me sens seul")) {
            LauraEntity laura = new LauraEntity(ModEntities.LAURA.get(), player.level());
            laura.setPos(player.getX(), player.getY(), player.getZ());
            laura.tame(player);
            player.level().addFreshEntity(laura);
            player.sendSystemMessage(Component.literal("Laura est apparue !"));
        } else if (message.contains("ou est tu") || message.contains("where are you")) {
            player.level().getEntitiesOfClass(LauraEntity.class, player.getBoundingBox().inflate(1000)).stream()
                    .filter(l -> l.isOwnedBy(player))
                    .findFirst()
                    .ifPresentOrElse(
                            l -> player.sendSystemMessage(
                                    Component.literal("Je suis ici : " + l.blockPosition().toShortString())),
                            () -> player.sendSystemMessage(Component.literal("Je ne sais pas où je suis...")));
        }
    }
}
