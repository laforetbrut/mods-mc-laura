package com.vyrriox.lauramod.neoforge.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

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

    /** Puts one of the stack in a free slot that accepts it. Returns true when she wears it (or could). */
    public static boolean equip(LivingEntity entity, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || CuriosApi.getItemStackSlots(stack, entity).isEmpty()) {
            return false;
        }
        LazyOptional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
        ICuriosItemHandler handler = inventory.resolve().orElse(null);
        if (handler == null) {
            return false;
        }
        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
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
