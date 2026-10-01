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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private record Recall(UUID owner, UUID laura, net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunk, boolean forcedByUs, long timeout) {
    }

    private static final List<Scheduled> SCHEDULED = new ArrayList<>();
    private static final List<Recall> RECALLS = new ArrayList<>();
    private static final long SNAPSHOT_INTERVAL = 20L * 60 * 5;

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
    private static final Map<UUID, Integer> LAST_FOLLOW_RECALL = new HashMap<>();

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
                boolean otherDimension = !r.dimension.equals(player.level().dimension());
                if (otherDimension && !LauraConfig.followAcrossDimensions.get()) {
                    continue;
                }
                Entity entity = findEntity(server, r.laura, r.dimension);
                if (entity instanceof LauraEntity laura) {
                    if (laura.getMode() != LauraMode.FOLLOW || laura.isOrderedToSit() || laura.isAsleep() || laura.fetchGoal().isActive()) {
                        continue;
                    }
                    // Loaded and ticking: she walks unless further than teleportDistance. Loaded but
                    // frozen (outside the simulation distance): she cannot walk, so she is brought along.
                    double teleport = LauraConfig.teleportDistance.getDouble();
                    boolean frozen = laura.level() instanceof ServerLevel level && !level.isPositionEntityTicking(laura.blockPosition());
                    boolean far = laura.level() != player.level() || laura.distanceToSqr(player) > teleport * teleport
                            || frozen && laura.distanceToSqr(player) > LEFT_BEHIND_DISTANCE * LEFT_BEHIND_DISTANCE;
                    if (far) {
                        teleport(laura, player.level(), player.blockPosition());
                    }
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
                    recall(player, r);
                }
            }
        }
    }

    public static void clear() {
        synchronized (SCHEDULED) {
            SCHEDULED.clear();
        }
        RECALLS.clear();
    }

    // ------------------------------------------------------------------ lookup

    public static LauraWorldData data(MinecraftServer server) {
        return LauraWorldData.get(server);
    }

    /** Loaded companions of a player, nearest first. */
    public static List<LauraEntity> findAll(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
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
        for (LauraEntity laura : owner.level().getEntitiesOfClass(LauraEntity.class, owner.getBoundingBox().inflate(64), l -> l.isOwnedBy(owner))) {
            if (!out.contains(laura)) {
                track(laura);
                out.add(laura);
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
        UUID selected = data(owner.level().getServer()).meta(owner.getUUID()).selected;
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
        UUID selected = owner.level().getServer() == null ? null : data(owner.level().getServer()).meta(owner.getUUID()).selected;
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
        LauraWorldData data = data(owner.level().getServer());
        data.meta(owner.getUUID()).selected = laura.getUUID();
        data.setDirty();
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
        if (!(laura.level() instanceof ServerLevel level) || laura.getOwnerUUID() == null || laura.isRemoved() || !laura.isAlive()) {
            return;
        }
        MinecraftServer server = level.getServer();
        LauraWorldData data = data(server);
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
            record.ownerName = owner.getGameProfile().name();
        }
        if (record.snapshot == null || level.getGameTime() % SNAPSHOT_INTERVAL < 100) {
            record.snapshot = laura.saveWithoutId(new CompoundTag());
        }
        data.setDirty();
    }

    // ------------------------------------------------------------------ summoning

    private static boolean underWorldLimit(LauraWorldData data) {
        int max = LauraConfig.maxPerWorld.getInt();
        return max <= 0 || data.countForLimit() < max;
    }

    /** Summons a new companion, or calls the existing ones when the player already has the maximum. */
    public static void summon(ServerPlayer player, boolean viaChat) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        if (!player.permissions().hasPermission(LauraMod.permission(LauraConfig.summonPermissionLevel.getInt()))) {
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
                r.dismissed = false;
                LauraEntity back = create(player, r.snapshot, player.blockPosition());
                if (back != null) {
                    LauraSpeech.say(back, player, "summon.back", LineFormatter.values());
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
        MinecraftServer server = player.level().getServer();
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
                long seconds = Math.max(1, (r.respawnAt - player.level().getGameTime()) / 20);
                player.sendSystemMessage(Component.translatable("lauramod.summon.respawning", r.lauraName, seconds));
                waiting = true;
                continue;
            }
            Entity e = findEntity(player.level().getServer(), r.laura, r.dimension);
            if (e instanceof LauraEntity laura) {
                if (laura.level() != player.level() || laura.distanceToSqr(player) > 16 * 16) {
                    teleport(laura, player.level(), player.blockPosition());
                    anyone = true;
                }
            } else {
                recall(player, r);
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
        ServerLevel level = player.level();
        LauraEntity laura = LauraRegistries.LAURA.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (laura == null) {
            return null;
        }
        if (snapshot != null) {
            laura.load(snapshot);
            laura.setHealth(laura.getMaxHealth());
            laura.deathTime = 0;
            laura.setRemainingFireTicks(0);
            laura.brain().needs().set(Needs.Need.HUNGER, Math.max(50, laura.brain().needs().get(Needs.Need.HUNGER)));
        } else {
            bind(laura, player);
            initDefaults(laura, player);
        }
        laura.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
        LauraMovement.teleportNear(laura, at);
        laura.setOrderedToSit(false);
        laura.setMode(LauraMode.FOLLOW);
        if (snapshot != null && !clearOldBody(player.level().getServer(), laura)) {
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
        if (owner.level().getServer() != null) {
            for (LauraWorldData.Record r : data(owner.level().getServer()).byOwner(owner.getUUID())) {
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
        LauraWorldData data = data(player.level().getServer());
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
        LauraWorldData data = data(player.level().getServer());
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
        laura.dropBelongings();
        if (laura.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, laura.getX(), laura.getY() + 1, laura.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        }
        data(player.level().getServer()).remove(laura.getUUID());
        laura.discard();
        player.sendSystemMessage(Component.translatable("lauramod.release.done", name).withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ recall across unloaded chunks

    private static void recall(ServerPlayer player, LauraWorldData.Record record) {
        MinecraftServer server = player.level().getServer();
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null) {
            restoreLost(player, record);
            return;
        }
        ChunkPos chunk = ChunkPos.containing(record.pos);
        boolean alreadyForced = level.getForceLoadedChunks().contains(chunk.pack());
        if (!alreadyForced) {
            level.setChunkForced(chunk.x(), chunk.z(), true);
        }
        RECALLS.removeIf(r -> r.laura().equals(record.laura));
        RECALLS.add(new Recall(player.getUUID(), record.laura, record.dimension, chunk, !alreadyForced, server.getTickCount() + 200));
        player.sendSystemMessage(Component.translatable("lauramod.summon.on_her_way", record.lauraName));
    }

    private static void tickRecalls(MinecraftServer server) {
        Iterator<Recall> it = RECALLS.iterator();
        while (it.hasNext()) {
            Recall recall = it.next();
            ServerLevel level = server.getLevel(recall.dimension());
            ServerPlayer owner = server.getPlayerList().getPlayer(recall.owner());
            Entity entity = level == null ? null : level.getEntity(recall.laura());
            boolean timeout = server.getTickCount() > recall.timeout();
            if (!(entity instanceof LauraEntity) && !timeout && owner != null) {
                continue;
            }
            it.remove();
            if (level != null && recall.forcedByUs()) {
                level.setChunkForced(recall.chunk().x(), recall.chunk().z(), false);
            }
            if (owner == null) {
                continue;
            }
            if (entity instanceof LauraEntity laura) {
                teleport(laura, owner.level(), owner.blockPosition());
            } else {
                LauraWorldData.Record record = data(server).get(recall.laura());
                if (record != null && record.isActive()) {
                    restoreLost(owner, record);
                }
            }
        }
    }

    /** She could not be found: bring her back from her last snapshot. */
    private static void restoreLost(ServerPlayer player, LauraWorldData.Record record) {
        LauraMod.LOGGER.info("{} of {} could not be found, restoring her from her last snapshot", record.lauraName, player.getGameProfile().name());
        if (record.snapshot == null) {
            data(player.level().getServer()).remove(record.laura);
            return;
        }
        LauraEntity laura = create(player, record.snapshot, player.blockPosition());
        if (laura != null) {
            LauraSpeech.say(laura, player, "summon.lost", LineFormatter.values());
        }
    }

    // ------------------------------------------------------------------ teleport

    /** Moves a companion next to a position, in any dimension. */
    public static void teleport(LauraEntity laura, ServerLevel target, BlockPos pos) {
        if (laura.isPassenger()) {
            laura.stopRiding();
        }
        laura.wakeUp();
        if (laura.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, laura.getX(), laura.getY() + 1, laura.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
        }
        if (laura.level() == target) {
            if (!LauraMovement.teleportNear(laura, pos)) {
                laura.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, laura.getYRot(), laura.getXRot());
            }
        } else {
            laura.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), laura.getYRot(), laura.getXRot(), false);
        }
        target.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.4F, 1.3F);
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
        ServerPlayer owner = LauraSpeech.owner(laura);
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
        for (LauraWorldData.Record r : data(player.level().getServer()).byOwner(player.getUUID())) {
            if (r.dead) {
                return true;
            }
        }
        return false;
    }

    /** A flower was laid on a gravestone: the companion who died first comes back there. */
    public static boolean reviveAtGrave(ServerPlayer player, BlockPos grave) {
        LauraWorldData data = data(player.level().getServer());
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
        chosen.dead = false;
        data.setDirty();
        ServerLevel level = player.level();
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
        MinecraftServer server = player.level().getServer();
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
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        LauraWorldData data = data(server);
        data.meta(player.getUUID()).lastLogout = System.currentTimeMillis();
        data.setDirty();
        RECALLS.removeIf(r -> r.owner().equals(player.getUUID()));
    }

    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel from) {
        if (!LauraConfig.followAcrossDimensions.get()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        List<LauraEntity> followers = new ArrayList<>();
        for (LauraWorldData.Record r : data(server).byOwner(player.getUUID())) {
            Entity e = from.getEntity(r.laura);
            if (e instanceof LauraEntity laura && laura.getMode() == LauraMode.FOLLOW && !laura.brain().isSulking() && !laura.isFetching()) {
                followers.add(laura);
            }
        }
        if (followers.isEmpty()) {
            return;
        }
        schedule(server, 20, () -> {
            if (player.hasDisconnected()) {
                return;
            }
            for (LauraEntity laura : followers) {
                if (laura.isAlive() && !laura.isRemoved()) {
                    teleport(laura, player.level(), player.blockPosition());
                }
            }
            schedule(server, 20, () -> {
                LauraEntity moved = findNear(player, 16);
                if (moved != null) {
                    String key = player.level().dimension() == Level.NETHER ? "dimension.nether"
                            : player.level().dimension() == Level.END ? "dimension.end" : "dimension.overworld";
                    LauraSpeech.say(moved, player, key, LineFormatter.values());
                    if (!key.equals("dimension.overworld")) {
                        LauraAdvancements.award(player, key.substring("dimension.".length()));
                    }
                }
            });
        });
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (!LauraConfig.keepOwnerItemsOnDeath.get() || player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        LauraEntity laura = findNear(player, 32);
        if (laura == null) {
            return;
        }
        BlockPos deathPos = player.blockPosition();
        MinecraftServer server = player.level().getServer();
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
        List<LauraWorldData.Record> mine = data(player.level().getServer()).byOwner(player.getUUID());
        if (mine.isEmpty()) {
            player.sendSystemMessage(Component.translatable("lauramod.not_found"));
            return;
        }
        for (LauraWorldData.Record r : mine) {
            Entity e = findEntity(player.level().getServer(), r.laura, r.dimension);
            BlockPos p = e != null ? e.blockPosition() : r.pos;
            String dim = e != null ? e.level().dimension().identifier().toString() : r.dimension.identifier().toString();
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
        List<LauraWorldData.Record> mine = data(player.level().getServer()).byOwner(player.getUUID());
        if (mine.isEmpty()) {
            player.sendSystemMessage(Component.translatable("lauramod.not_found"));
            return;
        }
        UUID selected = data(player.level().getServer()).meta(player.getUUID()).selected;
        for (LauraWorldData.Record r : mine) {
            boolean isSelected = r.laura.equals(selected);
            player.sendSystemMessage(Component.literal(isSelected ? "> " : "  ").append(Component.literal(r.lauraName).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(" ").append(ChatButtons.suggest(Component.translatable("lauramod.list.select"), "/laura select " + r.lauraName, ChatFormatting.GRAY)));
        }
    }
}
