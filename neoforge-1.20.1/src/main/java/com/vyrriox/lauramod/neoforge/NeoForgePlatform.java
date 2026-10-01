package com.vyrriox.lauramod.neoforge;

import com.vyrriox.lauramod.neoforge.compat.Ae2Storage;
import com.vyrriox.lauramod.neoforge.compat.CuriosTrinkets;
import com.vyrriox.lauramod.neoforge.compat.FarmersDelightPots;
import com.vyrriox.lauramod.platform.CookingPots;
import com.vyrriox.lauramod.platform.InventoryAccess;
import com.vyrriox.lauramod.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
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
 * NeoForge implementation of the loader services.
 *
 * @author vyrriox
 */
final class NeoForgePlatform implements Platform {
    private final ModContainer container;

    private final boolean ae2;
    private final boolean curios;
    private CookingPots cookingPots;

    NeoForgePlatform(ModContainer container) {
        this.container = container;
        this.ae2 = ModList.get().isLoaded("ae2");
        this.curios = ModList.get().isLoaded("curios");
        if (this.curios) {
            CuriosTrinkets.register();
        }
    }

    // The compat classes are only touched behind these checks, so they are never loaded without their mod.

    @Override
    public CookingPots cookingPots() {
        if (cookingPots == null) {
            cookingPots = ModList.get().isLoaded("farmersdelight") ? new FarmersDelightPots() : CookingPots.NONE;
        }
        return cookingPots;
    }

    @Override
    public boolean equipTrinket(LivingEntity entity, ItemStack stack, boolean simulate) {
        return curios && CuriosTrinkets.equip(entity, stack, simulate);
    }

    @Override
    public java.util.List<ItemStack> trinkets(LivingEntity entity) {
        return curios ? CuriosTrinkets.worn(entity) : java.util.List.of();
    }

    @Override
    public void dropTrinkets(LivingEntity entity) {
        if (curios) {
            CuriosTrinkets.dropAll(entity);
        }
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
    public String playerLanguage(ServerPlayer player) {
        // NeoForge keeps the language of the client settings packet on the player, like Forge.
        return player.getLanguage();
    }

    /**
     * Gives a respawned player the client settings of the player it replaces. Minecraft 1.20.1
     * builds a new player on respawn, the client does not send its settings again and NeoForge does
     * not copy the language, so Laura would answer in English after a death.
     */
    static void carryClientSettings(ServerPlayer from, ServerPlayer to) {
        int modelParts = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            if (from.isModelPartShown(part)) {
                modelParts |= part.getMask();
            }
        }
        // updateOptions does not read the view distance.
        to.updateOptions(new ServerboundClientInformationPacket(from.getLanguage(), 0, from.getChatVisibility(), from.canChatInColor(),
                modelParts, from.getMainArm(), from.isTextFilteringEnabled(), from.allowsListing()));
    }

    @Override
    public void sendToServer(byte[] data) {
        LauraChannel.sendToServer(data);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, byte[] data) {
        // Players without the mod (or test players) have no channel: skip them quietly.
        if (player.connection != null && LauraChannel.isRemotePresent(player.connection.connection)) {
            LauraChannel.sendToPlayer(player, data);
        }
    }

    @Override
    public boolean mayUseContainer(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // The event NeoForge posts when a player right clicks a block: claim mods deny it there.
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY;
    }

    @Override
    public boolean mayChangeDimension(net.minecraft.world.entity.Entity entity, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destination) {
        // The event NeoForge posts before a portal takes an entity to another dimension: other mods cancel it there.
        return net.minecraftforge.common.ForgeHooks.onTravelToDimension(entity, destination);
    }

    @Override
    public InventoryAccess inventoryAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (ae2) {
            // A block of an ME network: the whole network, not just the block's own slots.
            InventoryAccess network = Ae2Storage.at(level, pos);
            if (network != null) {
                return network;
            }
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    @Override
    public InventoryAccess itemInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        return handler == null ? null : new ItemHandlerAccess(handler);
    }

    /** Any item handler capability: vanilla chests, Sophisticated Storage, AE2 and RS interfaces, Create vaults... */
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
