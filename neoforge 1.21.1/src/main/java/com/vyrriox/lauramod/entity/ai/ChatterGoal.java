package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.util.InteractionDatabase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class ChatterGoal extends Goal {
    private final LauraEntity laura;

    public ChatterGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = (Player) laura.getOwner();
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("<Laura> " + InteractionDatabase.getRandomMessage()));
        }
        cooldown = 2400; // 2 minutes min between chatter
    }
}
