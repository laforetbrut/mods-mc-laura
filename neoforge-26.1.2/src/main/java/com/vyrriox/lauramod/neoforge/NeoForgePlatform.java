package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.nio.file.Path;

/**
 * NeoForge implementation of the loader services.
 *
 * @author vyrriox
 */
final class NeoForgePlatform implements Platform {
    private final ModContainer container;

    NeoForgePlatform(ModContainer container) {
        this.container = container;
    }

    @Override
    public void configureMockConnection(net.minecraft.network.Connection connection) {
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String modVersion() {
        return container.getModInfo().getVersion().toString();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.getDist().isClient();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void sendToServer(byte[] data) {
        LauraNeoForgeClient.sendToServer(data);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, byte[] data) {
        // Players without the mod (or test players) have no channel: skip them quietly.
        if (player.connection != null && player.connection.hasChannel(LauraPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new LauraPayload(data));
        }
    }

    @Override
    public InventoryAccess inventoryAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    @Override
    public InventoryAccess itemInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        // The handler writes its changes back into this very stack.
        ResourceHandler<ItemResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Item.ITEM);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    /** Any NeoForge item handler: vanilla chests, Sophisticated Storage, AE2 and RS interfaces, Create vaults... */
    private record ItemHandlerAccess(ResourceHandler<ItemResource> handler) implements InventoryAccess {
        @Override
        public int size() {
            return handler.size();
        }

        @Override
        public ItemStack get(int slot) {
            return ItemUtil.getStack(handler, slot);
        }

        @Override
        public ItemStack extract(int slot, int amount) {
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int taken = handler.extract(slot, resource, Math.min(amount, resource.getMaxStackSize()), transaction);
                transaction.commit();
                return taken <= 0 ? ItemStack.EMPTY : resource.toStack(taken);
            }
        }

        @Override
        public ItemStack insert(ItemStack stack) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), null);
            return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            // Never committed: closing the transaction undoes the insertion.
            try (Transaction transaction = Transaction.openRoot()) {
                return handler.insert(ItemResource.of(stack), 1, transaction) > 0;
            }
        }
    }
}
