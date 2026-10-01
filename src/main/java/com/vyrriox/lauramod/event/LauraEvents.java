package com.vyrriox.lauramod.event;

import com.mojang.brigadier.CommandDispatcher;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.command.LauraCommand;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.test.LauraSelfTest;
import com.vyrriox.lauramod.world.LauraChat;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-independent event handlers. Each loader forwards its own events here.
 *
 * @author vyrriox
 */
public final class LauraEvents {
    private LauraEvents() {
    }

    public static void onServerStarting(MinecraftServer server) {
        LauraMod.reloadAll();
        LauraManager.clear();
        LauraNetwork.clear();
    }

    public static void onServerStarted(MinecraftServer server) {
        LauraSelfTest.onServerStarted(server);
    }

    public static void onServerStopped(MinecraftServer server) {
        LauraManager.clear();
        LauraNetwork.clear();
        LauraSelfTest.onServerStopped();
    }

    public static void onServerTick(MinecraftServer server) {
        if (com.vyrriox.lauramod.api.ScriptData.consumeReloadRequest()) {
            // Scripts changed Laura's content: rebuild the tables with it.
            LauraMod.reloadAll();
        }
        LauraManager.tick(server);
        LauraNetwork.tick(server);
        LauraSelfTest.tick(server);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LauraCommand.register(dispatcher);
    }

    public static void onChat(ServerPlayer player, String message) {
        LauraChat.onChat(player, message);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        LauraManager.onPlayerJoin(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        LauraManager.onPlayerLeave(player);
    }

    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel from) {
        LauraManager.onPlayerChangedDimension(player, from);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        LauraManager.onPlayerDeath(player);
    }

    /**
     * An entity is being added to a server level. Only the loaders that can move an entity to
     * another dimension without asking it (the teleporters of Forge and NeoForge on Minecraft
     * 1.20.1) forward the event they fire then (EntityJoinLevelEvent).
     */
    public static void onEntityJoined(Entity entity) {
        if (entity instanceof LauraEntity laura) {
            laura.reportArrival();
        }
    }

    /**
     * An entity stops being part of a server level: its chunk unloads, or it is removed. Loaders
     * forward the event they fire when entity tracking ends (EntityLeaveLevelEvent, ENTITY_UNLOAD).
     */
    public static void onEntityUnloaded(Entity entity) {
        if (entity instanceof LauraEntity laura) {
            LauraManager.onUnloaded(laura);
        }
    }

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        LauraEntity laura = LauraManager.findNear(player, 24);
        if (laura != null) {
            laura.brain().watcher().onBlockBroken(player, state);
        }
    }

    public static void onEntityKilled(LivingEntity victim, ServerPlayer killer) {
        if (victim instanceof LauraEntity) {
            return;
        }
        LauraEntity laura = LauraManager.findNear(killer, 32);
        if (laura != null) {
            laura.brain().watcher().onKill(killer, victim);
        }
    }
}
