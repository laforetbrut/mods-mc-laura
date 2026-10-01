package com.vyrriox.lauramod.neoforge.compat;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Curios: Laura has trinket slots (data/lauramod/curios/entities/laura.json). A ring, a necklace
 * or any trinket given to her goes into a free valid slot and works on her. Only loaded when Curios
 * is installed.
 *
 * @author vyrriox
 */
public final class CuriosTrinkets {
    private CuriosTrinkets() {
    }

    /** Registers the Curios listeners. Called once, only when Curios is installed. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(DropRulesEvent.class, CuriosTrinkets::onDropRules);
    }

    /**
     * Curios drops the trinkets of any entity that dies. With the GRAVE and TIMER revive modes she
     * comes back with everything she had (her snapshot is taken before the drops), so her trinkets
     * stay on her: dropped as well, they would be duplicated.
     */
    private static void onDropRules(DropRulesEvent event) {
        if (event.getEntity() instanceof LauraEntity laura && laura.getOwnerUUID() != null && LauraManager.keepsBelongingsOnDeath()) {
            event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP);
        }
    }

    /** Copies of the trinkets the entity wears. */
    public static List<ItemStack> worn(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values()) {
                collect(handler.getStacks(), out, false);
                collect(handler.getCosmeticStacks(), out, false);
            }
        });
        return out;
    }

    /** Drops every trinket at the entity's feet and empties the slots (she leaves for good). */
    public static void dropAll(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values()) {
                collect(handler.getStacks(), out, true);
                collect(handler.getCosmeticStacks(), out, true);
            }
        });
        for (ItemStack stack : out) {
            entity.spawnAtLocation(stack);
        }
    }

    private static void collect(IDynamicStackHandler stacks, List<ItemStack> out, boolean remove) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (!stack.isEmpty()) {
                out.add(stack.copy());
                if (remove) {
                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /** Puts one of the stack in a free slot that accepts it. Returns true when she wears it (or could). */
    public static boolean equip(LivingEntity entity, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || CuriosApi.getItemStackSlots(stack, entity).isEmpty()) {
            return false;
        }
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        if (inventory.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, ICurioStacksHandler> entry : inventory.get().getCurios().entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (!stacks.getStackInSlot(i).isEmpty()) {
                    continue;
                }
                SlotContext context = new SlotContext(entry.getKey(), entity, i, false, true);
                if (CuriosApi.isStackValid(context, stack)) {
                    if (!simulate) {
                        stacks.setStackInSlot(i, stack.copyWithCount(1));
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
