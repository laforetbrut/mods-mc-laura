package com.vyrriox.lauramod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {
    private final PathfinderMob mob;
    private LivingEntity targetPlayer;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;

    public FollowPlayerGoal(PathfinderMob mob, double speedModifier, float startDistance, float stopDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.navigation = mob.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity player = this.mob.level().getNearestPlayer(this.mob, 50.0D);
        if (player == null) {
            return false;
        } else if (this.mob.distanceToSqr(player) < (double) (this.startDistance * this.startDistance)) {
            return false;
        } else {
            this.targetPlayer = player;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.navigation.isDone()
                && this.mob.distanceToSqr(this.targetPlayer) > (double) (this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.targetPlayer, 10.0F, (float) this.mob.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.mob.isLeashed() && !this.mob.isPassenger()) {
                if (this.mob.distanceToSqr(this.targetPlayer) >= 144.0D) {
                    this.teleportToPlayer();
                } else {
                    this.navigation.moveTo(this.targetPlayer, this.speedModifier);
                }
            }
        }
    }

    private void teleportToPlayer() {
        for (int i = 0; i < 10; ++i) {
            int j = this.getRandomInt(-3, 3);
            int k = this.getRandomInt(-1, 1);
            int l = this.getRandomInt(-3, 3);
            if (this.maybeTeleport(this.targetPlayer.getX() + (double) j, this.targetPlayer.getY() + (double) k,
                    this.targetPlayer.getZ() + (double) l)) {
                return;
            }
        }
    }

    private boolean maybeTeleport(double x, double y, double z) {
        if (Math.abs(x - this.targetPlayer.getX()) < 2.0D && Math.abs(z - this.targetPlayer.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(x, y, z)) {
            return false;
        } else {
            this.mob.moveTo(x, y, z, this.mob.getYRot(), this.mob.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(double x, double y, double z) {
        PathType pathtype = this.navigation.getNodeEvaluator().getPathType(this.mob.level(), (int) x, (int) y, (int) z);
        if (pathtype != PathType.WALKABLE) {
            return false;
        } else {
            return this.mob.level().noCollision(this.mob,
                    this.mob.getBoundingBox().move(x - this.mob.getX(), y - this.mob.getY(), z - this.mob.getZ()));
        }
    }

    private int getRandomInt(int min, int max) {
        return this.mob.getRandom().nextInt(max - min + 1) + min;
    }
}
