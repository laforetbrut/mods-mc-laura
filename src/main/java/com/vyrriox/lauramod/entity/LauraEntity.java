package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.world.LauraAdvancements;
import com.google.gson.JsonObject;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.entity.ai.FetchGoal;
import com.vyrriox.lauramod.entity.ai.LauraAttentionGoal;
import com.vyrriox.lauramod.entity.ai.LauraBathGoal;
import com.vyrriox.lauramod.entity.ai.LauraDanceGoal;
import com.vyrriox.lauramod.entity.ai.LauraEmoteGoal;
import com.vyrriox.lauramod.entity.ai.LauraFindFoodGoal;
import com.vyrriox.lauramod.entity.ai.LauraFollowOwnerGoal;
import com.vyrriox.lauramod.entity.ai.LauraHomeGoal;
import com.vyrriox.lauramod.entity.ai.LauraPickupGoal;
import com.vyrriox.lauramod.entity.ai.LauraSitGoal;
import com.vyrriox.lauramod.entity.ai.LauraSleepGoal;
import com.vyrriox.lauramod.entity.ai.LauraStrollGoal;
import com.vyrriox.lauramod.entity.ai.LauraTargetGoals;
import com.vyrriox.lauramod.entity.ai.LauraWorkGoal;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.entity.work.LauraWorkplace;
import com.vyrriox.lauramod.entity.brain.LauraBrain;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.gift.GiftTable;
import com.vyrriox.lauramod.inventory.LauraInventoryMenu;
import com.vyrriox.lauramod.registry.LauraRegistries;
import com.vyrriox.lauramod.skin.SkinRef;
import com.vyrriox.lauramod.util.ItemSpec;
import com.vyrriox.lauramod.world.LauraActions;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

/**
 * Laura herself.
 * <p>
 * The entity keeps the synced state and the vanilla hooks; the personality (needs, mood, desires,
 * comments) lives in {@link LauraBrain}, orders in {@link LauraActions}.
 *
 * @author vyrriox
 */
public class LauraEntity extends TamableAnimal {
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SLIM = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_MODEL = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_MOOD = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_COMBAT = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_AFFECTION = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_NEEDS = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> DATA_THOUGHT = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_EMOTE = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EMOTE_TIME = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_CARRY = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_BACK = SynchedEntityData.defineId(LauraEntity.class, EntityDataSerializers.ITEM_STACK);

    public static final int STATE_SAD = 1;
    public static final int STATE_GAGGED = 1 << 1;
    public static final int STATE_DANCING = 1 << 2;
    public static final int STATE_FETCHING = 1 << 3;
    public static final int STATE_FLOOR_SLEEP = 1 << 4;
    public static final int STATE_WORRIED = 1 << 5;
    public static final int STATE_PICKUP = 1 << 6;

    private SimpleContainer inventory = new SimpleContainer(27);
    private final LauraBrain brain = new LauraBrain(this);
    private final LauraWorkplace workplace = new LauraWorkplace(this);
    private LauraWorkGoal workGoal;
    private final List<ItemStack> keptOwnerItems = new ArrayList<>();
    /** Items of a fetch that was cut short by a save, a death or a change of dimension; put in her bags on her next tick. */
    private final List<ItemStack> pendingFetchBag = new ArrayList<>();
    /** Saved items that no longer fit in her bag (inventoryRows was lowered): dropped on her first tick. */
    private final List<ItemStack> overflow = new ArrayList<>();
    /** Game time of the sleep order she is following, -1 when she sleeps on her own or is awake. */
    private long sleepOrderedAt = -1;
    private int lastInteractTick = -1;
    private java.util.UUID lastInteractPlayer;
    private BlockPos homePos;
    private ResourceKey<Level> homeDimension;
    private BlockPos wanderCenter;
    private int gagTicks;
    private int emoteEndTick;
    private String skinLabel = "";
    private long summonGameTime = -1;
    private FetchGoal fetchGoal;
    private final int tickOffset;
    private boolean tracked;
    /** Set on the copy the game makes of her in another dimension, until the world registry has been told. */
    private boolean arrived;

    public LauraEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(20);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
        resizeInventory();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 0.0);
    }

    /** The eye height of a player. Minecraft 1.20.1 has no eye height in the entity type builder. */
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.62F * dimensions.height / 1.8F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN, SkinRef.DEFAULT.serialize());
        this.entityData.define(DATA_SLIM, true);
        this.entityData.define(DATA_MODEL, "");
        this.entityData.define(DATA_STATE, (byte) 0);
        this.entityData.define(DATA_MOOD, (byte) Mood.NEUTRAL.ordinal());
        this.entityData.define(DATA_MODE, (byte) LauraMode.FOLLOW.ordinal());
        this.entityData.define(DATA_COMBAT, (byte) CombatMode.PASSIVE.ordinal());
        this.entityData.define(DATA_AFFECTION, 500);
        this.entityData.define(DATA_NEEDS, new Needs().pack());
        this.entityData.define(DATA_THOUGHT, "");
        this.entityData.define(DATA_EMOTE, 0);
        this.entityData.define(DATA_EMOTE_TIME, 0);
        this.entityData.define(DATA_CARRY, ItemStack.EMPTY);
        this.entityData.define(DATA_BACK, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        this.fetchGoal = new FetchGoal(this);
        this.workGoal = new LauraWorkGoal(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LauraSleepGoal(this));
        this.goalSelector.addGoal(1, new LauraSitGoal(this));
        this.goalSelector.addGoal(2, new LauraEmoteGoal(this));
        this.goalSelector.addGoal(2, new LauraFindFoodGoal(this));
        this.goalSelector.addGoal(3, this.fetchGoal);
        this.goalSelector.addGoal(3, this.workGoal);
        this.goalSelector.addGoal(3, new LauraTargetGoals.Melee(this));
        this.goalSelector.addGoal(4, new LauraTargetGoals.AvoidMonsters(this));
        this.goalSelector.addGoal(5, new LauraFollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new LauraHomeGoal(this));
        this.goalSelector.addGoal(6, new LauraAttentionGoal(this));
        this.goalSelector.addGoal(6, new LauraBathGoal(this));
        this.goalSelector.addGoal(6, new LauraDanceGoal(this));
        this.goalSelector.addGoal(7, new LauraPickupGoal(this));
        this.goalSelector.addGoal(8, new LauraStrollGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        LauraTargetGoals.register(this, this.targetSelector);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (!tracked) {
            // First tick of this entity (loaded, created, or just arrived from another dimension).
            tracked = true;
            if (!reportArrival()) {
                LauraManager.track(this);
            }
        }
        if (emoteEndTick > 0) {
            Emote emote = getEmote();
            int elapsed = emote.duration - (emoteEndTick - this.tickCount);
            EmoteEffects.tick(this, emote, elapsed);
            if (this.tickCount >= emoteEndTick) {
                emoteEndTick = 0;
                this.entityData.set(DATA_EMOTE, 0);
                setState(STATE_DANCING, false);
            }
        }
        if (gagTicks > 0 && --gagTicks == 0 && isGagged()) {
            LauraActions.ungag(null, this, true);
        }
        if (!overflow.isEmpty()) {
            for (ItemStack stack : overflow) {
                this.spawnAtLocation(stack);
            }
            overflow.clear();
        }
        if (!pendingFetchBag.isEmpty()) {
            // Not while loading: she has to be in the world to drop what does not fit.
            for (ItemStack stack : pendingFetchBag) {
                ItemStack rest = bags().add(stack);
                if (!rest.isEmpty()) {
                    this.spawnAtLocation(rest);
                }
            }
            pendingFetchBag.clear();
        }
        if ((this.tickCount + tickOffset) % 5 == 0) {
            LauraActions.processQueue(this);
            if (this.isRemoved()) {
                // A queued order sent her to another dimension: she goes on there, as another entity.
                return;
            }
        }
        if ((this.tickCount + tickOffset) % 20 == 0) {
            if (LauraManager.removeTwin(this)) {
                return;
            }
            applyConfigAttributes();
            brain.tickSecond();
            LauraManager.track(this);
        }
    }

    // ------------------------------------------------------------------ ownership and dimensions

    /**
     * By UUID. The vanilla check looks her owner up in her own level, so a partner who is in another
     * dimension would not be her owner and could not call her.
     */
    @Override
    public boolean isOwnedBy(LivingEntity entity) {
        return entity != null && entity.getUUID().equals(this.getOwnerUUID());
    }

    /**
     * Portals end here. The game replaces her with a copy in the other level: the world registry
     * learns it at once.
     */
    @Nullable
    @Override
    public Entity changeDimension(ServerLevel destination) {
        if (destination != this.level() && destination.getEntity(this.getUUID()) != null) {
            // A second her is already there (see LauraManager.removeTwin). The game would not add the
            // one who arrives, and she would be lost: she stays.
            return null;
        }
        Entity moved = super.changeDimension(destination);
        if (moved instanceof LauraEntity copy && copy != this) {
            copy.arrived = false;
            LauraManager.onChangedDimension(copy);
        }
        return moved;
    }

    /** Portals only (commands and calls do not ask): with followAcrossDimensions off she does not wander into another dimension alone. */
    @Override
    public boolean canChangeDimensions() {
        return LauraConfig.followAcrossDimensions.get() && super.canChangeDimensions();
    }

    /** The vanilla teleport to another level (/tp, /execute in) replaces her without saying by whom: {@link #moveToLevel} does the same and knows the copy. */
    @Override
    public boolean teleportTo(ServerLevel level, double x, double y, double z, Set<RelativeMovement> relativeMovements, float yRot, float xRot) {
        if (level == this.level()) {
            return super.teleportTo(level, x, y, z, relativeMovements, yRot, xRot);
        }
        return moveToLevel(level, x, y, z, yRot, xRot) != null;
    }

    /**
     * Moves her to a place in another level, the way the game teleports an entity there: a copy of
     * her is made in that level and this entity is removed. Returns the copy, which the world
     * registry already knows about, or null (she is then unchanged) when the trip was refused:
     * another mod cancelled it, or a second her is already there (see {@link LauraManager#removeTwin})
     * and the game would not add the one who arrives.
     */
    @Nullable
    public LauraEntity moveToLevel(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        if (!(this.level() instanceof ServerLevel) || level == this.level() || this.isRemoved() || level.getEntity(this.getUUID()) != null) {
            return null;
        }
        // The game asks nobody before this kind of trip; the loader's portals do, and so does she.
        if (!LauraMod.platform().mayChangeDimension(this, level.dimension())) {
            return null;
        }
        if (!(this.getType().create(level) instanceof LauraEntity copy)) {
            return null;
        }
        this.unRide();
        copy.restoreFrom(this);
        copy.moveTo(x, y, z, yRot, Mth.clamp(xRot, -90.0F, 90.0F));
        copy.setYHeadRot(yRot);
        // She arrives standing still, whatever speed she left with (a fall, a push).
        copy.setDeltaMovement(Vec3.ZERO);
        this.removeAfterChangingDimensions();
        level.addDuringTeleport(copy);
        copy.arrived = false;
        LauraManager.onChangedDimension(copy);
        return copy;
    }

    /** The game fills the copy it makes of her in another level with this, whatever asked for the trip. */
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        this.arrived = true;
    }

    /**
     * A copy of her that was just made in another dimension tells the world registry where she is.
     * True when she was one and had not done it yet. The trips that go through the overrides above
     * do it themselves. The others are made by a teleporter of the loader (the portals of other
     * mods on Forge and NeoForge): the loader says when she joins her new level, and her first tick
     * is the last resort.
     */
    public boolean reportArrival() {
        if (!arrived) {
            return false;
        }
        arrived = false;
        LauraManager.onChangedDimension(this);
        return true;
    }

    private void applyConfigAttributes() {
        setBase(Attributes.MAX_HEALTH, LauraConfig.maxHealth.getDouble());
        setBase(Attributes.MOVEMENT_SPEED, LauraConfig.movementSpeed.getDouble());
        setBase(Attributes.ATTACK_DAMAGE, LauraConfig.attackDamage.getDouble());
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = this.level().isClientSide;
        // A name tag that reaches this point is an unnamed one (the game already used the named
        // ones to rename her): it is a gift or a wish like any other item.
        if (stack.is(Items.LEAD)) {
            return super.mobInteract(player, hand);
        }
        if (!this.isTame() || this.getOwnerUUID() == null) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                LauraManager.claim(serverPlayer, this);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (client) {
            // The client bridge checks ownership against the settings sent by the server.
            if (stack.isEmpty() && !player.isSecondaryUseActive()) {
                LauraMod.client().openInteractionScreen(this);
            }
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        // One interaction per player and per tick: a click that reaches the server twice must not
        // hand over two items (and make her speak twice).
        int serverTick = serverPlayer.getServer() == null ? this.tickCount : serverPlayer.getServer().getTickCount();
        if (lastInteractTick == serverTick && serverPlayer.getUUID().equals(lastInteractPlayer)) {
            return InteractionResult.CONSUME;
        }
        lastInteractTick = serverTick;
        lastInteractPlayer = serverPlayer.getUUID();
        if (!this.isOwnedBy(player) && !LauraConfig.othersCanInteract.get()) {
            LauraSpeech.refuse(this, serverPlayer);
            return InteractionResult.CONSUME;
        }
        if (LauraConfig.gagEnabled.get() && matchesAny(stack, LauraConfig.gagItems.get())) {
            LauraActions.gag(serverPlayer, this, stack);
            return InteractionResult.CONSUME;
        }
        if (isGagged() && (matchesAny(stack, LauraConfig.ungagItems.get()) || (stack.isEmpty() && player.isSecondaryUseActive()))) {
            LauraActions.ungag(serverPlayer, this, false);
            return InteractionResult.CONSUME;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            LauraActions.wash(serverPlayer, this, hand);
            return InteractionResult.CONSUME;
        }
        if (LauraBags.isWearableOnBack(stack) && getBackItem().isEmpty()
                || !stack.isEmpty() && GiftTable.food(stack) == null && GiftTable.find(stack) == null && LauraMod.platform().equipTrinket(this, stack.copyWithCount(1), true)) {
            LauraActions.giveItem(serverPlayer, this, stack, hand);
            return InteractionResult.CONSUME;
        }
        if (!stack.isEmpty() && (GiftTable.food(stack) != null || GiftTable.find(stack) != null || brain.wants(stack))) {
            LauraActions.giveItem(serverPlayer, this, stack, hand);
            return InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            openInventory(serverPlayer);
            return InteractionResult.CONSUME;
        }
        brain.onInteraction(serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static boolean matchesAny(ItemStack stack, List<String> specs) {
        if (stack.isEmpty()) {
            return false;
        }
        for (String spec : specs) {
            Optional<ItemSpec> parsed = ItemSpec.parse(spec);
            if (parsed.isPresent() && parsed.get().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public void openInventory(ServerPlayer player) {
        com.vyrriox.lauramod.network.LauraNetwork.sendMenuContext(player, this);
        player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> new LauraInventoryMenu(id, playerInventory, this),
                this.getDisplayName()));
    }

    // ------------------------------------------------------------------ damage and death

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (LauraConfig.invulnerable.get() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && !source.isCreativePlayer() && isShieldedFrom(living)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            if (isAsleep()) {
                wakeUp();
            }
            if (attacker instanceof ServerPlayer player) {
                if (this.isOwnedBy(player)) {
                    brain.onHitByOwner(player);
                } else {
                    brain.onHitByOther(player);
                }
            } else if (attacker instanceof LivingEntity living) {
                brain.onHurtByMob(living);
            }
        }
        return hurt;
    }

    // ------------------------------------------------------------------ players and their companions

    /** The player behind an entity: the player itself, or the owner of a companion or a pet. */
    @Nullable
    private static UUID playerBehind(LivingEntity entity) {
        if (entity instanceof Player) {
            return entity.getUUID();
        }
        return entity instanceof OwnableEntity owned ? owned.getOwnerUUID() : null;
    }

    /**
     * Whether the server lets her side and the side of {@code rival} hurt each other: the PvP
     * setting first, then the teams (friendly fire).
     */
    private boolean pvpAllowedWith(UUID rival, LivingEntity entity) {
        MinecraftServer server = this.level().getServer();
        if (server == null || !server.isPvpAllowed()) {
            return false;
        }
        Player rivalPlayer = entity instanceof Player p ? p : this.level().getPlayerByUUID(rival);
        if (rivalPlayer != null && this.getOwner() instanceof Player partner) {
            return partner.canHarmPlayer(rivalPlayer);
        }
        // One of the two players is away: the teams of the entities decide.
        return !this.isAlliedTo(entity);
    }

    /**
     * Whether she may fight this target. Never her partner, nor her partner's other companions
     * and pets. A player, or the companion or pet of another player, only when the config allows
     * it and the server lets the two players fight (PvP setting and teams).
     */
    public boolean mayFight(LivingEntity target) {
        if (target == this || this.isOwnedBy(target)) {
            return false;
        }
        UUID rival = playerBehind(target);
        if (rival == null) {
            return true;
        }
        if (rival.equals(this.getOwnerUUID())) {
            return false;
        }
        return LauraConfig.attackPlayers.get() && pvpAllowedWith(rival, target);
    }

    /**
     * The other side of {@link #mayFight}: where the server forbids fights between players, another
     * player (or their companion or pet) cannot hurt her either, since she could not answer.
     */
    private boolean isShieldedFrom(LivingEntity attacker) {
        if (!LauraConfig.shieldWithoutPvp.get() || this.getOwnerUUID() == null) {
            return false;
        }
        UUID rival = playerBehind(attacker);
        return rival != null && !rival.equals(this.getOwnerUUID()) && !pvpAllowedWith(rival, attacker);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return mayFight(target) && super.canAttack(target);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return mayFight(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living && !mayFight(living)) {
            // The rules changed during the fight (PvP turned off, teams changed).
            this.setTarget(null);
            return false;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.getOwnerUUID() != null && !this.isRemoved()) {
            // Recorded before the death animation so her snapshot still has her belongings.
            LauraManager.onDeath(this);
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        if (LauraManager.keepsBelongingsOnDeath() && this.getOwnerUUID() != null) {
            return;
        }
        dropBelongings();
    }

    /** Drops everything she carries at her feet: bag, equipment, items kept for her partner, back item. */
    public void dropBelongings() {
        if (fetchGoal != null) {
            // What she carries for a fetch goes back into her bag first, and is dropped with the rest.
            fetchGoal.cancel(false);
        }
        for (ItemStack stack : pendingFetchBag) {
            this.spawnAtLocation(stack);
        }
        pendingFetchBag.clear();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        for (ItemStack stack : keptOwnerItems) {
            this.spawnAtLocation(stack);
        }
        keptOwnerItems.clear();
        for (ItemStack stack : overflow) {
            this.spawnAtLocation(stack);
        }
        overflow.clear();
        if (!getBackItem().isEmpty()) {
            this.spawnAtLocation(getBackItem().copy());
            setBackItem(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    // ------------------------------------------------------------------ sounds

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (isAsleep()) {
            return null;
        }
        return isGagged() ? LauraRegistries.Sound.MUFFLED.get() : LauraRegistries.Sound.AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    public void playLauraSound(LauraRegistries.Sound sound, float volume, float pitch) {
        SoundEvent event = sound.get();
        if (event != null) {
            this.playSound(event, volume, pitch);
        }
    }

    // ------------------------------------------------------------------ name

    @Override
    public Component getName() {
        Component custom = this.getCustomName();
        return custom != null ? custom : Component.literal(LauraConfig.defaultName.get());
    }

    public String getLauraName() {
        return getName().getString();
    }

    // ------------------------------------------------------------------ state accessors

    public LauraBrain brain() {
        return brain;
    }

    /** Her bag plus the backpack she wears, as one storage. */
    public LauraBags bags() {
        return new LauraBags(this);
    }

    /** What she wears on her back (a backpack), EMPTY when nothing. */
    public ItemStack getBackItem() {
        return this.entityData.get(DATA_BACK);
    }

    public void setBackItem(ItemStack stack) {
        this.entityData.set(DATA_BACK, stack == null ? ItemStack.EMPTY : stack);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    public List<ItemStack> keptOwnerItems() {
        return keptOwnerItems;
    }

    public FetchGoal fetchGoal() {
        return fetchGoal;
    }

    public LauraWorkGoal workGoal() {
        return workGoal;
    }

    public LauraWorkplace workplace() {
        return workplace;
    }

    private void resizeInventory() {
        int size = Math.max(1, Math.min(6, LauraConfig.inventoryRows == null ? 3 : LauraConfig.inventoryRows.getInt())) * 9;
        if (inventory.getContainerSize() != size) {
            SimpleContainer resized = new SimpleContainer(size);
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (i < size) {
                    resized.setItem(i, stack);
                } else if (!stack.isEmpty() && !this.level().isClientSide) {
                    this.spawnAtLocation(stack);
                }
            }
            inventory = resized;
        }
    }

    public SkinRef getSkin() {
        return SkinRef.parse(this.entityData.get(DATA_SKIN));
    }

    public String getSkinRaw() {
        return this.entityData.get(DATA_SKIN);
    }

    public boolean isSlim() {
        return this.entityData.get(DATA_SLIM);
    }

    public void setSkin(SkinRef ref, boolean slim) {
        String old = this.entityData.get(DATA_SKIN);
        this.entityData.set(DATA_SKIN, ref.serialize());
        this.entityData.set(DATA_SLIM, slim);
        this.skinLabel = "";
        if (!old.equals(ref.serialize()) && !this.level().isClientSide) {
            brain.onLookChanged();
        }
    }

    public String getSkinLabel() {
        return skinLabel.isEmpty() ? getSkinRaw() : skinLabel;
    }

    public void setSkinLabel(String label) {
        this.skinLabel = label == null ? "" : label;
    }

    public String getModelName() {
        return this.entityData.get(DATA_MODEL);
    }

    public void setModel(String model) {
        String old = this.entityData.get(DATA_MODEL);
        String value = model == null ? "" : model;
        this.entityData.set(DATA_MODEL, value);
        if (!old.equals(value) && !this.level().isClientSide) {
            brain.onLookChanged();
        }
    }

    private boolean getState(int flag) {
        return (this.entityData.get(DATA_STATE) & flag) != 0;
    }

    public void setState(int flag, boolean value) {
        byte current = this.entityData.get(DATA_STATE);
        byte updated = (byte) (value ? current | flag : current & ~flag);
        if (updated != current) {
            this.entityData.set(DATA_STATE, updated);
        }
    }

    public boolean isSad() {
        return getState(STATE_SAD);
    }

    public boolean isGagged() {
        return getState(STATE_GAGGED);
    }

    public void setGagged(boolean gagged, int ticks) {
        setState(STATE_GAGGED, gagged);
        this.gagTicks = gagged ? ticks : 0;
    }

    public int gagTicksLeft() {
        return gagTicks;
    }

    public boolean isDancing() {
        return getState(STATE_DANCING);
    }

    public boolean isFetching() {
        return getState(STATE_FETCHING);
    }

    public boolean isWorried() {
        return getState(STATE_WORRIED);
    }

    public boolean isPickingUpItems() {
        return getState(STATE_PICKUP);
    }

    public void setPickingUpItems(boolean value) {
        setState(STATE_PICKUP, value);
    }

    public Mood getMood() {
        return Mood.byId(this.entityData.get(DATA_MOOD));
    }

    public void setMood(Mood mood) {
        if (getMood() != mood) {
            this.entityData.set(DATA_MOOD, (byte) mood.ordinal());
        }
        setState(STATE_SAD, mood == Mood.SAD || mood == Mood.SULKING);
    }

    public LauraMode getMode() {
        return LauraMode.byId(this.entityData.get(DATA_MODE));
    }

    public void setMode(LauraMode mode) {
        this.entityData.set(DATA_MODE, (byte) mode.ordinal());
        this.setOrderedToSit(mode == LauraMode.STAY);
        if (mode == LauraMode.WANDER) {
            this.wanderCenter = this.blockPosition();
        }
        this.getNavigation().stop();
        LauraManager.modeChanged(this);
    }

    public CombatMode getCombatMode() {
        return CombatMode.byId(this.entityData.get(DATA_COMBAT));
    }

    public void setCombatMode(CombatMode mode) {
        this.entityData.set(DATA_COMBAT, (byte) mode.ordinal());
        if (mode == CombatMode.PASSIVE) {
            this.setTarget(null);
        }
    }

    public int getAffection() {
        return this.entityData.get(DATA_AFFECTION);
    }

    public void setAffection(int affection) {
        this.entityData.set(DATA_AFFECTION, Math.max(0, Math.min(1000, affection)));
    }

    public long getPackedNeeds() {
        return this.entityData.get(DATA_NEEDS);
    }

    public void syncNeeds(long packed) {
        if (this.entityData.get(DATA_NEEDS) != packed) {
            this.entityData.set(DATA_NEEDS, packed);
        }
    }

    public String getThought() {
        return this.entityData.get(DATA_THOUGHT);
    }

    public void setThought(String thought) {
        String value = thought == null ? "" : thought;
        if (!this.entityData.get(DATA_THOUGHT).equals(value)) {
            this.entityData.set(DATA_THOUGHT, value);
        }
    }

    public ItemStack getCarried() {
        return this.entityData.get(DATA_CARRY);
    }

    public void setCarried(ItemStack stack) {
        this.entityData.set(DATA_CARRY, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public void setFetching(boolean fetching) {
        setState(STATE_FETCHING, fetching);
    }

    public void setWorried(boolean worried) {
        setState(STATE_WORRIED, worried);
    }

    public Emote getEmote() {
        return Emote.byId(this.entityData.get(DATA_EMOTE));
    }

    /** Game time (truncated to int) at which the current emote started. */
    public int getEmoteStart() {
        return this.entityData.get(DATA_EMOTE_TIME);
    }

    public void playEmote(Emote emote) {
        this.entityData.set(DATA_EMOTE, emote.ordinal());
        this.entityData.set(DATA_EMOTE_TIME, (int) this.level().getGameTime());
        this.emoteEndTick = emote == Emote.NONE ? 0 : this.tickCount + emote.duration;
        setState(STATE_DANCING, emote == Emote.DANCE);
        if (emote != Emote.NONE) {
            this.getNavigation().stop();
        }
    }

    public boolean isEmoting() {
        return emoteEndTick > 0 && this.tickCount < emoteEndTick;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public ResourceKey<Level> getHomeDimension() {
        return homeDimension;
    }

    public void setHome(BlockPos pos, ResourceKey<Level> dimension) {
        this.homePos = pos == null ? null : pos.immutable();
        this.homeDimension = pos == null ? null : dimension;
    }

    public BlockPos getWanderCenter() {
        return wanderCenter == null ? this.blockPosition() : wanderCenter;
    }

    public long getSummonGameTime() {
        return summonGameTime;
    }

    public void setSummonGameTime(long time) {
        this.summonGameTime = time;
    }

    /** Whole in-game days since she was summoned. */
    public int daysTogether() {
        if (summonGameTime < 0) {
            return 0;
        }
        return (int) Math.max(0, (this.level().getGameTime() - summonGameTime) / 24000L);
    }

    // ------------------------------------------------------------------ sleeping

    public boolean isAsleep() {
        return this.isSleeping() || getState(STATE_FLOOR_SLEEP);
    }

    /** Lies down in the given bed, or on the floor when {@code bed} is null. */
    public void goToSleep(@Nullable BlockPos bed) {
        this.getNavigation().stop();
        if (bed != null && this.level().getBlockState(bed).getBlock() instanceof BedBlock && !this.level().getBlockState(bed).getValue(BedBlock.OCCUPIED)) {
            this.startSleeping(bed);
            if (!this.level().isClientSide) {
                LauraAdvancements.award(LauraSpeech.owner(this), "sleep_bed");
            }
        } else {
            setState(STATE_FLOOR_SLEEP, true);
            this.setPose(Pose.SLEEPING);
        }
    }

    public void wakeUp() {
        if (this.isSleeping()) {
            this.stopSleeping();
        }
        if (getState(STATE_FLOOR_SLEEP)) {
            setState(STATE_FLOOR_SLEEP, false);
            this.setPose(Pose.STANDING);
        }
        sleepOrderedAt = -1;
    }

    @Override
    public void stopSleeping() {
        super.stopSleeping();
        setState(STATE_FLOOR_SLEEP, false);
        sleepOrderedAt = -1;
    }

    /** Marks the sleep she just started as an order: it lasts until she is rested, not until the next daylight check. */
    public void markSleepOrdered() {
        sleepOrderedAt = this.level().getGameTime();
    }

    public boolean isSleepOrdered() {
        return sleepOrderedAt >= 0;
    }

    public long sleepOrderedAt() {
        return sleepOrderedAt;
    }

    /** Bed within the radius that nobody sleeps in. */
    @Nullable
    public BlockPos findFreeBed(BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD
                    && !state.getValue(BedBlock.OCCUPIED)) {
                double d = pos.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ particles

    public void spawnParticles(ParticleOptions particle, int count, double spread) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(particle, this.getX(), this.getY() + this.getBbHeight() * 0.8, this.getZ(), count, spread, spread * 0.5, spread, 0.02);
        }
    }

    public void spawnItemParticles(ItemStack stack, int count) {
        if (this.level() instanceof ServerLevel level && !stack.isEmpty()) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getEyeY() - 0.1, this.getZ(), count, 0.15, 0.1, 0.15, 0.05);
        }
    }

    public void hearts(int count) {
        spawnParticles(ParticleTypes.HEART, count, 0.4);
    }

    // ------------------------------------------------------------------ status for menus

    public JsonObject statusJson(ServerPlayer viewer) {
        JsonObject json = new JsonObject();
        json.addProperty("name", getLauraName());
        LivingEntity owner = this.getOwner();
        json.addProperty("owner", owner != null ? owner.getName().getString() : "");
        json.addProperty("isOwner", this.isOwnedBy(viewer));
        json.addProperty("mode", getMode().name());
        json.addProperty("combat", getCombatMode().name());
        json.addProperty("pickup", isPickingUpItems());
        json.addProperty("affection", getAffection());
        json.addProperty("days", daysTogether());
        json.addProperty("health", this.getHealth());
        json.addProperty("maxHealth", this.getMaxHealth());
        json.addProperty("mood", getMood().name());
        json.addProperty("gagSeconds", gagTicks / 20);
        json.addProperty("skin", getSkinRaw());
        json.addProperty("skinLabel", getSkinLabel());
        json.addProperty("slim", isSlim());
        json.addProperty("model", getModelName());
        json.addProperty("annoyance", LauraConfig.annoyance.get().name());
        if (homePos != null) {
            json.addProperty("home", homePos.getX() + " " + homePos.getY() + " " + homePos.getZ());
            json.addProperty("homeDimension", homeDimension == null ? "" : homeDimension.location().toString());
        }
        Needs needs = brain.needs();
        JsonObject needsJson = new JsonObject();
        for (Needs.Need need : Needs.Need.values()) {
            needsJson.addProperty(need.key(), Math.round(needs.get(need)));
        }
        json.add("needs", needsJson);
        Desire desire = brain.desire();
        if (desire != null) {
            json.addProperty("desire", desire.syncString());
            json.addProperty("desireSeconds", Math.max(0, (desire.deadline() - this.level().getGameTime()) / 20));
            json.addProperty("desireText", com.vyrriox.lauramod.util.TextCodec.toJson(desire.describe(), this.level().registryAccess()));
        }
        StringBuilder jobs = new StringBuilder();
        for (LauraJob job : workplace.jobs().keySet()) {
            jobs.append(job.name()).append(',');
        }
        json.addProperty("jobs", jobs.toString());
        LauraTask current = workplace.current();
        if (current != null) {
            json.addProperty("taskCurrent", com.vyrriox.lauramod.util.TextCodec.toJson(current.describe(), this.level().registryAccess()));
        } else if (fetchGoal != null && fetchGoal.isActive()) {
            json.addProperty("taskCurrent", com.vyrriox.lauramod.util.TextCodec.toJson(Component.translatable("lauramod.task.fetch"), this.level().registryAccess()));
        }
        com.google.gson.JsonArray queue = new com.google.gson.JsonArray();
        for (LauraTask task : workplace.queued()) {
            queue.add(com.vyrriox.lauramod.util.TextCodec.toJson(task.describe(), this.level().registryAccess()));
        }
        json.add("taskQueue", queue);
        json.addProperty("chests", workplace.chests().size());
        return json;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LauraDataVersion", 2);
        tag.putString("Skin", getSkinRaw());
        tag.putBoolean("SkinSlim", isSlim());
        tag.putString("SkinLabel", skinLabel);
        tag.putString("Model", getModelName());
        tag.putByte("Mode", (byte) getMode().ordinal());
        tag.putByte("Combat", (byte) getCombatMode().ordinal());
        tag.putBoolean("Pickup", isPickingUpItems());
        tag.putInt("Affection", getAffection());
        // The real value: 0 means "no timer" (gag.durationSeconds = 0), she keeps the gag after a reload.
        tag.putInt("GagTicks", isGagged() ? gagTicks : 0);
        tag.putBoolean("Gagged", isGagged());
        tag.putLong("SummonTime", summonGameTime);
        ListTag items = inventory.createTag();
        for (ItemStack stack : overflow) {
            if (!stack.isEmpty()) {
                // Not dropped yet (saved before her first tick): kept, they overflow again at the next load.
                items.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("Inventory", items);
        if (!getBackItem().isEmpty()) {
            tag.put("BackItem", getBackItem().save(new CompoundTag()));
        }
        ListTag kept = new ListTag();
        for (ItemStack stack : keptOwnerItems) {
            if (!stack.isEmpty()) {
                kept.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("KeptOwnerItems", kept);
        // The fetch itself is not saved, only what she already carries for it: without this the items
        // vanish when she is saved and unloaded, dies, is sent away or changes dimension on the way.
        ListTag fetchBag = new ListTag();
        for (ItemStack stack : pendingFetchBag) {
            if (!stack.isEmpty()) {
                fetchBag.add(stack.save(new CompoundTag()));
            }
        }
        if (fetchGoal != null) {
            for (ItemStack stack : fetchGoal.carried()) {
                if (!stack.isEmpty()) {
                    fetchBag.add(stack.save(new CompoundTag()));
                }
            }
        }
        if (!fetchBag.isEmpty()) {
            tag.put("FetchBag", fetchBag);
        }
        if (homePos != null) {
            tag.putInt("HomeX", homePos.getX());
            tag.putInt("HomeY", homePos.getY());
            tag.putInt("HomeZ", homePos.getZ());
            tag.putString("HomeDim", homeDimension == null ? Level.OVERWORLD.location().toString() : homeDimension.location().toString());
        }
        if (wanderCenter != null) {
            tag.putLong("WanderCenter", wanderCenter.asLong());
        }
        // Not "Brain": vanilla uses that key for its own memories.
        tag.put("LauraBrain", brain.save());
        tag.put("Work", workplace.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        resizeInventory();
        if (tag.contains("Skin")) {
            this.entityData.set(DATA_SKIN, SkinRef.parse(tag.getString("Skin")).serialize());
            this.entityData.set(DATA_SLIM, !tag.contains("SkinSlim") || tag.getBoolean("SkinSlim"));
        } else if (tag.contains("SkinUrl") && !tag.getString("SkinUrl").isEmpty()) {
            // Version 1.x stored a plain URL.
            this.entityData.set(DATA_SKIN, new SkinRef(SkinRef.Type.URL, SkinRef.normalizeUrl(tag.getString("SkinUrl")), "").serialize());
            this.entityData.set(DATA_SLIM, false);
        }
        skinLabel = tag.getString("SkinLabel");
        this.entityData.set(DATA_MODEL, tag.getString("Model"));
        if (tag.contains("Mode")) {
            this.entityData.set(DATA_MODE, tag.getByte("Mode"));
        } else {
            this.entityData.set(DATA_MODE, (byte) (this.isOrderedToSit() ? LauraMode.STAY : LauraMode.FOLLOW).ordinal());
        }
        this.entityData.set(DATA_COMBAT, tag.contains("Combat") ? tag.getByte("Combat") : (byte) LauraConfig.defaultCombatMode.get().ordinal());
        setPickingUpItems(tag.getBoolean("Pickup"));
        this.entityData.set(DATA_AFFECTION, tag.contains("Affection") ? tag.getInt("Affection") : LauraConfig.startAffection.getInt());
        if (tag.getBoolean("Gagged")) {
            setGagged(true, tag.getInt("GagTicks"));
        }
        summonGameTime = tag.contains("SummonTime") ? tag.getLong("SummonTime") : this.level().getGameTime();
        overflow.clear();
        if (tag.contains("Inventory", Tag.TAG_LIST)) {
            // Like SimpleContainer.fromTag, but what no longer fits (general.inventoryRows was lowered)
            // is kept and dropped at her feet on her first tick instead of being deleted.
            ListTag items = tag.getList("Inventory", Tag.TAG_COMPOUND);
            inventory.clearContent();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = ItemStack.of(items.getCompound(i));
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack rest = inventory.addItem(stack);
                if (!rest.isEmpty()) {
                    overflow.add(rest);
                }
            }
        }
        keptOwnerItems.clear();
        setBackItem(tag.contains("BackItem", Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("BackItem")) : ItemStack.EMPTY);
        if (tag.contains("KeptOwnerItems", Tag.TAG_LIST)) {
            ListTag kept = tag.getList("KeptOwnerItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < kept.size(); i++) {
                ItemStack stack = ItemStack.of(kept.getCompound(i));
                if (!stack.isEmpty()) {
                    keptOwnerItems.add(stack);
                }
            }
        }
        pendingFetchBag.clear();
        if (tag.contains("FetchBag", Tag.TAG_LIST)) {
            ListTag fetchBag = tag.getList("FetchBag", Tag.TAG_COMPOUND);
            for (int i = 0; i < fetchBag.size(); i++) {
                ItemStack stack = ItemStack.of(fetchBag.getCompound(i));
                if (!stack.isEmpty()) {
                    pendingFetchBag.add(stack);
                }
            }
        }
        if (tag.contains("HomeX")) {
            homePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            ResourceLocation dim = ResourceLocation.tryParse(tag.getString("HomeDim"));
            homeDimension = ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.location() : dim);
        }
        if (tag.contains("WanderCenter")) {
            wanderCenter = BlockPos.of(tag.getLong("WanderCenter"));
        }
        if (tag.contains("LauraBrain", Tag.TAG_COMPOUND)) {
            brain.load(tag.getCompound("LauraBrain"));
        } else if (tag.getBoolean("IsSad")) {
            brain.startSulking(tag.getInt("SadnessTimer"));
        }
        if (tag.contains("Work", Tag.TAG_COMPOUND)) {
            workplace.load(tag.getCompound("Work"));
        }
        brain.syncToEntity();
    }
}
