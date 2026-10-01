package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * When she craves attention she plants herself right in front of her partner, stares, waves and
 * pokes. The more annoying the setting, the more often.
 *
 * @author vyrriox
 */
public class LauraAttentionGoal extends Goal {
    private final LauraEntity laura;
    private LivingEntity owner;
    private int ticks;
    private int cooldown;

    public LauraAttentionGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0 || !LauraConfig.needsEnabled.get()) {
            return false;
        }
        cooldown = (int) (400 / Math.max(0.25, LauraConfig.annoyance.get().nagging));
        LivingEntity o = laura.getOwner();
        if (o == null || o.level() != laura.level() || laura.distanceToSqr(o) > 20 * 20 || laura.isAsleep() || laura.isFetching()
                || laura.isOrderedToSit() || laura.getMode() == LauraMode.HOME) {
            return false;
        }
        if (laura.brain().needs().get(Needs.Need.ATTENTION) > 22) {
            return false;
        }
        owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && ticks < 160 && laura.brain().needs().get(Needs.Need.ATTENTION) < 40;
    }

    @Override
    public void start() {
        ticks = 0;
    }

    @Override
    public void tick() {
        ticks++;
        laura.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (laura.distanceToSqr(owner) > 1.8 * 1.8) {
            if (ticks % 10 == 1) {
                laura.getNavigation().moveTo(owner, 1.2);
            }
        } else {
            laura.getNavigation().stop();
            if (ticks % 60 == 20) {
                laura.playEmote(laura.getRandom().nextBoolean() ? Emote.WAVE : Emote.JUMP);
            } else if (ticks % 60 == 50 && owner instanceof ServerPlayer player) {
                laura.brain().poke(player);
            }
        }
    }

    @Override
    public void stop() {
        owner = null;
        laura.getNavigation().stop();
    }
}
