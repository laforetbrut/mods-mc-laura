package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class LauraSleepGoal extends Goal {
    private final LauraEntity laura;
    private Player owner;

    public LauraSleepGoal(LauraEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!laura.isTame())
            return false;
        this.owner = (Player) laura.getOwner();
        return owner != null && owner.isSleeping();
    }

    @Override
    public void start() {
        if (this.laura.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            BlockPos playerPos = player.blockPosition();
            if (this.laura.level().getBlockState(playerPos).getBlock() instanceof BedBlock) {
                this.laura.getNavigation().moveTo(playerPos.getX(), playerPos.getY(), playerPos.getZ(), 1.0D);
            } else {
                this.laura.sendSystemMessage(Component.translatable("chat.lauramod.nobed"));
            }
        }
    }

    @Override
    public void tick() {
        if (owner == null)
            return;
        if (laura.distanceToSqr(owner) > 5) {
            laura.getNavigation().moveTo(owner, 1.0);
        }
        // TODO: Logic to make her actually lie down or complain about no bed
    }
}
