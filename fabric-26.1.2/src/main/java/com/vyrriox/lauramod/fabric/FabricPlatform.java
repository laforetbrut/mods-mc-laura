package com.vyrriox.lauramod.fabric;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.platform.CookingPots;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Fabric implementation of the loader services.
 *
 * @author vyrriox
 */
final class FabricPlatform implements Platform {
    /** Installed by the client entry point: server-bound packets only exist on the physical client. */
    private static volatile Consumer<byte[]> clientSender;

    private final String version;
    private CookingPots cookingPots;

    FabricPlatform() {
        this.version = FabricLoader.getInstance().getModContainer(LauraMod.MODID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    static void setClientSender(Consumer<byte[]> sender) {
        clientSender = sender;
    }

    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public String modVersion() {
        return version;
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void sendToServer(byte[] data) {
        Consumer<byte[]> sender = clientSender;
        if (sender == null) {
            LauraMod.LOGGER.warn("Tried to send a packet to the server outside of a client");
            return;
        }
        sender.accept(data);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, byte[] data) {
        // Players without the mod (or test players) have no channel: skip them quietly.
        if (player.connection != null && ServerPlayNetworking.canSend(player, LauraPayload.TYPE)) {
            ServerPlayNetworking.send(player, new LauraPayload(data));
        }
    }

    @Override
    public CookingPots cookingPots() {
        if (cookingPots == null) {
            cookingPots = hasFarmersDelightRefabricated()
                    ? new com.vyrriox.lauramod.fabric.compat.FarmersDelightPots()
                    : CookingPots.NONE;
        }
        return cookingPots;
    }

    /** Farmer's Delight on Fabric is the Refabricated port; another port would have a different API. */
    private static boolean hasFarmersDelightRefabricated() {
        if (!FabricLoader.getInstance().isModLoaded("farmersdelight")) {
            return false;
        }
        try {
            Class.forName("vectorwing.farmersdelight.refabricated.inventory.ItemStackHandler", false, FabricPlatform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            LauraMod.LOGGER.warn("Farmer's Delight is installed but is not the Refabricated port: cooking pots are not supported");
            return false;
        }
    }

    @Override
    public InventoryAccess inventoryAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return TransferInventoryAccess.at(level, pos);
    }

    @Override
    public InventoryAccess itemInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return TransferInventoryAccess.inside(stack);
    }
}
