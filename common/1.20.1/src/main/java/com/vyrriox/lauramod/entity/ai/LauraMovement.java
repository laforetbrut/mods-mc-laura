package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Safe teleport helper shared by the follow, home and fetch goals and by the world manager. Kept in
 * one class because the path type and the dimension change APIs differ between Minecraft versions.
 *
 * @author vyrriox
 */
public final class LauraMovement {
    /** How far around the target a free spot is looked for before falling back to the ground below it. */
    private static final int NEAR = 4;
    /** Columns around the target that are searched downwards for ground. */
    private static final int BELOW = 2;

    private LauraMovement() {
    }

    /** Teleports next to the position if a safe spot exists. Returns true on success. */
    public static boolean teleportNear(LauraEntity laura, BlockPos target) {
        BlockPos spot = randomSpot(laura, target);
        if (spot == null) {
            return false;
        }
        laura.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, laura.getYRot(), laura.getXRot());
        laura.getNavigation().stop();
        // She may have been falling: the fall must not count where she lands.
        laura.resetFallDistance();
        LauraManager.moved(laura);
        return true;
    }

    /** A standable spot picked at random a few blocks around the target, so that she does not always land on the same block. */
    @Nullable
    private static BlockPos randomSpot(LauraEntity laura, BlockPos target) {
        for (int i = 0; i < 16; i++) {
            int dx = laura.getRandom().nextIntBetweenInclusive(-3, 3);
            int dz = laura.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 1 && Math.abs(dz) < 1) {
                continue;
            }
            int dy = laura.getRandom().nextIntBetweenInclusive(-1, 1);
            BlockPos pos = target.offset(dx, dy, dz);
            if (canStandAt(laura, pos)) {
                return pos;
            }
        }
        return null;
    }

    public static boolean canStandAt(LauraEntity laura, BlockPos pos) {
        BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(laura.level(), pos.mutable());
        if (type != BlockPathTypes.WALKABLE) {
            return false;
        }
        if (laura.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
            return false;
        }
        // Never inside a portal: it would take her away again at once.
        if (isPortal(laura.level().getBlockState(pos).getBlock()) || isPortal(laura.level().getBlockState(pos.above()).getBlock())) {
            return false;
        }
        return fits(laura, pos);
    }

    /** The blocks that send whoever touches them somewhere else. */
    private static boolean isPortal(Block block) {
        return block instanceof NetherPortalBlock || block instanceof EndPortalBlock || block instanceof EndGatewayBlock;
    }

    /** The surface of water: she floats there, next to a partner who swims or sits in a boat. */
    private static boolean canFloatAt(LauraEntity laura, BlockPos pos) {
        Level level = laura.level();
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).isEmpty() && fits(laura, pos);
    }

    /** Her standing body, centered on the block, touches nothing. */
    private static boolean fits(LauraEntity laura, BlockPos pos) {
        return laura.level().noCollision(laura, laura.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(pos)));
    }

    /**
     * A safe place for her next to the target, in the level she is in: a random spot close by, else
     * the nearest spot she can stand on or float at, else where she would land under the target (a
     * partner who flies). Null when there is none, as over the void or a lake of lava: she must then
     * stay where she is, never be dropped at the target itself.
     */
    @Nullable
    public static BlockPos findSafeSpot(LauraEntity laura, BlockPos target) {
        BlockPos spot = randomSpot(laura, target);
        if (spot != null) {
            return spot;
        }
        spot = BlockPos.findClosestMatch(target, NEAR, NEAR, pos -> canStandAt(laura, pos)).map(BlockPos::immutable).orElse(null);
        if (spot != null) {
            return spot;
        }
        spot = BlockPos.findClosestMatch(target, NEAR, NEAR, pos -> canFloatAt(laura, pos)).map(BlockPos::immutable).orElse(null);
        if (spot != null) {
            return spot;
        }
        Level level = laura.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (BlockPos column : BlockPos.withinManhattan(target, BELOW, 0, BELOW)) {
            // Down through the air to the first thing under the target: ground or water takes her,
            // anything else (lava, fire, a cactus) rules the column out. Never further down, into a cave.
            for (int y = target.getY() - NEAR - 1; y > level.getMinBuildHeight(); y--) {
                pos.set(column.getX(), y, column.getZ());
                if (canStandAt(laura, pos) || canFloatAt(laura, pos)) {
                    return pos.immutable();
                }
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Same search in another level. Every check above reads the level of the entity it is given,
     * so a stand-in of her kind, never added to the world, does the looking.
     */
    @Nullable
    public static BlockPos findSafeSpot(LauraEntity laura, ServerLevel level, BlockPos target) {
        if (laura.level() == level) {
            return findSafeSpot(laura, target);
        }
        LauraEntity scout = LauraRegistries.LAURA.get().create(level);
        return scout == null ? null : findSafeSpot(scout, target);
    }

    /**
     * Puts her on a spot, in her level or in another one. The game moves an entity to another
     * dimension by replacing it with a copy there: that copy is returned and the entity passed in
     * is gone. Null when the trip was refused (a second her is already there); she is then
     * unchanged.
     */
    @Nullable
    public static LauraEntity place(LauraEntity laura, ServerLevel level, BlockPos spot) {
        Vec3 at = Vec3.atBottomCenterOf(spot);
        // She may have been falling: the fall must not count where she lands.
        laura.resetFallDistance();
        if (laura.level() == level) {
            laura.moveTo(at.x, at.y, at.z, laura.getYRot(), laura.getXRot());
            laura.getNavigation().stop();
            return laura;
        }
        // As after a portal: she usually lands next to the one her partner came through and must not walk straight back.
        laura.setPortalCooldown();
        return laura.moveToLevel(level, at.x, at.y, at.z, laura.getYRot(), laura.getXRot());
    }
}
