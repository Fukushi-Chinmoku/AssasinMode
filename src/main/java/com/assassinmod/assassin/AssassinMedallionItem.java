package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AssassinMedallionItem extends Item {
    public AssassinMedallionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (AssassinAbilityManager.hasClass(player)) {
            player.sendSystemMessage(Component.literal("You are already an Assassin."));
            return InteractionResultHolder.fail(stack);
        }

        AssassinAbilityManager.giveClass(player);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.8F, 1.3F);
        player.sendSystemMessage(Component.literal("The Assassin Medallion awakens your class. Press V to open the skill tree."));
        return InteractionResultHolder.success(stack);
    }
}
