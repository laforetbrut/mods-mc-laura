package com.vyrriox.lauramod.platform;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * Everything the common code needs from the mod loader. Each loader project provides one
 * implementation and hands it to {@link com.vyrriox.lauramod.LauraMod#init}.
 *
 * @author vyrriox
 */
public interface Platform {
    /** "neoforge", "forge" or "fabric". */
    String loaderName();

    String modVersion();

    /** The game's global config directory (not the mod sub-folder). */
    Path configDir();

    /** The game directory (instance root). */
    Path gameDir();

    boolean isPhysicalClient();

    boolean isDevelopment();

    boolean isModLoaded(String modId);

    /** Sends a raw mod packet from the client to the server. Only called on the client. */
    void sendToServer(byte[] data);

    /** Sends a raw mod packet to one player. */
    void sendToPlayer(ServerPlayer player, byte[] data);

    /**
     * Client side: false when the server this client plays on did not announce the mod channel, so
     * the mod is missing there. Loaders that refuse such a connection by themselves (the channel is
     * required on NeoForge and Forge) keep the default.
     */
    default boolean serverHasChannel() {
        return true;
    }

    /**
     * The inventory of a block through the loader's item API (capabilities, Transfer API), which
     * covers modded storage. Null when the loader finds nothing (vanilla containers are then used).
     */
    InventoryAccess inventoryAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);

    /**
     * Asks the other mods whether the player may open the container at pos, the way the loader does
     * when a player right clicks a block (claim and protection mods answer there). True when nobody
     * objects. Laura calls it before she uses a container her partner did not assign to her.
     */
    default boolean mayUseContainer(ServerPlayer player, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return true;
    }

    /**
     * The inventory inside an item (a backpack), through the loader's item API. Null when the item
     * has none or the loader cannot tell.
     */
    default InventoryAccess itemInventory(net.minecraft.world.item.ItemStack stack) {
        return null;
    }

    /**
     * Prepares the fake connection of a self test player so that other mods can send it their
     * packets (the loader normally negotiates channels with a real client).
     */
    default void configureMockConnection(net.minecraft.network.Connection connection) {
    }

    /** Cooking pots of food mods (Farmer's Delight), or {@link CookingPots#NONE}. */
    default CookingPots cookingPots() {
        return CookingPots.NONE;
    }

    /**
     * Puts one of the stack in a trinket slot of the entity (Curios, Accessories...). Returns true
     * when it is worn (or, with {@code simulate}, when it could be). False when no trinket mod is
     * installed or no slot accepts it.
     */
    default boolean equipTrinket(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack stack, boolean simulate) {
        return false;
    }

    /** Copies of the trinkets the entity wears. Empty when no trinket mod is installed. */
    default java.util.List<net.minecraft.world.item.ItemStack> trinkets(net.minecraft.world.entity.LivingEntity entity) {
        return java.util.List.of();
    }

    /** Drops every trinket the entity wears at its feet and empties its trinket slots. */
    default void dropTrinkets(net.minecraft.world.entity.LivingEntity entity) {
    }
}
