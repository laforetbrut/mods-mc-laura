package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.world.LauraWorldChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Dances next to a jukebox that plays music.
 *
 * @author vyrriox
 */
public class LauraDanceGoal extends Goal {
    private final LauraEntity laura;
    private BlockPos jukebox;
    private int cooldown;
    private int ticks;

    public LauraDanceGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 60;
        if (!LauraConfig.danceToJukebox.get() || laura.isFetching() || laura.isAsleep() || laura.brain().isSulking() || laura.isGagged() && laura.getRandom().nextBoolean()) {
            return false;
        }
        jukebox = LauraWorldChecks.findPlayingJukebox(laura, 12);
        return jukebox != null;
    }

    @Override
    public boolean canContinueToUse() {
        return jukebox != null && ticks % 40 != 39 || LauraWorldChecks.findPlayingJukebox(laura, 12) != null;
    }

    @Override
    public void start() {
        ticks = 0;
        if (laura.getRandom().nextInt(3) == 0) {
            LauraSpeech.sayToOwner(laura, "dance.jukebox", LineFormatter.values());
            com.vyrriox.lauramod.world.LauraAdvancements.award(LauraSpeech.owner(laura), "dance_jukebox");
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (jukebox == null) {
            return;
        }
        if (laura.blockPosition().distSqr(jukebox) > 16) {
            if (ticks % 20 == 1) {
                laura.getNavigation().moveTo(jukebox.getX() + 0.5, jukebox.getY(), jukebox.getZ() + 0.5, 1.0);
            }
            return;
        }
        laura.getNavigation().stop();
        if (!laura.isEmoting()) {
            laura.playEmote(Emote.DANCE);
            laura.brain().onDance();
        }
    }

    @Override
    public void stop() {
        jukebox = null;
        if (laura.getEmote() == Emote.DANCE) {
            laura.playEmote(Emote.NONE);
        }
    }
}
