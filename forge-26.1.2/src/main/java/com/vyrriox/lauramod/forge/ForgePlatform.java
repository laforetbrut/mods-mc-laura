package com.vyrriox.lauramod.forge;

import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.nio.file.Path;

/**
 * Forge implementation of the loader services.
 *
 * @author vyrriox
 */
final class ForgePlatform implements Platform {
    private final ModContainer container;

    ForgePlatform(ModContainer container) {
        this.container = container;
    }

    @Override
    public String loaderName() {
        return "forge";
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
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }

    @Override
    public void sendToServer(byte[] data) {
        LauraChannel.sendToServer(data);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, byte[] data) {
        // Players without the mod (or test players) have no channel: skip them quietly.
        LauraChannel.sendToPlayer(player, data);
    }

    @Override
    public InventoryAccess inventoryAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        // No side: the whole inventory, as a player opening the block would see it.
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    @Override
    public InventoryAccess itemInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    /** Any Forge item handler: vanilla chests, Sophisticated Storage, Refined Storage interfaces, Create vaults... */
    private record ItemHandlerAccess(IItemHandler handler) implements InventoryAccess {
        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public ItemStack get(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extract(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack insert(ItemStack stack) {
            return ItemHandlerHelper.insertItemStacked(handler, stack, false);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(1), true).isEmpty();
        }
    }
}
