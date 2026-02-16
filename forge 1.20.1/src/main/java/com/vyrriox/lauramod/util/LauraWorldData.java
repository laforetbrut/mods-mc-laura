package com.vyrriox.lauramod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import javax.annotation.Nullable;

import java.util.UUID;

public class LauraWorldData extends SavedData {
    private static final String DATA_NAME = "lauramod_world_data";
    private UUID lauraUUID = null;
    private boolean exists = false;

    public static LauraWorldData get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DimensionDataStorage storage = serverLevel.getServer().overworld().getDataStorage();
            return storage.computeIfAbsent(LauraWorldData::load, LauraWorldData::new, DATA_NAME);
        }
        return new LauraWorldData();
    }

    public boolean exists() {
        return exists;
    }

    public void setExists(boolean exists, @Nullable UUID uuid) {
        this.exists = exists;
        this.lauraUUID = uuid;
        this.setDirty();
    }

    public UUID getLauraUUID() {
        return lauraUUID;
    }

    public static LauraWorldData load(CompoundTag tag) {
        LauraWorldData data = new LauraWorldData();
        data.exists = tag.getBoolean("Exists");
        if (tag.hasUUID("LauraUUID")) {
            data.lauraUUID = tag.getUUID("LauraUUID");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Exists", exists);
        if (lauraUUID != null) {
            tag.putUUID("LauraUUID", lauraUUID);
        }
        return tag;
    }
}
