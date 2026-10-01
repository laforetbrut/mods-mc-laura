package com.vyrriox.lauramod.entity.work;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A job routine, ticked by {@link com.vyrriox.lauramod.entity.ai.LauraWorkGoal}.
 *
 * @author vyrriox
 */
public interface Work {
    enum Status {
        /** Busy, keep ticking. */
        WORKING,
        /** Nothing to do right now. */
        IDLE,
        /** One-shot errand finished. */
        DONE,
        /** Cannot do it (the reason key is in {@link #failKey()}). */
        FAILED
    }

    Status tick();

    /** Called when the work is interrupted or switched. */
    void stop();

    /** Dialogue key explaining a failure. */
    String failKey();

    /** Items produced for a one-shot errand, to bring back to her partner. */
    List<ItemStack> products();
}
