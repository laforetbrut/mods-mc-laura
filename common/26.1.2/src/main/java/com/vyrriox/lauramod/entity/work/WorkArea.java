package com.vyrriox.lauramod.entity.work;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * A cylinder-like work zone: a center, a horizontal radius and a dimension.
 *
 * @author vyrriox
 */
public record WorkArea(BlockPos center, int radius, ResourceKey<Level> dimension) {
    public boolean contains(BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        int dy = pos.getY() - center.getY();
        return dx * dx + dz * dz <= radius * radius && Math.abs(dy) <= Math.max(6, radius / 2);
    }

    public boolean isIn(Level level) {
        return level.dimension().equals(dimension);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        tag.putString("Dim", dimension.identifier().toString());
        return tag;
    }

    public static WorkArea load(CompoundTag tag) {
        Identifier dim = Identifier.tryParse(tag.getStringOr("Dim", ""));
        return new WorkArea(BlockPos.of(tag.getLongOr("Center", 0L)), Math.max(1, tag.getIntOr("Radius", 0)),
                ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.identifier() : dim));
    }

    public String describe() {
        return center.getX() + " " + center.getY() + " " + center.getZ() + " (r=" + radius + ")";
    }
}
