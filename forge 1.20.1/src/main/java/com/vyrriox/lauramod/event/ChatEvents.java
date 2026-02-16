package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.init.ModEntities;
import com.vyrriox.lauramod.init.ModSounds;
import com.vyrriox.lauramod.util.InteractionDatabase;
import com.vyrriox.lauramod.util.LauraWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LauraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatEvents {

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString().toLowerCase();
        ServerPlayer player = event.getPlayer();

        if (message.contains("je me sens seul") || message.contains("i feel lonely")
                || message.contains("ich fühle mich einsam") || message.contains("me siento solo")
                || message.contains("mi sento solo") || message.contains("eu me sinto sozinho")) {
            // Anti-duplication check (Global World-Wide)
            LauraWorldData data = LauraWorldData.get(player.level());

            if (data.exists()) {
                player.sendSystemMessage(Component.literal(
                        "<Laura> " + InteractionDatabase.getStaticString(player.getLanguage(), "already_here")));
            } else {
                LauraEntity laura = new LauraEntity(ModEntities.LAURA.get(), player.level());
                laura.setPos(player.getX(), player.getY(), player.getZ());
                laura.tame(player);
                player.level().addFreshEntity(laura);
                player.sendSystemMessage(Component.literal("Laura est apparue !"));
            }
        } else if (message.contains("ou est tu") || message.contains("where are you") || message.contains("wo bist du")
                || message.contains("donde estas") || message.contains("dove sei")
                || message.contains("onde voce esta")) {
            // Find player's Laura
            player.level().getEntitiesOfClass(LauraEntity.class, player.getBoundingBox().inflate(1000)).stream()
                    .filter(l -> l.isOwnedBy(player))
                    .findFirst()
                    .ifPresentOrElse(
                            laura -> player.sendSystemMessage(Component.literal("Laura: " + (int) laura.getX() + ", "
                                    + (int) laura.getY() + ", " + (int) laura.getZ())),
                            () -> {
                                String msg = player.getLanguage().startsWith("fr")
                                        ? "Tu n'as pas de Laura ! Fais 'je me sens seul' pour l'appeler."
                                        : "You don't have a Laura! Say 'I feel lonely' to call her.";
                                player.sendSystemMessage(Component.literal(msg));
                            });
        } else {
            // Check for Laura nearby
            player.level().getEntitiesOfClass(LauraEntity.class, player.getBoundingBox().inflate(10)).stream()
                    .filter(l -> l.isOwnedBy(player))
                    .findFirst()
                    .ifPresent(laura -> {
                        String locale = player.getLanguage();
                        // Apology logic
                        if (laura.isSad() && (message.contains("désolé") || message.contains("pardon")
                                || message.contains("sorry") || message.contains("entschuldigung")
                                || message.contains("lo siento") || message.contains("scusa")
                                || message.contains("desculpe"))) {
                            laura.setSad(false);
                            player.sendSystemMessage(Component.literal(
                                    "<Laura> " + InteractionDatabase.getStaticString(locale, "apology_accept")));
                            laura.playSound(ModSounds.LAURA_HAPPY.get(), 1.0F, 1.0F);
                        }
                        // Love Response logic
                        else if (laura.isWaitingForLoveResponse()) {
                            if (message.contains("oui") || message.contains("yes") || message.contains("ja")
                                    || message.contains("sí") || message.contains("si") || message.contains("sim")) {
                                laura.handleLoveResponse(true);
                            } else if (message.contains("non") || message.contains("no") || message.contains("nein")
                                    || message.contains("não")) {
                                laura.handleLoveResponse(false);
                            }
                        }
                        // General Interactions (Universal Intent Matching)
                        else {
                            String response = InteractionDatabase.getResponse(locale, message);
                            if (response != null) {
                                player.sendSystemMessage(Component.literal("<Laura> " + response));
                            }
                        }
                    });
        }
    }
}
