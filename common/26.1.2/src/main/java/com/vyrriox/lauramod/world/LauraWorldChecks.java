package com.vyrriox.lauramod.world;

import com.vyrriox.lauramod.desire.DesireTable;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * World queries used by her needs and desires. Kept in one place because several of them use
 * vanilla APIs that change between Minecraft versions.
 *
 * @author vyrriox
 */
public final class LauraWorldChecks {
    public enum Climate {
        COLD, MILD, HOT
    }

    private static final Map<LauraEntity, long[]> MUSIC_CACHE = new WeakHashMap<>();

    private LauraWorldChecks() {
    }

    public static boolean isNight(Level level) {
        long time = level.getOverworldClockTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    public static boolean isSunset(Level level) {
        long time = level.getOverworldClockTime() % 24000L;
        return time >= 11800L && time < 13300L && !level.isRaining();
    }

    public static boolean canSeeSky(LauraEntity laura) {
        return laura.level().canSeeSky(laura.blockPosition().above());
    }

    /** True when a jukebox plays music within 12 blocks. Cached for 3 seconds. */
    public static boolean isMusicPlaying(LauraEntity laura) {
        long now = laura.level().getGameTime();
        long[] cached = MUSIC_CACHE.get(laura);
        if (cached != null && now - cached[0] < 60) {
            return cached[1] != 0;
        }
        boolean playing = findPlayingJukebox(laura, 12) != null;
        MUSIC_CACHE.put(laura, new long[]{now, playing ? 1 : 0});
        return playing;
    }

    public static BlockPos findPlayingJukebox(LauraEntity laura, int radius) {
        Level level = laura.level();
        BlockPos center = laura.blockPosition();
        ChunkPos min = ChunkPos.containing(center.offset(-radius, 0, -radius));
        ChunkPos max = ChunkPos.containing(center.offset(radius, 0, radius));
        for (int cx = min.x(); cx <= max.x(); cx++) {
            for (int cz = min.z(); cz <= max.z(); cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (e.getValue() instanceof JukeboxBlockEntity jukebox && e.getKey().distSqr(center) <= radius * radius) {
                        BlockState state = jukebox.getBlockState();
                        if (state.hasProperty(JukeboxBlock.HAS_RECORD) && state.getValue(JukeboxBlock.HAS_RECORD) && jukebox.getSongPlayer().isPlaying()) {
                            return e.getKey();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isNearLitCampfire(LauraEntity laura) {
        Level level = laura.level();
        for (BlockPos pos : BlockPos.betweenClosed(laura.blockPosition().offset(-4, -2, -4), laura.blockPosition().offset(4, 2, 4))) {
            if (CampfireBlock.isLitCampfire(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    public static boolean ownerHasPetNear(LauraEntity laura, ServerPlayer owner) {
        return !laura.level().getEntitiesOfClass(TamableAnimal.class, laura.getBoundingBox().inflate(8),
                pet -> pet != laura && !(pet instanceof LauraEntity) && pet.isTame() && pet.isOwnedBy(owner)).isEmpty();
    }

    public static Climate climate(LauraEntity laura) {
        Level level = laura.level();
        if (level.dimension() == Level.NETHER) {
            return Climate.HOT;
        }
        Holder<Biome> biome = level.getBiome(laura.blockPosition());
        if (biome.value().coldEnoughToSnow(laura.blockPosition(), level.getSeaLevel())) {
            return Climate.COLD;
        }
        return biome.value().getBaseTemperature() >= 1.5F ? Climate.HOT : Climate.MILD;
    }

    /** True if she stands in one of the biomes (or dimensions) of the place. */
    public static boolean isInPlace(LauraEntity laura, DesireTable.PlaceDesire place) {
        if (place == null) {
            return false;
        }
        Level level = laura.level();
        Holder<Biome> biome = level.getBiome(laura.blockPosition());
        for (String target : place.targets()) {
            String t = target.trim();
            if (t.startsWith("dimension:")) {
                Identifier dim = Identifier.tryParse(t.substring("dimension:".length()));
                if (dim != null && level.dimension().identifier().equals(dim)) {
                    return true;
                }
            } else if (t.startsWith("#")) {
                Identifier tag = Identifier.tryParse(t.substring(1));
                if (tag != null && biome.is(TagKey.create(Registries.BIOME, tag))) {
                    return true;
                }
            } else {
                Identifier id = Identifier.tryParse(t);
                if (id != null && biome.is(ResourceKey.create(Registries.BIOME, id))) {
                    return true;
                }
            }
        }
        return false;
    }
}
