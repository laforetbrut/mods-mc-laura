package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.registration.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.List;

@EventBusSubscriber(modid = LauraMod.MODID)
public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getRawText();
        ServerPlayer player = event.getPlayer();
        ServerLevel level = player.serverLevel();

        if (message.equalsIgnoreCase("Je me sens seul")) {
            // Check if Laura is already near the player
            List<LauraEntity> lauras = level.getEntitiesOfClass(LauraEntity.class,
                    player.getBoundingBox().inflate(100));

            if (lauras.isEmpty()) {
                LauraEntity laura = ModEntities.LAURA.get().create(level);
                if (laura != null) {
                    laura.moveTo(player.getX() + 2, player.getY(), player.getZ() + 2, player.getYRot(),
                            player.getXRot());
                    level.addFreshEntity(laura);
                    player.sendSystemMessage(Component.translatable("chat.lauramod.appearance"));
                }
            } else {
                player.sendSystemMessage(Component.translatable("chat.lauramod.already_here"));
            }
        }
    }
}
