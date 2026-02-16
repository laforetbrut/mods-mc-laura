package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.entity.ai.ChatterGoal;
import com.vyrriox.lauramod.entity.ai.ComplainGoal;
import com.vyrriox.lauramod.entity.ai.LauraSleepGoal;
import com.vyrriox.lauramod.entity.ai.ScareVillagersGoal;
import com.vyrriox.lauramod.init.ModSounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.vyrriox.lauramod.inventory.LauraInventoryMenu;

public class LauraEntity extends TamableAnimal implements MenuProvider {
    private static final EntityDataAccessor<String> SKIN_URL = SynchedEntityData.defineId(LauraEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_SAD = SynchedEntityData.defineId(LauraEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SADNESS_TIMER = SynchedEntityData.defineId(LauraEntity.class,
            EntityDataSerializers.INT);

    private final SimpleContainer inventory = new SimpleContainer(9);
    private int aggressionLevel = 0;
    private int lastHitTick = 0;
    private int loveCheckCooldown = 12000;
    private int fartCooldown = 24000;
    private boolean isWaitingForLoveResponse = false;
    private int loveResponseTimer = 0;

    public LauraEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_URL, "");
        builder.define(IS_SAD, false);
        builder.define(SADNESS_TIMER, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("SkinUrl", this.getSkinUrl());
        compound.put("Inventory", this.inventory.createTag(this.level().registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SkinUrl")) {
            this.setSkinUrl(compound.getString("SkinUrl"));
        }
        if (compound.contains("Inventory")) {
            this.inventory.fromTag(compound.getList("Inventory", 10), this.level().registryAccess());
        }
    }

    public String getSkinUrl() {
        return this.entityData.get(SKIN_URL);
    }

    public void setSkinUrl(String url) {
        this.entityData.set(SKIN_URL, url);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() || LauraEntity.this.isSad();
            }
        });
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return super.canUse() && !LauraEntity.this.isSad();
            }
        });
        this.goalSelector.addGoal(3, new ComplainGoal(this));
        this.goalSelector.addGoal(3, new ChatterGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !LauraEntity.this.isSad();
            }
        });
        this.goalSelector.addGoal(4, new LauraSleepGoal(this));
        this.goalSelector.addGoal(5, new ScareVillagersGoal(this));
        this.goalSelector.addGoal(6, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.6D, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return super.canUse() && !LauraEntity.this.isSad();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public boolean isSad() {
        return this.entityData.get(IS_SAD);
    }

    public void setSad(boolean sad) {
        this.entityData.set(IS_SAD, sad);
        if (sad) {
            this.setOrderedToSit(true);
            this.entityData.set(SADNESS_TIMER, 3600);
        } else {
            this.entityData.set(SADNESS_TIMER, 0);
            this.aggressionLevel = 0;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            // Rapid Regeneration: 1 HP per tick
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(1.0F);
            }

            int timer = this.entityData.get(SADNESS_TIMER);
            if (timer > 0) {
                this.entityData.set(SADNESS_TIMER, timer - 1);
                if (timer - 1 <= 0) {
                    this.setSad(false);
                }
            }

            if (this.tickCount - lastHitTick > 600) {
                this.aggressionLevel = 0;
            }
            // Love Check
            if (this.isTame() && !this.isSad() && this.random.nextInt(loveCheckCooldown) == 0) {
                Player owner = (Player) this.getOwner();
                if (owner instanceof ServerPlayer serverPlayer && this.distanceTo(owner) < 10) {
                    owner.sendSystemMessage(Component.literal("<Laura> "
                            + InteractionDatabase.getStaticString(serverPlayer.getLanguage(), "love_check")));
                    this.isWaitingForLoveResponse = true;
                    this.loveResponseTimer = 600; // 30 seconds to answer
                }
            }

            if (this.isWaitingForLoveResponse) {
                this.loveResponseTimer--;
                if (this.loveResponseTimer <= 0) {
                    this.isWaitingForLoveResponse = false;
                }
            }

            // Fart (extremely rare)
            if (this.random.nextInt(fartCooldown) == 0) {
                this.playSound(ModSounds.LAURA_FART.get(), 1.0F, 1.0F);
                Player owner = (Player) this.getOwner();
                if (owner instanceof ServerPlayer serverPlayer && this.distanceTo(owner) < 5) {
                    owner.sendSystemMessage(Component.literal(
                            "<Laura> " + InteractionDatabase.getStaticString(serverPlayer.getLanguage(), "fart")));
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player && this.isOwnedBy(player)) {
            this.aggressionLevel++;
            this.lastHitTick = this.tickCount;
            if (this.aggressionLevel >= 3) {
                this.setSad(true);
                this.playSound(ModSounds.LAURA_SAD.get(), 1.0F, 1.0F);
                player.sendSystemMessage(Component
                        .literal("<Laura> " + InteractionDatabase.getStaticString(player.getLanguage(), "sad")));
            } else {
                this.playSound(ModSounds.LAURA_ANGRY.get(), 1.0F, 1.0F);
                player.sendSystemMessage(Component
                        .literal("<Laura> " + InteractionDatabase.getStaticString(player.getLanguage(), "angry")));
            }
        }
        return super.hurt(source, amount);
    }

    public void handleLoveResponse(boolean loved) {
        this.isWaitingForLoveResponse = false;
        Player owner = (Player) this.getOwner();
        if (owner instanceof ServerPlayer serverPlayer) {
            String locale = serverPlayer.getLanguage();
            if (loved) {
                this.playSound(ModSounds.LAURA_HAPPY.get(), 1.0F, 1.0F);
                owner.sendSystemMessage(
                        Component.literal("<Laura> " + InteractionDatabase.getStaticString(locale, "love_yes")));
                for (int i = 0; i < 7; ++i) {
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    double d2 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART, this.getRandomX(1.0D),
                            this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
                }
            } else {
                this.setSad(true);
                this.playSound(ModSounds.LAURA_SAD.get(), 1.0F, 1.0F);
                owner.sendSystemMessage(
                        Component.literal("<Laura> " + InteractionDatabase.getStaticString(locale, "love_no")));
            }
        }
    }

    public boolean isWaitingForLoveResponse() {
        return isWaitingForLoveResponse;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (itemstack.is(Items.POPPY) && !this.isBaby()) {
                    if (!this.level().isClientSide) {
                        itemstack.shrink(1);
                        this.spawnAtLocation(new ItemStack(Items.DIAMOND), 1);
                        player.sendSystemMessage(Component.translatable("chat.lauramod.thanks"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (player.isSecondaryUseActive()) {
                    if (!this.level().isClientSide) {
                        player.openMenu(this);
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                } else {
                    if (!this.level().isClientSide) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }
        } else {
            if (!this.level().isClientSide) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Laura");
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LAURA_AMBIENT.get();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new LauraInventoryMenu(id, playerInventory, this.inventory);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }
}
