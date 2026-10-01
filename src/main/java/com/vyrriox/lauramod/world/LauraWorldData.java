package com.vyrriox.lauramod.world;

import com.mojang.serialization.Codec;
import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide registry of every Laura (stored in the world data folder as
 * {@code data/lauramod/world_data.dat}; the file of older worlds is read once). Each record is one companion bound to the player who summoned her; a player can
 * have several. Lets commands and chat find them anywhere, keeps them while they wait on a grave
 * and restores them if they get lost.
 *
 * @author vyrriox
 */
public class LauraWorldData extends SavedData {
    /** File name used before Minecraft 26.1, directly in the world data folder. */
    public static final String NAME = "lauramod_world_data";
    private static final Codec<LauraWorldData> CODEC = CompoundTag.CODEC.xmap(LauraWorldData::load, data -> data.save(new CompoundTag()));
    /** Mod data has no data fixer: NeoForge, Forge and Fabric API skip the fix step when the type is null. */
    public static final SavedDataType<LauraWorldData> TYPE = new SavedDataType<>(LauraMod.id("world_data"), LauraWorldData::new, CODEC, null);

    /** One companion. */
    public static final class Record {
        public UUID laura;
        public UUID owner;
        public String ownerName = "";
        public String lauraName = "Laura";
        public ResourceKey<Level> dimension = Level.OVERWORLD;
        public BlockPos pos = BlockPos.ZERO;
        /** Last saved copy of her data: brings her back after a death or if she gets lost. */
        public CompoundTag snapshot;
        /** Dead and waiting for a flower on a gravestone. */
        public boolean dead;
        /** Game time at which she comes back (TIMER revive mode), -1 otherwise. */
        public long respawnAt = -1;
        public boolean dismissed;
        /** She was following her partner the last time she was seen (used to bring her along after a teleport). */
        public boolean following;
        public long createdAt = System.currentTimeMillis();

        /** Present in the world or able to come back. */
        public boolean isActive() {
            return !dead && respawnAt < 0 && !dismissed;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.store("Laura", UUIDUtil.CODEC, laura);
            tag.store("Owner", UUIDUtil.CODEC, owner);
            tag.putString("OwnerName", ownerName);
            tag.putString("LauraName", lauraName);
            tag.putString("Dim", dimension.identifier().toString());
            tag.putLong("Pos", pos.asLong());
            if (snapshot != null) {
                tag.put("Snapshot", snapshot);
            }
            tag.putBoolean("Dead", dead);
            tag.putLong("RespawnAt", respawnAt);
            tag.putBoolean("Dismissed", dismissed);
            tag.putBoolean("Following", following);
            tag.putLong("Created", createdAt);
            return tag;
        }

        static Record load(CompoundTag tag) {
            Record r = new Record();
            r.laura = tag.read("Laura", UUIDUtil.CODEC).orElseThrow();
            r.owner = tag.read("Owner", UUIDUtil.CODEC).orElseThrow();
            r.ownerName = tag.getStringOr("OwnerName", "");
            r.lauraName = tag.getStringOr("LauraName", "Laura");
            Identifier dim = Identifier.tryParse(tag.getStringOr("Dim", ""));
            r.dimension = ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.identifier() : dim);
            r.pos = BlockPos.of(tag.getLongOr("Pos", 0L));
            r.snapshot = tag.getCompound("Snapshot").orElse(null);
            r.dead = tag.getBooleanOr("Dead", false);
            r.respawnAt = tag.getLongOr("RespawnAt", -1);
            r.dismissed = tag.getBooleanOr("Dismissed", false);
            r.following = tag.getBooleanOr("Following", false);
            r.createdAt = tag.getLongOr("Created", 0L);
            return r;
        }
    }

    /** Per player data. */
    public static final class OwnerMeta {
        public UUID owner;
        public long lastLogout;
        public long lastSummon;
        public UUID selected;
        /** Advancement counters (desires fulfilled, items fetched...). */
        public final Map<String, Integer> stats = new HashMap<>();

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.store("Owner", UUIDUtil.CODEC, owner);
            tag.putLong("LastLogout", lastLogout);
            tag.putLong("LastSummon", lastSummon);
            if (selected != null) {
                tag.store("Selected", UUIDUtil.CODEC, selected);
            }
            if (!stats.isEmpty()) {
                CompoundTag statsTag = new CompoundTag();
                stats.forEach(statsTag::putInt);
                tag.put("Stats", statsTag);
            }
            return tag;
        }

        static OwnerMeta load(CompoundTag tag) {
            OwnerMeta m = new OwnerMeta();
            m.owner = tag.read("Owner", UUIDUtil.CODEC).orElseThrow();
            m.lastLogout = tag.getLongOr("LastLogout", 0L);
            m.lastSummon = tag.getLongOr("LastSummon", 0L);
            m.selected = tag.read("Selected", UUIDUtil.CODEC).orElse(null);
            CompoundTag statsTag = tag.getCompoundOrEmpty("Stats");
            for (String key : statsTag.keySet()) {
                m.stats.put(key, statsTag.getIntOr(key, 0));
            }
            return m;
        }
    }

    private final Map<UUID, Record> records = new HashMap<>();
    private final Map<UUID, OwnerMeta> owners = new HashMap<>();
    /** The single Laura of a 1.x world, before she is registered again. */
    private UUID legacyLaura;
    /** Companions an administrator removed while they were not loaded: they leave when they are loaded. */
    private final java.util.Set<UUID> removed = new java.util.LinkedHashSet<>();
    private static final int MAX_REMOVED = 1024;

    /** Remembers that this companion must leave the world as soon as she is loaded. */
    public void markRemoved(UUID laura) {
        removed.add(laura);
        java.util.Iterator<UUID> oldest = removed.iterator();
        while (removed.size() > MAX_REMOVED && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
        setDirty();
    }

    /** True, once, for a companion that was removed while she was not loaded. */
    public boolean consumeRemoved(UUID laura) {
        if (removed.remove(laura)) {
            setDirty();
            return true;
        }
        return false;
    }

    public static LauraWorldData get(MinecraftServer server) {
        SavedDataStorage storage = server.getDataStorage();
        LauraWorldData data = storage.get(TYPE);
        if (data == null) {
            data = loadLegacy(server);
            storage.set(TYPE, data == null ? data = new LauraWorldData() : data);
        }
        return data;
    }

    /** A world last played before Minecraft 26.1 still has its companions in the old file. */
    private static LauraWorldData loadLegacy(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.DATA).resolve(NAME + ".dat");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            LauraWorldData data = load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("data"));
            LauraMod.LOGGER.info("Read {} companions from the world data of an older version", data.records.size());
            return data;
        } catch (IOException | RuntimeException e) {
            LauraMod.LOGGER.warn("Could not read the old companion data {}: {}", file.getFileName(), e.toString());
            return null;
        }
    }

    public Record get(UUID laura) {
        return records.get(laura);
    }

    public Record getOrCreate(UUID laura, UUID owner) {
        Record r = records.get(laura);
        if (r == null) {
            r = new Record();
            r.laura = laura;
            r.owner = owner;
            records.put(laura, r);
            setDirty();
        }
        return r;
    }

    public void remove(UUID laura) {
        if (records.remove(laura) != null) {
            setDirty();
        }
    }

    /** Forgets every companion of a player and their stats (used by the self test for its fake players). */
    public void forgetOwner(UUID owner) {
        boolean changed = records.values().removeIf(r -> owner.equals(r.owner));
        changed |= owners.remove(owner) != null;
        if (changed) {
            setDirty();
        }
    }

    public Collection<Record> all() {
        return Collections.unmodifiableCollection(records.values());
    }

    /** Every companion of a player, oldest first. */
    public List<Record> byOwner(UUID owner) {
        List<Record> out = new ArrayList<>();
        for (Record r : records.values()) {
            if (owner.equals(r.owner)) {
                out.add(r);
            }
        }
        out.sort(Comparator.comparingLong(r -> r.createdAt));
        return out;
    }

    public OwnerMeta meta(UUID owner) {
        return owners.computeIfAbsent(owner, id -> {
            OwnerMeta m = new OwnerMeta();
            m.owner = id;
            return m;
        });
    }

    /** Companions counted by the world limit: alive, waiting on a grave or dismissed. */
    public long countForLimit() {
        return records.size();
    }

    public UUID legacyLaura() {
        return legacyLaura;
    }

    public void clearLegacy() {
        legacyLaura = null;
        setDirty();
    }

    public static LauraWorldData load(CompoundTag tag) {
        LauraWorldData data = new LauraWorldData();
        ListTag list = tag.getListOrEmpty("Lauras");
        for (int i = 0; i < list.size(); i++) {
            try {
                Record r = Record.load(list.getCompoundOrEmpty(i));
                data.records.put(r.laura, r);
            } catch (RuntimeException ignored) {
                // A broken record is skipped rather than breaking the world.
            }
        }
        ListTag metas = tag.getListOrEmpty("Owners");
        for (int i = 0; i < metas.size(); i++) {
            try {
                OwnerMeta m = OwnerMeta.load(metas.getCompoundOrEmpty(i));
                data.owners.put(m.owner, m);
            } catch (RuntimeException ignored) {
                // Same.
            }
        }
        ListTag removedList = tag.getListOrEmpty("Removed");
        for (int i = 0; i < removedList.size(); i++) {
            try {
                data.removed.add(UUID.fromString(removedList.getStringOr(i, "")));
            } catch (IllegalArgumentException ignored) {
                // Not a UUID: skipped.
            }
        }
        if (tag.read("LauraUUID", UUIDUtil.CODEC).isPresent() && tag.getBooleanOr("Exists", false)) {
            data.legacyLaura = tag.read("LauraUUID", UUIDUtil.CODEC).orElse(null);
        } else {
            data.legacyLaura = tag.read("LegacyLaura", UUIDUtil.CODEC).orElse(null);
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Record r : records.values()) {
            list.add(r.save());
        }
        tag.put("Lauras", list);
        ListTag metas = new ListTag();
        for (OwnerMeta m : owners.values()) {
            metas.add(m.save());
        }
        tag.put("Owners", metas);
        ListTag removedList = new ListTag();
        for (UUID id : removed) {
            removedList.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("Removed", removedList);
        if (legacyLaura != null) {
            tag.store("LegacyLaura", UUIDUtil.CODEC, legacyLaura);
        }
        tag.putInt("Version", 2);
        return tag;
    }
}
