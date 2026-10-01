package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Keeps her still while she plays an emote, facing her partner.
 *
 * @author vyrriox
 */
public class LauraEmoteGoal extends Goal {
    private final LauraEntity laura;

    public LauraEmoteGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Emote emote = laura.getEmote();
        return laura.isEmoting() && emote != Emote.EAT && emote != Emote.POKE && emote != Emote.SLAP && !laura.isFetching();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        laura.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity owner = laura.getOwner();
        if (owner != null && owner.distanceToSqr(laura) < 16 * 16 && laura.getEmote() != Emote.TWIRL) {
            laura.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        }
        if (laura.getEmote() == Emote.TWIRL) {
            laura.setYBodyRot(laura.yBodyRot + 24.0F);
            laura.setYHeadRot(laura.yBodyRot);
        }
    }
}
