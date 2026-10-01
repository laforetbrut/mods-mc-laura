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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

/**
 * NeoForge implementation of the loader services.
 *
 * @author vyrriox
 */
final class NeoForgePlatform implements Platform {
    private final ModContainer container;

    private final boolean ae2;
    private final boolean curios;

    NeoForgePlatform(ModContainer container) {
        this.container = container;
        this.ae2 = ModList.get().isLoaded("ae2");
        this.curios = ModList.get().isLoaded("curios");
    }

    @Override
    public void configureMockConnection(net.minecraft.network.Connection connection) {
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
    }

    @Override
    public com.vyrriox.lauramod.platform.CookingPots cookingPots() {
        if (cookingPots == null) {
            cookingPots = ModList.get().isLoaded("farmersdelight")
                    ? new com.vyrriox.lauramod.neoforge.compat.FarmersDelightPots()
                    : com.vyrriox.lauramod.platform.CookingPots.NONE;
        }
        return cookingPots;
    }

    private com.vyrriox.lauramod.platform.CookingPots cookingPots;

    @Override
    public boolean equipTrinket(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, boolean simulate) {
        return curios && com.vyrriox.lauramod.neoforge.compat.CuriosTrinkets.equip(entity, stack, simulate);
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
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void sendToServer(byte[] data) {
        PacketDistributor.sendToServer(new LauraPayload(data));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, byte[] data) {
        // Players without the mod (or test players) have no channel: skip them quietly.
        if (player.connection != null && player.connection.hasChannel(LauraPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new LauraPayload(data));
        }
    }

    @Override
    public boolean mayUseContainer(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // The event NeoForge posts when a player right clicks a block: claim mods deny it there.
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event =
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseBlock() != net.neoforged.neoforge.common.util.TriState.FALSE;
    }

    @Override
    public InventoryAccess inventoryAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (ae2) {
            // A block of an ME network: the whole network, not just the block's own slots.
            InventoryAccess network = com.vyrriox.lauramod.neoforge.compat.Ae2Storage.at(level, pos);
            if (network != null) {
                return network;
            }
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    @Override
    public InventoryAccess itemInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    /** Any NeoForge item handler: vanilla chests, Sophisticated Storage, AE2 and RS interfaces, Create vaults... */
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
