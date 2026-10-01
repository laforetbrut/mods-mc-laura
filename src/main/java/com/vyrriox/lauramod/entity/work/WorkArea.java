package com.vyrriox.lauramod.entity.work;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
        tag.putString("Dim", dimension.location().toString());
        return tag;
    }

    public static WorkArea load(CompoundTag tag) {
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
        return new WorkArea(BlockPos.of(tag.getLong("Center")), Math.max(1, tag.getInt("Radius")),
                ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.location() : dim));
    }

    public String describe() {
        return center.getX() + " " + center.getY() + " " + center.getZ() + " (r=" + radius + ")";
    }
}
