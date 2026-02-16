package com.vyrriox.lauramod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class LauraCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("laura")
                .then(Commands.literal("skin")
                        .then(Commands.argument("url", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String url = StringArgumentType.getString(context, "url");
                                    Entity entity = context.getSource().getEntity();
                                    if (entity != null) {
                                        // Find nearest Laura
                                        List<LauraEntity> lauras = entity.level().getEntitiesOfClass(LauraEntity.class,
                                                new AABB(entity.blockPosition()).inflate(10));

                                        if (!lauras.isEmpty()) {
                                            LauraEntity laura = lauras.get(0);
                                            laura.setSkinUrl(url);
                                            context.getSource().sendSuccess(() -> Component.literal("Skin updated!"),
                                                    true);
                                            return 1;
                                        } else {
                                            context.getSource()
                                                    .sendFailure(Component.literal("No Laura found nearby."));
                                            return 0;
                                        }
                                    }
                                    return 0;
                                }))));
    }
}
