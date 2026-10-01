package com.vyrriox.lauramod.block;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.util.ItemSpec;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * Laura's Gravestone. When she dies, lay a flower on it (right click) to bring her back.
 *
 * @author vyrriox
 */
public class LauraGraveBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NORTH_SOUTH = Shapes.or(
            Block.box(1, 0, 4, 15, 2, 12),
            Block.box(3, 2, 6, 13, 13, 10),
            Block.box(4, 13, 6, 12, 15, 10),
            Block.box(6, 15, 6, 10, 16, 10));
    private static final VoxelShape EAST_WEST = Shapes.or(
            Block.box(4, 0, 1, 12, 2, 15),
            Block.box(6, 2, 3, 10, 13, 13),
            Block.box(6, 13, 4, 10, 15, 12),
            Block.box(6, 15, 6, 10, 16, 10));

    public LauraGraveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    public static boolean isReviveItem(ItemStack stack) {
        for (String spec : LauraConfig.reviveItems.get()) {
            Optional<ItemSpec> parsed = ItemSpec.parse(spec);
            if (parsed.isPresent() && parsed.get().test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** Minecraft 1.20.1 has a single block interaction: a revive item first, then the bare hand hint. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (isReviveItem(stack)) {
            return useItemOn(stack, state, level, pos, player, hand, hit);
        }
        return hand == InteractionHand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : InteractionResult.PASS;
    }

    private InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (LauraConfig.reviveMode.get() != LauraConfig.ReviveMode.GRAVE) {
            player.sendSystemMessage(Component.translatable("lauramod.grave.disabled").withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }
        if (LauraManager.reviveAtGrave(serverPlayer, pos)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        ((ServerLevel) level).sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
        player.sendSystemMessage(Component.translatable("lauramod.grave.empty").withStyle(ChatFormatting.GRAY));
        return InteractionResult.CONSUME;
    }

    private InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean waiting = player instanceof ServerPlayer sp && LauraManager.hasDeadCompanion(sp);
            player.sendSystemMessage(Component.translatable(waiting ? "lauramod.grave.hint_flower" : "lauramod.grave.empty").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
