package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class ComplainGoal extends Goal {
    private final LauraEntity laura;
    private int cooldown;

    public ComplainGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return (laura.level().isRaining() || !laura.level().isDay()) && laura.getRandom().nextInt(200) == 0;
    }

    @Override
    public void start() {
        Player owner = (Player) laura.getOwner();
        if (owner != null) {
            if (laura.level().isRaining()) {
                owner.sendSystemMessage(Component.translatable("chat.lauramod.complain_rain"));
            } else if (!laura.level().isDay()) {
                owner.sendSystemMessage(Component.translatable("chat.lauramod.complain_night"));
            }
        }
        cooldown = 2400; // 2 minutes cooldown
    }
}
