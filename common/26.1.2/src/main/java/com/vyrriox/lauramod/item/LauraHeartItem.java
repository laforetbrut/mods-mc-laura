package com.vyrriox.lauramod.item;

import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.world.LauraManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Laura's Heart: cook a pink tulip in a furnace, then use it to summon Laura. If she is already
 * with you, the heart calls her back and makes her very, very happy.
 *
 * @author vyrriox
 */
public class LauraHeartItem extends Item {
    public LauraHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        LauraEntity existing = LauraManager.find(serverPlayer);
        if (existing == null && LauraManager.hasDeadCompanion(serverPlayer)) {
            player.sendSystemMessage(Component.translatable("lauramod.grave.heart_hint").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.FAIL;
        }
        if (existing != null) {
            if (existing.level() != level || existing.distanceToSqr(player) > 12 * 12) {
                LauraManager.teleport(existing, serverPlayer.level(), player.blockPosition());
                existing = LauraManager.find(serverPlayer);
            }
            if (existing != null) {
                existing.brain().changeAffection(50);
                existing.brain().needs().add(Needs.Need.ATTENTION, 40);
                existing.brain().makeHappy(600);
                existing.hearts(12);
                existing.playEmote(Emote.CELEBRATE);
                LauraSpeech.say(existing, serverPlayer, "heart.gift", LineFormatter.values());
            }
        } else {
            LauraManager.call(serverPlayer);
            if (LauraManager.find(serverPlayer) == null) {
                return InteractionResult.FAIL;
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.lauramod.laura_heart.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("item.lauramod.laura_heart.recipe").withStyle(ChatFormatting.GRAY));
    }
}
