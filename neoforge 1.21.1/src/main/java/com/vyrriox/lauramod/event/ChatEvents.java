package com.vyrriox.lauramod.event;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.init.ModEntities;
import com.vyrriox.lauramod.init.ModSounds;
import com.vyrriox.lauramod.util.InteractionDatabase;
import com.vyrriox.lauramod.util.LauraWorldData;
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

        if (message.contains("je me sens seul") || message.contains("i feel lonely")
                || message.contains("ich fühle mich einsam") || message.contains("me siento solo")
                || message.contains("mi sento solo") || message.contains("eu me sinto sozinho")) {
            // Anti-duplication check (Global World-Wide)
            LauraWorldData data = LauraWorldData.get(player.level());

            if (data.exists()) {
                reply(player, "<§dLaura§r> " + InteractionDatabase.getStaticString(player.getLanguage(), "already_here"));
            } else {
                LauraEntity laura = new LauraEntity(ModEntities.LAURA.get(), player.level());
                laura.setPos(player.getX(), player.getY(), player.getZ());
                laura.tame(player);
                player.level().addFreshEntity(laura);
                reply(player, "Laura est apparue !");
            }
        } else if (message.contains("ou est tu") || message.contains("where are you") || message.contains("wo bist du")
                || message.contains("donde estas") || message.contains("dove sei")
                || message.contains("onde voce esta")) {
            // Find player's Laura
            player.level().getEntitiesOfClass(LauraEntity.class, player.getBoundingBox().inflate(1000)).stream()
                    .filter(l -> l.isOwnedBy(player))
                    .findFirst()
                    .ifPresentOrElse(
                            laura -> reply(player, "Laura: " + (int) laura.getX() + ", " + (int) laura.getY() + ", " + (int) laura.getZ()),
                            () -> {
                                String msg = player.getLanguage().startsWith("fr")
                                        ? "Tu n'as pas de Laura ! Fais 'je me sens seul' pour l'appeler."
                                        : "You don't have a Laura! Say 'I feel lonely' to call her.";
                                reply(player, msg);
                            });
        } else {
            // Check for Laura nearby
            player.level().getEntitiesOfClass(LauraEntity.class, player.getBoundingBox().inflate(10)).stream()
                    .filter(l -> l.isOwnedBy(player))
                    .findFirst()
                    .ifPresent(laura -> {
                        String locale = player.getLanguage();
                        
                        if (message.contains("suis moi") || message.contains("follow me") || message.contains("ven") || message.contains("folge mir") || message.contains("seguimi") || message.contains("siga")) {
                            laura.setOrderedToSit(false);
                            laura.getNavigation().stop();
                            reply(player, "<§dLaura§r> " + InteractionDatabase.getStaticString(locale, "follow"));
                            return;
                        } else if (message.contains("reste ici") || message.contains("stay") || message.contains("quedate") || message.contains("bleib hier") || message.contains("resta qui") || message.contains("fique aqui")) {
                            laura.setOrderedToSit(true);
                            laura.getNavigation().stop();
                            reply(player, "<§dLaura§r> " + InteractionDatabase.getStaticString(locale, "stay"));
                            return;
                        }

                        // Apology logic
                        if (laura.isSad() && (message.contains("désol") || message.contains("desol") || message.contains("pardon")
                                || message.contains("sorry") || message.contains("entschuldigung")
                                || message.contains("lo siento") || message.contains("scusa")
                                || message.contains("desculpe"))) {
                            laura.setSad(false);
                            reply(player, "<§dLaura§r> " + InteractionDatabase.getStaticString(locale, "apology_accept"));
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
                                reply(player, "<§dLaura§r> " + response);
                            }
                        }
                    });
        }
    }

    private static void reply(ServerPlayer player, String msg) {
        new Thread(() -> {
            try { Thread.sleep(50); } catch (Exception e) {}
            if (player.getServer() != null) {
                player.getServer().execute(() -> player.sendSystemMessage(Component.literal(msg)));
            } else {
                player.sendSystemMessage(Component.literal(msg));
            }
        }).start();
    }
}