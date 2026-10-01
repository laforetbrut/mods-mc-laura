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

    /**
     * The language code the player's client reported ("en_us" when unknown). Vanilla Minecraft
     * 1.20.1 drops it when the client sends its options, so each loader has its own way to keep it.
     */
    String playerLanguage(ServerPlayer player);

    /** Sends a raw mod packet from the client to the server. Only called on the client. */
    void sendToServer(byte[] data);

    /** Sends a raw mod packet to one player. */
    void sendToPlayer(ServerPlayer player, byte[] data);

    /**
     * The inventory of a block through the loader's item API (capabilities, Transfer API), which
     * covers modded storage. Null when the loader finds nothing (vanilla containers are then used).
     */
    InventoryAccess inventoryAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);

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
}
