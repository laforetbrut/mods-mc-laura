package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.brain.Needs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Random walks: around her home, around where she was told to wander, or near her partner when
 * they are not moving. When bored she wanders further, just to make you look for her.
 *
 * @author vyrriox
 */
public class LauraStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final LauraEntity laura;

    public LauraStrollGoal(LauraEntity laura) {
        super(laura, 0.8);
        this.laura = laura;
    }

    @Override
    public boolean canUse() {
        if (laura.isOrderedToSit() || laura.brain().isSulking() || laura.isAsleep() || laura.isFetching() || laura.isEmoting()) {
            return false;
        }
        LauraMode mode = laura.getMode();
        if (mode == LauraMode.FOLLOW) {
            LivingEntity owner = laura.getOwner();
            if (owner == null || owner.distanceToSqr(laura) > 6 * 6 || owner.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                return false;
            }
        }
        return super.canUse();
    }

    @Override
    protected Vec3 getPosition() {
        Vec3 pos = super.getPosition();
        if (pos == null) {
            return null;
        }
        BlockPos center;
        double radius;
        switch (laura.getMode()) {
            case HOME -> {
                if (laura.getHomePos() == null) {
                    return pos;
                }
                center = laura.getHomePos();
                radius = LauraConfig.homeRadius.getInt();
            }
            case WANDER -> {
                center = laura.getWanderCenter();
                boolean bored = LauraConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.FUN) < 30;
                radius = bored ? 32 : 16;
            }
            default -> {
                LivingEntity owner = laura.getOwner();
                if (owner == null) {
                    return pos;
                }
                center = owner.blockPosition();
                radius = 6;
            }
        }
        return pos.distanceToSqr(Vec3.atBottomCenterOf(center)) <= radius * radius ? pos : null;
    }
}
