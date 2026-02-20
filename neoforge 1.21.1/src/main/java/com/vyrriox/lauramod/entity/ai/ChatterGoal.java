package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.util.InteractionDatabase;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class ChatterGoal extends Goal {
    private final LauraEntity laura;
    private int chatterTimer = 12000; // 10 minutes

    public ChatterGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (laura.isTame() && !laura.isSad() && laura.getOwner() != null) {
            if (chatterTimer > 0) {
                chatterTimer--;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        Player owner = (Player) laura.getOwner();
        if (owner instanceof ServerPlayer serverPlayer) {
            owner.sendSystemMessage(
                    Component.literal("<§dLaura§r> " + InteractionDatabase.getRandomMessage(serverPlayer.getLanguage())));
        }
        chatterTimer = 12000; // Reset to 10 minutes
    }
}
