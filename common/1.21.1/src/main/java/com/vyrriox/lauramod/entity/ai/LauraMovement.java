package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Safe teleport helper shared by the follow, home and fetch goals. Kept in one class because the
 * path type API changes between Minecraft versions.
 *
 * @author vyrriox
 */
public final class LauraMovement {
    private LauraMovement() {
    }

    /** Teleports next to the position if a safe spot exists. Returns true on success. */
    public static boolean teleportNear(LauraEntity laura, BlockPos target) {
        for (int i = 0; i < 16; i++) {
            int dx = laura.getRandom().nextIntBetweenInclusive(-3, 3);
            int dz = laura.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 1 && Math.abs(dz) < 1) {
                continue;
            }
            int dy = laura.getRandom().nextIntBetweenInclusive(-1, 1);
            BlockPos pos = target.offset(dx, dy, dz);
            if (canStandAt(laura, pos)) {
                laura.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, laura.getYRot(), laura.getXRot());
                laura.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    public static boolean canStandAt(LauraEntity laura, BlockPos pos) {
        PathType type = WalkNodeEvaluator.getPathTypeStatic(laura, pos);
        if (type != PathType.WALKABLE) {
            return false;
        }
        if (laura.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos delta = pos.subtract(laura.blockPosition());
        return laura.level().noCollision(laura, laura.getBoundingBox().move(delta));
    }
}
