package com.vyrriox.lauramod.world;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.ai.LauraMovement;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.network.LauraNetwork;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.skin.SkinService;
import com.vyrriox.lauramod.util.ChatButtons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * World-level management of every companion: summoning (several per player), finding and
 * selecting them, recalling them from unloaded chunks, graves and revival, following across
 * dimensions, and a small tick scheduler.
 *
 * @author vyrriox
 */
public final class LauraManager {
    private record Scheduled(long tick, Runnable task) {
    }

    /** {@code restore}: asked for by her partner, so a companion who is not found is brought back from her snapshot. */
    private record Recall(UUID owner, UUID laura, net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunk, boolean restore, long timeout) {
    }

    private static final List<Scheduled> SCHEDULED = new ArrayList<>();
    private static final List<Recall> RECALLS = new ArrayList<>();
    private static final long SNAPSHOT_INTERVAL = 20L * 60 * 5;
    /** How often a ticking companion refreshes her record (see {@link LauraEntity#tick}). */
    public static final int TRACK_INTERVAL = 20;
    private static final int RECALL_TICKS = 200;
    /** Chunks kept around the place a recalled companion was last seen: she is found even if she walked a little since. */
    private static final int RECALL_RADIUS = 2;
    /**
     * Loads the chunks of a recall. A ticket of this type expires by itself and is never saved with
     * the world, unlike a forced chunk: whatever happens to the recall, nothing stays loaded.
     */
    private static final TicketType<ChunkPos> RECALL_TICKET = TicketType.create("lauramod_recall", Comparator.comparingLong(ChunkPos::toLong), RECALL_TICKS + 100);

    private LauraManager() {
    }

    // ------------------------------------------------------------------ scheduler

    public static void schedule(MinecraftServer server, int delayTicks, Runnable task) {
        synchronized (SCHEDULED) {
            SCHEDULED.add(new Scheduled(server.getTickCount() + Math.max(1, delayTicks), task));
        }
    }

    public static void tick(MinecraftServer server) {
        List<Runnable> due = new ArrayList<>();
        synchronized (SCHEDULED) {
            Iterator<Scheduled> it = SCHEDULED.iterator();
            while (it.hasNext()) {
                Scheduled s = it.next();
                if (server.getTickCount() >= s.tick()) {
                    due.add(s.task());
                    it.remove();
                }
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (RuntimeException e) {
                LauraMod.LOGGER.error("Scheduled Laura task failed", e);
            }
        }
        if (server.getTickCount() % 10 == 0) {
            tickRecalls(server);
        }
        if (server.getTickCount() % 20 == 0) {
            tickTimedRespawns(server);
        }
        if (server.getTickCount() % 40 == 0) {
            tickLeftBehind(server);
        }
    }

    /** Far enough to be sure she did not just lag behind: a teleport (waystone, /home, /tp, pearl...). */
    private static final double LEFT_BEHIND_DISTANCE = 96;
    /** A portal sends her through at once and holds a player for a few seconds: she waits that long on the other side. */
    private static final int PORTAL_WAIT = 200;
    private static final Map<UUID, Integer> LAST_FOLLOW_RECALL = new HashMap<>();
    /** Server tick at which a companion last arrived in another dimension. */
    private static final Map<UUID, Integer> ARRIVED = new HashMap<>();

    /** In follow mode and free to move: where her partner goes, she goes. */
    private static boolean follows(LauraEntity laura) {
        return laura.getMode() == LauraMode.FOLLOW && !laura.isOrderedToSit() && !laura.isAsleep() && !laura.fetchGoal().isActive();
    }

    /** She just changed dimension, most likely through a portal her partner is still standing in. */
    private static boolean waitsForPartner(MinecraftServer server, UUID laura) {
        Integer arrived = ARRIVED.get(laura);
        if (arrived != null && server.getTickCount() - arrived >= PORTAL_WAIT) {
            ARRIVED.remove(laura);
            return false;
        }
        return arrived != null;
    }

    /**
     * Brings following companions along when their partner teleports far away by any means
     * (Waystones, homes, commands, ender pearls, other mods). Loaded companions are moved at once;
     * companions left in unloaded chunks are recalled.
     */
    private static void tickLeftBehind(MinecraftServer server) {
        LauraWorldData data = data(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            for (LauraWorldData.Record r : data.byOwner(player.getUUID())) {
                if (!r.isActive() || !r.following) {
                    continue;
                }
                Entity entity = findEntity(server, r.laura, r.dimension);
                if (entity instanceof LauraEntity laura) {
                    if (!follows(laura)) {
                        continue;
                    }
                    boolean elsewhere = laura.level() != player.level();
                    if (elsewhere && (!LauraConfig.followAcrossDimensions.get() || waitsForPartner(server, r.laura))) {
                        continue;
                    }
                    // Loaded and ticking: she walks unless further than teleportDistance. Loaded but
                    // frozen (outside the simulation distance): she cannot walk, so she is brought along.
                    double teleport = LauraConfig.teleportDistance.getDouble();
                    boolean frozen = laura.level() instanceof ServerLevel level && !level.isPositionEntityTicking(laura.blockPosition());
                    boolean far = elsewhere || laura.distanceToSqr(player) > teleport * teleport
                            || frozen && laura.distanceToSqr(player) > LEFT_BEHIND_DISTANCE * LEFT_BEHIND_DISTANCE;
                    if (far) {
                        // Null when her partner has no ground near him (he flies): the next pass tries again.
                        teleport(laura, player.serverLevel(), player.blockPosition());
                    }
                    continue;
                }
                boolean otherDimension = !r.dimension.equals(player.level().dimension());
                // The far side of a portal can take a moment to load: she is there, only not visible yet.
                if (otherDimension && (!LauraConfig.followAcrossDimensions.get() || waitsForPartner(server, r.laura))) {
                    continue;
                }
                if (entity != null) {
                    continue;
                }
                boolean far = otherDimension || r.pos.distSqr(player.blockPosition()) > LEFT_BEHIND_DISTANCE * LEFT_BEHIND_DISTANCE;
                Integer last = LAST_FOLLOW_RECALL.get(r.laura);
                boolean recently = last != null && server.getTickCount() - last < 20 * 60;
                boolean pending = RECALLS.stream().anyMatch(rc -> rc.laura().equals(r.laura));
                if (far && !recently && !pending) {
                    LAST_FOLLOW_RECALL.put(r.laura, server.getTickCount());
                    recall(player, r, false);
                }
            }
        }
    }

    /** Server start and stop: nothing of one world may leak into the next one opened in the same game session. */
    public static void clear() {
        synchronized (SCHEDULED) {
            SCHEDULED.clear();
        }
        RECALLS.clear();
        LAST_FOLLOW_RECALL.clear();
        ARRIVED.clear();
    }

    // ------------------------------------------------------------------ lookup

    public static LauraWorldData data(MinecraftServer server) {
        return LauraWorldData.get(server);
    }

    /** Loaded companions of a player, nearest first. */
    public static List<LauraEntity> findAll(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        List<LauraEntity> out = new ArrayList<>();
        if (server == null) {
            return out;
        }
        for (LauraWorldData.Record r : data(server).byOwner(owner.getUUID())) {
            if (!r.isActive()) {
                continue;
            }
            Entity e = findEntity(server, r.laura, r.dimension);
            if (e instanceof LauraEntity laura && laura.isAlive() && !laura.isRemoved()) {
                out.add(laura);
            }
        }
        for (LauraEntity laura : owner.serverLevel().getEntitiesOfClass(LauraEntity.class, owner.getBoundingBox().inflate(64), l -> l.isOwnedBy(owner))) {
            if (!out.contains(laura)) {
                track(laura);
                // Not the ones an administrator removed while they were not loaded: track just let them go.
                if (!laura.isRemoved()) {
                    out.add(laura);
                }
            }
        }
        out.sort(Comparator.comparingDouble(l -> l.level() == owner.level() ? l.distanceToSqr(owner) : Double.MAX_VALUE));
        return out;
    }

    /** The selected companion if she is loaded, otherwise the nearest one. */
    public static LauraEntity find(ServerPlayer owner) {
        List<LauraEntity> all = findAll(owner);
        if (all.isEmpty()) {
            return null;
        }
        UUID selected = data(owner.getServer()).meta(owner.getUUID()).selected;
        for (LauraEntity laura : all) {
            if (laura.getUUID().equals(selected)) {
                return laura;
            }
        }
        return all.get(0);
    }

    /** The selected companion if she is within range, otherwise the nearest one within range. */
    public static LauraEntity findNear(ServerPlayer owner, double range) {
        List<LauraEntity> all = findAll(owner);
        UUID selected = owner.getServer() == null ? null : data(owner.getServer()).meta(owner.getUUID()).selected;
        LauraEntity nearest = null;
        for (LauraEntity laura : all) {
            if (laura.level() != owner.level() || laura.distanceToSqr(owner) > range * range) {
                continue;
            }
            if (laura.getUUID().equals(selected)) {
                return laura;
            }
            if (nearest == null) {
                nearest = laura;
            }
        }
        return nearest;
    }

    public static LauraEntity findByName(ServerPlayer owner, String name) {
        for (LauraEntity laura : findAll(owner)) {
            if (laura.getLauraName().equalsIgnoreCase(name.trim())) {
                return laura;
            }
        }
        return null;
    }

    public static void select(ServerPlayer owner, LauraEntity laura) {
        LauraWorldData data = data(owner.getServer());
        data.meta(owner.getUUID()).selected = laura.getUUID();
        data.setDirty();
    }

    /**
     * The entity a companion is now. After a change of dimension the one at hand is removed and she
     * lives on as another entity: code that keeps giving her orders must go on with that one. Null
     * when she is gone or not loaded.
     */
    @Nullable
    public static LauraEntity live(LauraEntity laura) {
        if (!laura.isRemoved()) {
            return laura;
        }
        MinecraftServer server = laura.getServer();
        return server != null && findEntity(server, laura.getUUID(), laura.level().dimension()) instanceof LauraEntity now && now.isAlive() ? now : null;
    }

    private static Entity findEntity(MinecraftServer server, UUID id, net.minecraft.resources.ResourceKey<Level> hint) {
        ServerLevel level = server.getLevel(hint);
        if (level != null) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        for (ServerLevel other : server.getAllLevels()) {
            Entity e = other.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ registry upkeep

    /** Registers or refreshes a companion in the world registry. */
    public static void track(LauraEntity laura) {
        if (laura.isRemoved()) {
            return;
        }
        write(laura, false);
    }

    private static void write(LauraEntity laura, boolean snapshot) {
        if (!(laura.level() instanceof ServerLevel level) || laura.getOwnerUUID() == null || !laura.isAlive()) {
            return;
        }
        MinecraftServer server = level.getServer();
        LauraWorldData data = data(server);
        if (data.consumeRemoved(laura.getUUID())) {
            // Removed by an administrator while her chunk was not loaded: she leaves now, instead of
            // registering herself again.
            removeForGood(laura);
            return;
        }
        if (laura.getUUID().equals(data.legacyLaura())) {
            data.clearLegacy();
        }
        LauraWorldData.Record record = data.getOrCreate(laura.getUUID(), laura.getOwnerUUID());
        record.owner = laura.getOwnerUUID();
        record.dimension = level.dimension();
        record.pos = laura.blockPosition();
        record.lauraName = laura.getLauraName();
        record.following = laura.getMode() == LauraMode.FOLLOW && !laura.isOrderedToSit() && !laura.isAsleep();
        record.dismissed = false;
        record.dead = false;
        record.respawnAt = -1;
        ServerPlayer owner = server.getPlayerList().getPlayer(laura.getOwnerUUID());
        if (owner != null) {
            record.ownerName = owner.getGameProfile().getName();
        }
        if (snapshot || record.snapshot == null || level.getGameTime() % SNAPSHOT_INTERVAL < TRACK_INTERVAL) {
            record.snapshot = laura.saveWithoutId(new CompoundTag());
        }
        data.setDirty();
    }

    /** A second her must have been around this long before it is removed: a teleport by another mod may overlap for a moment. */
    private static final int TWIN_AGE = 100;

    /**
     * True when this entity is a second copy of a companion who lives in another dimension, and has
     * been removed. Travelling never leaves one: it is what a restore from her snapshot left when a
     * world saved by an older version had her record in the wrong place. The one that loaded last
     * goes, the one her partner has been with since then stays.
     */
    public static boolean removeTwin(LauraEntity laura) {
        if (laura.tickCount < TWIN_AGE || !(laura.level() instanceof ServerLevel level)) {
            return false;
        }
        for (ServerLevel other : level.getServer().getAllLevels()) {
            // Entity ids grow with every entity the server creates: the higher one came last.
            if (other != level && other.getEntity(laura.getUUID()) instanceof LauraEntity twin && twin.isAlive() && twin.getId() < laura.getId()) {
                LauraMod.LOGGER.warn("{} exists twice (in {} and in {}), removing the one that loaded last", laura.getLauraName(),
                        other.dimension().location(), level.dimension().location());
                laura.discard();
                return true;
            }
        }
        return false;
    }

    /** She was moved in one step (a goal teleported her): the place in her record follows at once. Never creates or revives a record. */
    public static void moved(LauraEntity laura) {
        LauraWorldData.Record record = activeRecord(laura);
        if (record != null && laura.level() instanceof ServerLevel level) {
            record.dimension = level.dimension();
            record.pos = laura.blockPosition();
            data(level.getServer()).setDirty();
        }
    }

    /** Her mode changed: whether she follows is what the left behind check reads. Never creates or revives a record. */
    public static void modeChanged(LauraEntity laura) {
        LauraWorldData.Record record = activeRecord(laura);
        if (record != null && laura.level() instanceof ServerLevel level) {
            record.following = laura.getMode() == LauraMode.FOLLOW && !laura.isOrderedToSit() && !laura.isAsleep();
            data(level.getServer()).setDirty();
        }
    }

    /** The record of a companion who is in the world (not one still being created from a snapshot). */
    private static LauraWorldData.Record activeRecord(LauraEntity laura) {
        if (!(laura.level() instanceof ServerLevel level) || laura.isRemoved() || !laura.isAlive() || level.getEntity(laura.getUUID()) != laura) {
            return null;
        }
        LauraWorldData.Record record = data(level.getServer()).get(laura.getUUID());
        return record != null && record.isActive() ? record : null;
    }

    /**
     * She arrived in another dimension, whatever sent her (a portal, a command, this mod): the game
     * replaced her with this new entity, and her record must say where she is before anything reads it.
     */
    public static void onChangedDimension(LauraEntity laura) {
        track(laura);
        if (laura.level() instanceof ServerLevel level) {
            ARRIVED.put(laura.getUUID(), level.getServer().getTickCount());
        }
    }

    /**
     * She leaves the loaded world with her chunk. The record keeps exactly where and how she is: a
     * recall then loads the right place, and a restore from her snapshot loses nothing.
     */
    public static void onUnloaded(LauraEntity laura) {
        Entity.RemovalReason reason = laura.getRemovalReason();
        // The game hides her with her chunk before it marks her as unloaded, so there is usually no
        // reason yet. A death, a discard or a change of dimension are recorded where they happen.
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            write(laura, true);
        }
    }

    // ------------------------------------------------------------------ summoning

    private static boolean underWorldLimit(LauraWorldData data) {
        int max = LauraConfig.maxPerWorld.getInt();
        return max <= 0 || data.countForLimit() < max;
    }

    /** Summons a new companion, or calls the existing ones when the player already has the maximum. */
    public static void summon(ServerPlayer player, boolean viaChat) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!player.hasPermissions(LauraConfig.summonPermissionLevel.getInt())) {
            player.sendSystemMessage(Component.translatable("lauramod.summon.no_permission"));
            return;
        }
        if (viaChat && !LauraConfig.chatSummon.get()) {
            return;
        }
        LauraWorldData data = data(server);
        List<LauraWorldData.Record> mine = data.byOwner(player.getUUID());
        // A dismissed companion comes back first.
        for (LauraWorldData.Record r : mine) {
            if (r.dismissed && r.snapshot != null) {
                if (!roomOrTell(player, player.blockPosition())) {
                    return;
                }
                r.dismissed = false;
                LauraEntity back = create(player, r.snapshot, player.blockPosition());
                if (back != null) {
                    LauraSpeech.say(back, player, "summon.back", LineFormatter.values());
                } else {
                    r.dismissed = true;
                }
                return;
            }
        }
        if (mine.size() >= LauraConfig.maxPerPlayer.getInt()) {
            if (viaChat) {
                // Proximity chat: far away companions cannot hear the call.
                LauraEntity nearest = findNear(player, LauraConfig.chatRange.getInt());
                if (nearest != null) {
                    LauraSpeech.say(nearest, player, "already_here", LineFormatter.values());
                } else {
                    player.sendSystemMessage(Component.translatable("lauramod.summon.max", LauraConfig.maxPerPlayer.getInt()));
                }
                return;
            }
            callEveryone(player, mine);
            return;
        }
        LauraWorldData.OwnerMeta meta = data.meta(player.getUUID());
        long now = System.currentTimeMillis();
        if (now - meta.lastSummon < LauraConfig.summonCooldownSeconds.getInt() * 1000L) {
            player.sendSystemMessage(Component.translatable("lauramod.summon.cooldown"));
            return;
        }
        if (!underWorldLimit(data)) {
            player.sendSystemMessage(Component.translatable("lauramod.summon.world_taken"));
            return;
        }
        if (!roomOrTell(player, player.blockPosition())) {
            return;
        }
        LauraEntity laura = create(player, null, player.blockPosition());
        if (laura == null) {
            return;
        }
        meta.lastSummon = now;
        meta.selected = laura.getUUID();
        data.setDirty();
        LauraSpeech.say(laura, player, mine.isEmpty() ? "summon" : "summon.another", LineFormatter.values());
        com.vyrriox.lauramod.api.LauraAPI.fire("summon", laura, player, "");
        offerRename(player, laura);
        LauraAdvancements.award(player, "root");
        if (!mine.isEmpty()) {
            LauraAdvancements.award(player, "harem");
        }
        if (mine.size() >= 2) {
            LauraAdvancements.award(player, "harem_3");
        }
    }

    /**
     * "Come" (command, call key, Laura's Heart) when none of the player's companions is loaded:
     * brings back the ones she has, wherever they are. A new companion is only created when she has
     * none at all; more companions are asked for with the summon command or the chat phrase.
     */
    public static void call(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        List<LauraWorldData.Record> mine = data(server).byOwner(player.getUUID());
        if (mine.isEmpty() || mine.stream().anyMatch(r -> r.dismissed && r.snapshot != null)) {
            // First companion, or a dismissed one coming back.
            summon(player, false);
            return;
        }
        callEveryone(player, mine);
    }

    /** Every companion of the player comes, the dead ones are reminded. */
    private static void callEveryone(ServerPlayer player, List<LauraWorldData.Record> mine) {
        boolean anyone = false;
        boolean waiting = false;
        for (LauraWorldData.Record r : mine) {
            if (r.dead) {
                player.sendSystemMessage(Component.translatable("lauramod.grave.waiting", r.lauraName).withStyle(ChatFormatting.GRAY));
                waiting = true;
                continue;
            }
            if (r.respawnAt >= 0) {
                long seconds = Math.max(1, (r.respawnAt - player.serverLevel().getGameTime()) / 20);
                player.sendSystemMessage(Component.translatable("lauramod.summon.respawning", r.lauraName, seconds));
                waiting = true;
                continue;
            }
            Entity e = findEntity(player.getServer(), r.laura, r.dimension);
            if (e instanceof LauraEntity laura) {
                if (laura.level() != player.level() || laura.distanceToSqr(player) > 16 * 16) {
                    if (teleport(laura, player.serverLevel(), player.blockPosition()) != null) {
                        anyone = true;
                    } else {
                        LauraSpeech.tell(laura, player, "stuck", LineFormatter.values());
                        waiting = true;
                    }
                }
            } else {
                recall(player, r, true);
                anyone = true;
            }
        }
        if (!anyone) {
            LauraEntity nearest = findNear(player, 16);
            if (nearest != null) {
                LauraSpeech.say(nearest, player, "already_here", LineFormatter.values());
            } else if (!waiting && mine.size() >= LauraConfig.maxPerPlayer.getInt()) {
                player.sendSystemMessage(Component.translatable("lauramod.summon.max", LauraConfig.maxPerPlayer.getInt()));
            }
        } else {
            player.sendSystemMessage(Component.translatable("lauramod.summon.everyone_coming"));
        }
    }

    /**
     * A companion restored from a snapshot keeps her UUID: her body may still be fading out (the
     * death animation lasts a second), in which case it is removed. Returns false if she is alive.
     */
    private static boolean clearOldBody(MinecraftServer server, LauraEntity fresh) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity old = level.getEntity(fresh.getUUID());
            if (old != null) {
                if (old.isAlive() && !old.isRemoved()) {
                    return false;
                }
                old.discard();
            }
        }
        return true;
    }

    /** Creates a companion next to a position, from a snapshot or brand new. */
    public static LauraEntity create(ServerPlayer player, CompoundTag snapshot, BlockPos at) {
        ServerLevel level = player.serverLevel();
        LauraEntity laura = LauraRegistries.LAURA.get().create(level);
        if (laura == null) {
            return null;
        }
        if (snapshot != null) {
            laura.load(snapshot);
            // A snapshot taken while she slept holds her bed: she comes back awake, and that bed is free.
            laura.wakeUp();
            laura.setHealth(laura.getMaxHealth());
            laura.deathTime = 0;
            laura.setRemainingFireTicks(0);
            laura.resetFallDistance();
            laura.brain().needs().set(Needs.Need.HUNGER, Math.max(50, laura.brain().needs().get(Needs.Need.HUNGER)));
        } else {
            bind(laura, player);
            initDefaults(laura, player);
        }
        laura.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
        // On the ground: under her partner when he is in the air. Over the void or lava there is
        // nowhere to put her down: she is not created, the caller decides what to tell and when to retry.
        BlockPos spot = LauraMovement.findSafeSpot(laura, at);
        if (spot == null) {
            return null;
        }
        laura.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot() + 180, 0);
        laura.setOrderedToSit(false);
        laura.setMode(LauraMode.FOLLOW);
        if (snapshot != null && !clearOldBody(player.getServer(), laura)) {
            LauraMod.LOGGER.warn("{} is still alive somewhere, not creating a copy", laura.getLauraName());
            return null;
        }
        if (!level.addFreshEntity(laura)) {
            LauraMod.LOGGER.warn("Could not add a Laura to the world (duplicate UUID)");
            return null;
        }
        level.sendParticles(ParticleTypes.END_ROD, laura.getX(), laura.getY() + 1, laura.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.HEART, laura.getX(), laura.getY() + 1.8, laura.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
        laura.playLauraSound(LauraRegistries.Sound.HAPPY, 1.0F, 1.0F);
        laura.playEmote(Emote.WAVE);
        track(laura);
        return laura;
    }

    /** Whether a companion can be put down next to this place (not over the void, not over lava). */
    private static boolean hasRoom(ServerPlayer player, BlockPos at) {
        LauraEntity probe = LauraRegistries.LAURA.get().create(player.serverLevel());
        if (probe == null) {
            return false;
        }
        probe.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        return LauraMovement.findSafeSpot(probe, at) != null;
    }

    /** Same check, and the player who asked for her is told why she does not come. */
    private static boolean roomOrTell(ServerPlayer player, BlockPos at) {
        if (hasRoom(player, at)) {
            return true;
        }
        player.sendSystemMessage(Component.translatable("lauramod.summon.no_room"));
        return false;
    }

    /** Clickable invitation to give a freshly summoned companion her own name. */
    public static void offerRename(ServerPlayer player, LauraEntity laura) {
        player.sendSystemMessage(Component.translatable("lauramod.rename.offer", laura.getLauraName()).withStyle(ChatFormatting.GRAY)
                .append(" ").append(ChatButtons.suggest(Component.translatable("lauramod.rename.button"), "/laura name ", ChatFormatting.LIGHT_PURPLE)));
    }

    public static void initDefaults(LauraEntity laura, ServerPlayer owner) {
        laura.setCustomName(Component.literal(nextName(owner)));
        SkinService.applyDefaultSkin(laura);
        SkinService.applyDefaultModel(laura);
        laura.setAffection(LauraConfig.startAffection.getInt());
        laura.setCombatMode(LauraConfig.defaultCombatMode.get());
        laura.setSummonGameTime(laura.level().getGameTime());
        laura.brain().resetForSummon();
        laura.setHealth(laura.getMaxHealth());
    }

    /** First name not already used by one of the player's companions. */
    private static String nextName(ServerPlayer owner) {
        List<String> used = new ArrayList<>();
        if (owner.getServer() != null) {
            for (LauraWorldData.Record r : data(owner.getServer()).byOwner(owner.getUUID())) {
                used.add(r.lauraName.toLowerCase(Locale.ROOT));
            }
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(LauraConfig.defaultName.get());
        candidates.addAll(LauraConfig.extraNames.get());
        for (String name : candidates) {
            if (!name.isBlank() && !used.contains(name.toLowerCase(Locale.ROOT))) {
                return name;
            }
        }
        return LauraConfig.defaultName.get() + " " + (used.size() + 1);
    }

    /**
     * Makes the player her owner. Not {@code tame(player)}: that fires the vanilla "tame an animal"
     * trigger, which would grant "Best Friends Forever" as if she were a pet.
     */
    private static void bind(LauraEntity laura, ServerPlayer player) {
        laura.setTame(true, true);
        laura.setOwnerUUID(player.getUUID());
    }

    /** A player right clicks an untamed Laura (spawn egg or /summon). */
    public static void claim(ServerPlayer player, LauraEntity laura) {
        LauraWorldData data = data(player.getServer());
        if (data.byOwner(player.getUUID()).size() >= LauraConfig.maxPerPlayer.getInt()) {
            player.sendSystemMessage(Component.translatable("lauramod.summon.max", LauraConfig.maxPerPlayer.getInt()));
            return;
        }
        if (!underWorldLimit(data)) {
            player.sendSystemMessage(Component.translatable("lauramod.summon.world_taken"));
            return;
        }
        bind(laura, player);
        initDefaults(laura, player);
        laura.setMode(LauraMode.FOLLOW);
        track(laura);
        select(player, laura);
        laura.hearts(6);
        LauraSpeech.say(laura, player, "summon", LineFormatter.values());
        offerRename(player, laura);
        LauraAdvancements.award(player, "root");
    }

    public static void dismiss(ServerPlayer player, LauraEntity laura) {
        LauraSpeech.say(laura, player, "dismiss", LineFormatter.values());
        // Being sent away hurts: she remembers it when she comes back.
        laura.brain().changeAffection(-LauraConfig.dismissAffectionPenalty.getInt());
        laura.brain().needs().add(Needs.Need.ATTENTION, -25);
        // Before the snapshot: removing an entity does not wake it up, her bed would stay occupied
        // for good and she would come back asleep.
        laura.wakeUp();
        LauraWorldData data = data(player.getServer());
        LauraWorldData.Record record = data.getOrCreate(laura.getUUID(), player.getUUID());
        record.snapshot = laura.saveWithoutId(new CompoundTag());
        record.dismissed = true;
        record.lauraName = laura.getLauraName();
        data.setDirty();
        if (laura.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, laura.getX(), laura.getY() + 1, laura.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        }
        laura.discard();
    }

    /** Forgets a companion for good (admin, or "release"). */
    public static void release(ServerPlayer player, LauraEntity laura) {
        String name = laura.getLauraName();
        if (laura.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, laura.getX(), laura.getY() + 1, laura.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        }
        data(player.getServer()).remove(laura.getUUID());
        removeForGood(laura);
        player.sendSystemMessage(Component.translatable("lauramod.release.done", name).withStyle(ChatFormatting.GRAY));
    }

    /** She leaves the world for good: her bed is freed and everything she carries stays on the ground. */
    private static void removeForGood(LauraEntity laura) {
        laura.wakeUp();
        laura.dropBelongings();
        LauraMod.platform().dropTrinkets(laura);
        laura.discard();
    }

    /**
     * Admin removal of every companion of a player. The loaded ones leave at once; the ones in
     * unloaded chunks are remembered and leave as soon as they are loaded (see {@link #track}),
     * instead of registering themselves again.
     */
    public static void removeAll(MinecraftServer server, ServerPlayer target) {
        LauraWorldData data = data(server);
        java.util.Set<UUID> gone = new java.util.HashSet<>();
        for (LauraEntity laura : findAll(target)) {
            gone.add(laura.getUUID());
            removeForGood(laura);
        }
        for (LauraWorldData.Record r : data.byOwner(target.getUUID())) {
            // Dismissed companions and the ones waiting on a grave only exist as their record.
            if (!gone.contains(r.laura) && r.isActive()) {
                Entity entity = findEntity(server, r.laura, r.dimension);
                if (entity == null) {
                    data.markRemoved(r.laura);
                } else if (entity instanceof LauraEntity laura && !laura.isRemoved()) {
                    removeForGood(laura);
                }
            }
            data.remove(r.laura);
        }
    }

    // ------------------------------------------------------------------ recall across unloaded chunks

    /**
     * Loads the place where a companion was last seen, so that she can be brought to her partner.
     * {@code restore}: her partner asked for her, a companion who is not found there comes back from
     * her last snapshot. An automatic recall never creates anything.
     */
    private static void recall(ServerPlayer player, LauraWorldData.Record record, boolean restore) {
        MinecraftServer server = player.getServer();
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null) {
            // Her dimension no longer exists: she cannot be anywhere else.
            restoreLost(player, record);
            return;
        }
        for (int i = 0; i < RECALLS.size(); i++) {
            Recall pending = RECALLS.get(i);
            if (pending.laura().equals(record.laura)) {
                // Already on her way: the same recall goes on, it only learns that she is now asked for.
                if (restore && !pending.restore()) {
                    RECALLS.set(i, new Recall(pending.owner(), pending.laura(), pending.dimension(), pending.chunk(), true, pending.timeout()));
                }
                return;
            }
        }
        ChunkPos chunk = new ChunkPos(record.pos);
        level.getChunkSource().addRegionTicket(RECALL_TICKET, chunk, RECALL_RADIUS, chunk);
        RECALLS.add(new Recall(player.getUUID(), record.laura, record.dimension, chunk, restore, server.getTickCount() + RECALL_TICKS));
        player.sendSystemMessage(Component.translatable("lauramod.summon.on_her_way", record.lauraName));
    }

    private static void release(MinecraftServer server, Recall recall) {
        ServerLevel level = server.getLevel(recall.dimension());
        if (level != null) {
            level.getChunkSource().removeRegionTicket(RECALL_TICKET, recall.chunk(), RECALL_RADIUS, recall.chunk());
        }
    }

    /** Recalls still waiting for their companion to load (read by the self tests). */
    public static int pendingRecalls() {
        return RECALLS.size();
    }

    private static void tickRecalls(MinecraftServer server) {
        Iterator<Recall> it = RECALLS.iterator();
        while (it.hasNext()) {
            Recall recall = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(recall.owner());
            LauraEntity laura = findEntity(server, recall.laura(), recall.dimension()) instanceof LauraEntity found && found.isAlive() ? found : null;
            boolean timeout = server.getTickCount() > recall.timeout();
            if (laura == null && !timeout && owner != null) {
                continue;
            }
            it.remove();
            release(server, recall);
            if (owner == null) {
                continue;
            }
            if (laura != null) {
                if (teleport(laura, owner.serverLevel(), owner.blockPosition()) == null && recall.restore()) {
                    LauraSpeech.tell(laura, owner, "stuck", LineFormatter.values());
                }
                continue;
            }
            LauraWorldData.Record record = data(server).get(recall.laura());
            if (record == null || !record.isActive()) {
                continue;
            }
            if (recall.restore()) {
                restoreLost(owner, record);
            } else {
                // Not where she was last seen. Nothing is created from an automatic recall: a copy made
                // from her snapshot while she exists elsewhere would be a second her. She is followed
                // again as soon as she loads and ticks.
                record.following = false;
                data(server).setDirty();
            }
        }
    }

    /** She could not be found: bring her back from her last snapshot. */
    private static void restoreLost(ServerPlayer player, LauraWorldData.Record record) {
        LauraMod.LOGGER.info("{} of {} could not be found, restoring her from her last snapshot", record.lauraName, player.getGameProfile().getName());
        if (record.snapshot == null) {
            data(player.getServer()).remove(record.laura);
            return;
        }
        if (!roomOrTell(player, player.blockPosition())) {
            return;
        }
        LauraEntity laura = create(player, record.snapshot, player.blockPosition());
        if (laura != null) {
            LauraSpeech.say(laura, player, "summon.lost", LineFormatter.values());
        }
    }

    // ------------------------------------------------------------------ teleport

    /**
     * Moves a companion to a safe spot next to a position, in any dimension, and returns her. After a
     * change of dimension that is a new entity: the one passed in is gone and must not be used again.
     * Returns null, and leaves her where she is, when there is nowhere safe near the position (a
     * partner who flies over the void) or when the trip was refused.
     */
    @Nullable
    public static LauraEntity teleport(LauraEntity laura, ServerLevel target, BlockPos pos) {
        BlockPos spot = LauraMovement.findSafeSpot(laura, target, pos);
        if (spot == null) {
            return null;
        }
        if (laura.level() == target && laura.blockPosition().closerThan(spot, 2)) {
            // Already there (the ground under a partner who hovers): no puff, no sound.
            return laura;
        }
        if (laura.isPassenger()) {
            laura.stopRiding();
        }
        laura.wakeUp();
        if (laura.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, laura.getX(), laura.getY() + 1, laura.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
        }
        LauraEntity moved = LauraMovement.place(laura, target, spot);
        if (moved == null) {
            return null;
        }
        track(moved);
        target.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.4F, 1.3F);
        return moved;
    }

    // ------------------------------------------------------------------ death, graves and revival

    public static boolean keepsBelongingsOnDeath() {
        return LauraConfig.reviveMode.get() != LauraConfig.ReviveMode.NONE;
    }

    public static void onDeath(LauraEntity laura) {
        com.vyrriox.lauramod.api.LauraAPI.fire("death", laura, LauraSpeech.owner(laura), "");
        if (!(laura.level() instanceof ServerLevel level) || laura.getOwnerUUID() == null) {
            return;
        }
        LauraWorldData data = data(level.getServer());
        LauraWorldData.Record record = data.getOrCreate(laura.getUUID(), laura.getOwnerUUID());
        // Her partner is told wherever he is: she may die at home while he is in another dimension.
        ServerPlayer owner = LauraSpeech.ownerAnywhere(laura);
        switch (LauraConfig.reviveMode.get()) {
            case GRAVE -> {
                record.snapshot = laura.saveWithoutId(new CompoundTag());
                record.dead = true;
                record.lauraName = laura.getLauraName();
                record.pos = laura.blockPosition();
                record.dimension = level.dimension();
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable("lauramod.grave.died", laura.getLauraName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    owner.sendSystemMessage(Component.translatable("lauramod.grave.how").withStyle(ChatFormatting.GRAY));
                }
            }
            case TIMER -> {
                record.snapshot = laura.saveWithoutId(new CompoundTag());
                record.respawnAt = level.getGameTime() + LauraConfig.respawnDelaySeconds.getInt() * 20L;
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable("lauramod.death.will_respawn", laura.getLauraName(), LauraConfig.respawnDelaySeconds.getInt()));
                }
            }
            case NONE -> data.remove(laura.getUUID());
        }
        data.setDirty();
    }

    public static boolean hasDeadCompanion(ServerPlayer player) {
        for (LauraWorldData.Record r : data(player.getServer()).byOwner(player.getUUID())) {
            if (r.dead) {
                return true;
            }
        }
        return false;
    }

    /** A flower was laid on a gravestone: the companion who died first comes back there. */
    public static boolean reviveAtGrave(ServerPlayer player, BlockPos grave) {
        LauraWorldData data = data(player.getServer());
        LauraWorldData.Record chosen = null;
        for (LauraWorldData.Record r : data.byOwner(player.getUUID())) {
            if (r.dead && r.snapshot != null) {
                chosen = r;
                break;
            }
        }
        if (chosen == null) {
            return false;
        }
        if (!roomOrTell(player, grave.above())) {
            return false;
        }
        chosen.dead = false;
        data.setDirty();
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, grave.getX() + 0.5, grave.getY() + 1.0, grave.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.02);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, grave.getX() + 0.5, grave.getY() + 1.2, grave.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, grave, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        LauraEntity laura = create(player, chosen.snapshot, grave.above());
        if (laura == null) {
            chosen.dead = true;
            return false;
        }
        laura.brain().makeHappy(600);
        laura.brain().changeAffection(20);
        laura.playEmote(Emote.HUG);
        LauraSpeech.say(laura, player, "revived", LineFormatter.values());
        com.vyrriox.lauramod.api.LauraAPI.fire("revive", laura, player, "");
        LauraAdvancements.award(player, "revive");
        return true;
    }

    private static void tickTimedRespawns(MinecraftServer server) {
        LauraWorldData data = data(server);
        long now = server.overworld().getGameTime();
        for (LauraWorldData.Record record : new ArrayList<>(data.all())) {
            if (record.respawnAt < 0 || now < record.respawnAt) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(record.owner);
            if (owner == null || !owner.isAlive()) {
                continue;
            }
            if (!hasRoom(owner, owner.blockPosition())) {
                // Her partner flies over the void or over lava: she waits, and comes back once he stands somewhere safe.
                record.respawnAt = now + 100;
                continue;
            }
            record.respawnAt = -1;
            LauraEntity laura = create(owner, record.snapshot, owner.blockPosition());
            if (laura != null) {
                LauraSpeech.say(laura, owner, "respawned", LineFormatter.values());
            }
            data.setDirty();
        }
    }

    // ------------------------------------------------------------------ player events

    public static void onPlayerJoin(ServerPlayer player) {
        LauraNetwork.sendSettings(player);
        MinecraftServer server = player.getServer();
        LauraWorldData.OwnerMeta meta = data(server).meta(player.getUUID());
        long away = meta.lastLogout <= 0 ? 0 : System.currentTimeMillis() - meta.lastLogout;
        schedule(server, 60, () -> {
            if (player.hasDisconnected() || !LauraConfig.greetOnJoin.get()) {
                return;
            }
            LauraEntity laura = findNear(player, 48);
            if (laura == null) {
                if (hasDeadCompanion(player)) {
                    player.sendSystemMessage(Component.translatable("lauramod.grave.reminder").withStyle(ChatFormatting.GRAY));
                }
                return;
            }
            long hours = away / 3_600_000L;
            if (hours >= 24) {
                LauraSpeech.say(laura, player, "welcome_back.days", LineFormatter.values().with("hours", hours));
                laura.brain().startSulking(20 * 60 * 3);
                laura.brain().changeAffection(-10);
            } else if (hours >= 2) {
                LauraSpeech.say(laura, player, "welcome_back.long", LineFormatter.values().with("hours", hours));
                laura.brain().changeAffection(-3);
            } else {
                LauraSpeech.say(laura, player, "welcome_back", LineFormatter.values());
            }
            laura.playEmote(Emote.WAVE);
        });
    }

    public static void onPlayerLeave(ServerPlayer player) {
        LauraNetwork.forget(player);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        LauraWorldData data = data(server);
        data.meta(player.getUUID()).lastLogout = System.currentTimeMillis();
        data.setDirty();
        RECALLS.removeIf(r -> {
            if (!r.owner().equals(player.getUUID())) {
                return false;
            }
            release(server, r);
            return true;
        });
    }

    /**
     * Her partner changed dimension: the companions who follow him come along in the same tick. They
     * are still loaded where he left them, a second later their chunks are gone and so is every
     * reference to them. The ones who are not loaded are recalled by the left behind check, and what
     * she says on arrival and the advancement come from her own brain once they are together.
     */
    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel from) {
        if (!LauraConfig.followAcrossDimensions.get()) {
            return;
        }
        MinecraftServer server = player.getServer();
        for (LauraWorldData.Record r : data(server).byOwner(player.getUUID())) {
            if (!r.isActive()) {
                continue;
            }
            Entity e = findEntity(server, r.laura, from.dimension());
            if (e instanceof LauraEntity laura && laura.isAlive() && laura.level() != player.level() && follows(laura)) {
                teleport(laura, player.serverLevel(), player.blockPosition());
            }
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (!LauraConfig.keepOwnerItemsOnDeath.get() || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        LauraEntity laura = findNear(player, 32);
        if (laura == null) {
            return;
        }
        BlockPos deathPos = player.blockPosition();
        MinecraftServer server = player.getServer();
        schedule(server, 3, () -> {
            if (!laura.isAlive()) {
                return;
            }
            int stacks = 0;
            for (ItemEntity item : laura.level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(deathPos).inflate(6),
                    e -> e.isAlive() && e.getAge() < 60)) {
                if (laura.keptOwnerItems().size() >= 200) {
                    break;
                }
                laura.keptOwnerItems().add(item.getItem().copy());
                item.discard();
                stacks++;
            }
            laura.playEmote(Emote.CRY);
            ServerPlayer owner = server.getPlayerList().getPlayer(player.getUUID());
            if (owner != null) {
                LauraSpeech.say(laura, owner, stacks > 0 ? "owner.died.items" : "owner.died", LineFormatter.values().with("count", stacks));
            }
        });
    }

    /** Where are they? Lists every companion of the player. */
    public static void where(ServerPlayer player) {
        List<LauraWorldData.Record> mine = data(player.getServer()).byOwner(player.getUUID());
        if (mine.isEmpty()) {
            player.sendSystemMessage(Component.translatable("lauramod.not_found"));
            return;
        }
        for (LauraWorldData.Record r : mine) {
            Entity e = findEntity(player.getServer(), r.laura, r.dimension);
            BlockPos p = e != null ? e.blockPosition() : r.pos;
            String dim = e != null ? e.level().dimension().location().toString() : r.dimension.location().toString();
            String state = r.dead ? "lauramod.state.grave" : r.dismissed ? "lauramod.state.dismissed" : r.respawnAt >= 0 ? "lauramod.state.respawning"
                    : e != null ? "lauramod.state.here" : "lauramod.state.unloaded";
            player.sendSystemMessage(Component.translatable("lauramod.where.entry", r.lauraName, p.getX(), p.getY(), p.getZ(), dim, Component.translatable(state)));
        }
        LauraEntity near = findNear(player, 48);
        if (near != null) {
            LauraSpeech.say(near, player, "where", LineFormatter.values());
        }
    }

    /** Clickable list of the player's companions (for /laura list). */
    public static void list(ServerPlayer player) {
        List<LauraWorldData.Record> mine = data(player.getServer()).byOwner(player.getUUID());
        if (mine.isEmpty()) {
            player.sendSystemMessage(Component.translatable("lauramod.not_found"));
            return;
        }
        UUID selected = data(player.getServer()).meta(player.getUUID()).selected;
        for (LauraWorldData.Record r : mine) {
            boolean isSelected = r.laura.equals(selected);
            player.sendSystemMessage(Component.literal(isSelected ? "> " : "  ").append(Component.literal(r.lauraName).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(" ").append(ChatButtons.suggest(Component.translatable("lauramod.list.select"), "/laura select " + r.lauraName, ChatFormatting.GRAY)));
        }
    }
}
