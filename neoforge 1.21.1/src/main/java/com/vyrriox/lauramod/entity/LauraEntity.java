package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.entity.ai.ChatterGoal;
import com.vyrriox.lauramod.entity.ai.ComplainGoal;
import com.vyrriox.lauramod.entity.ai.LauraSleepGoal;
import com.vyrriox.lauramod.entity.ai.ScareVillagersGoal;
import com.vyrriox.lauramod.init.ModSounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
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
    private final SimpleContainer inventory = new SimpleContainer(9);

    public LauraEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_URL, "");
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
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new ComplainGoal(this));
        this.goalSelector.addGoal(3, new ChatterGoal(this));
        this.goalSelector.addGoal(4, new LauraSleepGoal(this));
        this.goalSelector.addGoal(5, new ScareVillagersGoal(this));
        this.goalSelector.addGoal(6, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.6D, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (itemstack.is(Items.POPPY) && !this.isBaby()) {
                    // Gift logic
                    if (!this.level().isClientSide) {
                        itemstack.shrink(1);
                        this.spawnAtLocation(new ItemStack(Items.DIAMOND), 1); // Mock 100% for now
                        player.sendSystemMessage(Component.translatable("chat.lauramod.thanks"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (player.isSecondaryUseActive()) {
                    if (!this.level().isClientSide) {
                        player.openMenu(this);
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                } else {
                    // Sit
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
