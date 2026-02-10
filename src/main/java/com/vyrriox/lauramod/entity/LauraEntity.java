package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.entity.ai.FollowPlayerGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LauraEntity extends PathfinderMob {
    private int chatTimer = 0;
    private static final int CHAT_INTERVAL = 6000; // Average every 5 minutes

    public LauraEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.2D, 5.0F, 2.0F));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            handleChatMessages();
            handleSleeping();
        }
    }

    private void handleChatMessages() {
        if (this.random.nextInt(CHAT_INTERVAL) == 0) {
            Player player = this.level().getNearestPlayer(this, 10);
            if (player != null) {
                String messageKey = "chat.lauramod.random." + this.random.nextInt(3);
                player.sendSystemMessage(Component.translatable(messageKey));
                // Play ambient sound here later
            }
        }
    }

    private void handleSleeping() {
        Player player = this.level().getNearestPlayer(this, 10);
        if (player != null && player.isSleeping()) {
            if (!this.isSleeping()) {
                BlockPos playerBedPos = player.getSleepingPos().orElse(null);
                if (playerBedPos != null) {
                    BlockPos nearbyPos = findNearbyBed(playerBedPos);
                    if (nearbyPos != null) {
                        this.startSleeping(nearbyPos);
                    } else {
                        // Sleep on the ground near the bed
                        this.startSleeping(playerBedPos.offset(1, 0, 1));
                    }
                }
            }
        } else if (this.isSleeping()) {
            this.stopSleeping();
        }
    }

    private BlockPos findNearbyBed(BlockPos pos) {
        for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            BlockState state = this.level().getBlockState(blockpos);
            if (state.getBlock() instanceof BedBlock) {
                return blockpos;
            }
        }
        return null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }
}
